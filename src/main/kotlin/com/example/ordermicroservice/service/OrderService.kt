package com.example.ordermicroservice.service

import com.avro.order.OrderOutboxMessage
import com.avro.shipping.ShippingMessage
import com.avro.support.geojson.Location
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.*
import com.example.ordermicroservice.dto.CreateOrderRequest
import com.example.ordermicroservice.dto.CreateOrderResponse
import com.example.ordermicroservice.dto.GetSellerResponse
import com.example.ordermicroservice.dto.GetUserResponse
import com.example.ordermicroservice.repository.mongo.OrderOutboxRepository
import com.example.ordermicroservice.repository.mongo.OrderRepository
import com.example.ordermicroservice.support.MachineIdGenerator
import com.example.ordermicroservice.support.RetryableTopicForOrderTopic
import com.example.ordermicroservice.support.SnowflakeIdGenerator
import com.example.ordermicroservice.vo.CreateOrderVo
import com.example.ordermicroservice.vo.OrderCompensation
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderOutboxRepository: OrderOutboxRepository,
    private val redisService: RedisService,
    private val shippingTemplate: KafkaTemplate<String, ShippingMessage>,
    @Qualifier("readTimeoutExceptionRetryTemplate") private val readTimeoutExceptionRetryTemplate: RetryTemplate
) {
    companion object {
        val log = KotlinLogging.logger {  }
    }

    @KafkaListener(topics = [KafkaTopicNames.ORDER_REQUEST],
        containerFactory = "createOrderListenerContainerFactory",
        groupId = "CREATE_ORDER",
        concurrency = "3"
    )
    fun createOrder(record: ConsumerRecord<String, CreateOrderVo>, ack: Acknowledgment) {
        val order = record.value()

        log.info { "process order = $order" }

        val orderEntity = Orders.of(
            id = null,
            userId = order.userId,
            orderNumber = SnowflakeIdGenerator(MachineIdGenerator.machineId()).nextId().toString(),
            orderedTime = getNowTime(),
            products = order.products,
            serviceProcessStage = ServiceProcessStage.NOT_PROCESS,
            sellerId = order.sellerId,
            paymentIntentToken = order.paymentIntentToken
        )

        val savedOrder = orderRepository.save(orderEntity)

        val aggId = redisService.getAggregatorId()

        val processStage = ProcessStage.BEFORE_PROCESS

        val outbox = OrderOutbox.of(
            id = null,
            aggId = aggId.toString(),
            processStage = processStage,
            orderId = savedOrder.orderNumber,
            paymentIntentToken = savedOrder.paymentIntentToken
        )

        orderOutboxRepository.save(outbox)

        ack.acknowledge()
    }

    private fun getNowTime(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return Instant.now().atZone(ZoneId.of("Asia/Seoul")).format(formatter)
    }

    @RetryableTopicForOrderTopic
    @KafkaListener(topics = ["order-outbox.topic"],
        groupId = "ORDER_OUTBOX",
        containerFactory = "orderOutboxListenerContainer",
        concurrency = "3"
    )
    fun processOrder(record: ConsumerRecord<String, OrderOutboxMessage>, ack: Acknowledgment) {
        val outbox = record.value()

        log.info { "$outbox order-outbox.topic이 들어왔습니다." }

        if(ProcessStage.PENDING != ProcessStage.of(outbox.processStage.name)) {
            log.info { "${outbox.processStage.name}이 올바르지 않습니다." }
            return
        }

        val foundOutbox = orderOutboxRepository.findByOrderId(outbox.orderId)
            ?: run {
                log.info { "No OrderNumber = ${outbox.orderId} found" }
                OrderOutbox(id = null, aggId = "noop", processStage = ProcessStage.EXCEPTION, orderId = "noop", paymentIntentToken = "")
            }

        if(foundOutbox.processStage == ProcessStage.EXCEPTION) {
            log.info { "$outbox 의 Process Stage가 올바르지 않습니다." }
            return
        }

        val shippingMessage = generateShippingMessage(orderId = outbox.orderId, paymentIntentToken = outbox.paymentIntentToken)
        shippingTemplate.executeInTransaction {
            it.send(KafkaTopicNames.SHIPPING, outbox.orderId, shippingMessage)
        }

        log.info { "${shippingMessage.orderId}가 배달 서비스로 퍼블리싱되었습니다." }

        foundOutbox.processStage = ProcessStage.PROCESSED
        orderOutboxRepository.save(foundOutbox)

        ack.acknowledge()
    }

    private fun generateShippingMessage(orderId: String, paymentIntentToken: String): ShippingMessage {
        val order = orderRepository.findByOrderNumber(orderId)
            ?: throw RuntimeException("${orderId}에 해당하는 주문이 없습니다!")

        val sellerRestClient = RestClient.create("http://localhost:8080")
        val seller = readTimeoutExceptionRetryTemplate.execute<GetSellerResponse, Throwable> {
            sellerRestClient.get().uri("/service/seller?sellerId={sellerId}", order.sellerId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body<GetSellerResponse>()
                ?: throw RuntimeException("판매자 ${order.sellerId}에 해당하는 주소가 없습니다.")
        }

        val userRestClient = RestClient.create("http://localhost:8080")
        val user = readTimeoutExceptionRetryTemplate.execute<GetUserResponse, Throwable> {
            userRestClient.get().uri("/service/user?userId={userId}", order.userId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body<GetUserResponse>()
                ?: throw RuntimeException("유저 ${order.userId}에 해당하는 주소가 없습니다.")
        }

        return ShippingMessage.newBuilder()
            .setOrderId(order.orderNumber)
            .setShippingLocation(Location.newBuilder()
                .setLatitude(user.location[0])
                .setLongitude(user.location[1])
                .build())
            .setShippingLocationString(user.address)
            .setUsername(user.username)
            .setSellerName(seller.sellerName)
            .setSellerLocationString(seller.address)
            .setProcessStage(com.avro.support.ProcessStage.PENDING)
            .setPaymentIntentToken(paymentIntentToken)
            .build()
    }

    @KafkaListener(
        topics = [KafkaTopicNames.COMPENSATION_REQUEST],
        concurrency = "3",
        containerFactory = "orderCompensationListenerContainerFactory",
        groupId = "ORDER_COMPENSATION"
    )
    fun orderCompensation(record: ConsumerRecord<String, OrderCompensation>, ack: Acknowledgment) {
        val order = record.value()

        log.info { "${order.exceptionStep}에서 오류가 발생했습니다." }

        val txOrder = orderRepository.findByOrderNumber(order.orderNumber)
            ?: throw RuntimeException("${order.orderNumber} 에 해당하는 주문이 없습니다.")
        txOrder.processed = ServiceProcessStage.EXCEPTION
        orderRepository.save(txOrder)

        val outbox = orderOutboxRepository.findByOrderId(order.orderNumber)
            ?: throw RuntimeException("Order Outbox 에 해당 $order.orderNumber 주문이 없습니다.")

        outbox.processStage = ProcessStage.EXCEPTION

        orderOutboxRepository.save(outbox)

        ack.acknowledge()
    }
}