package com.example.ordermicroservice.service

import com.avro.order.OrderOutboxMessage
import com.avro.shipping.ShippingMessage
import com.avro.support.geojson.Location
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.OrderOutbox
import com.example.ordermicroservice.document.Orders
import com.example.ordermicroservice.document.ProcessStage
import com.example.ordermicroservice.document.Products
import com.example.ordermicroservice.dto.CreateOrderRequest
import com.example.ordermicroservice.dto.CreateOrderResponse
import com.example.ordermicroservice.dto.GetSellerResponse
import com.example.ordermicroservice.dto.GetUserResponse
import com.example.ordermicroservice.repository.mongo.OrderOutboxRepository
import com.example.ordermicroservice.repository.mongo.OrderRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.http.MediaType
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy
import org.springframework.kafka.support.Acknowledgment
import org.springframework.retry.annotation.Backoff
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderOutboxRepository: OrderOutboxRepository,
    private val redisService: RedisService,
    private val shippingTemplate: KafkaTemplate<String, ShippingMessage>,
) {
    companion object {
        val log = KotlinLogging.logger {  }
    }
    fun createOrder(order: CreateOrderRequest): CreateOrderResponse {
        val savedOrder = CompletableFuture.supplyAsync {
            val orderEntity = Orders.of(
                id = null,
                userId = order.userId,
                orderNumber = generateOrderNumber(order.userId),
                orderedTime = getNowTime(),
                products = order.products,
                sellerId = order.sellerId
            )
            val savedOrder = orderRepository.save(orderEntity)
            savedOrder
        }.thenApply {
            val aggId = redisService.getAggregatorId()
            val processStage = ProcessStage.BEFORE_PROCESS
            val outbox = OrderOutbox.of(id = null, aggId = aggId.toString(),
                processStage =  processStage, orderId = it.orderNumber)

            orderOutboxRepository.save(outbox)

            it
        }.join()

        return CreateOrderResponse.of(
            orderNumber = savedOrder.orderNumber,
            products = savedOrder.products,
            amount = calculateAmount(products = savedOrder.products),
            orderedTime = savedOrder.orderedTime,
            address = "",
            sellerName = "",
            username = ""
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun generateOrderNumber(userId: String): String {
        val orderedTime = Instant.now().toEpochMilli().toHexString()
        val snowflake = userId.take(8)
        return orderedTime.plus(snowflake)
    }

    private fun getNowTime(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return Instant.now().atZone(ZoneId.of("Asia/Seoul")).format(formatter)
    }

    private fun calculateAmount(products: List<Products>): Long =
        products.sumOf { it.quantity * it.price }

    @RetryableTopic(
        retryTopicSuffix = "-retry",
        dltTopicSuffix = "-dlt",
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        concurrency = "3",
        numPartitions = "10",
        replicationFactor = "3",
        dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
        backoff = Backoff(value = 1000L, multiplier = 2.0, maxDelay = 20000L, random = true),
        kafkaTemplate = "orderOutboxTemplate"
    )
    @KafkaListener(topics = ["order-outbox.topic"],
        groupId = "ORDER_OUTBOX",
        containerFactory = "orderOutboxListenerContainer",
        concurrency = "3",
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
                OrderOutbox(id = null, aggId = "noop", processStage = ProcessStage.EXCEPTION, orderId = "noop")
            }

        if(foundOutbox.processStage == ProcessStage.EXCEPTION) {
            log.info { "$outbox 의 Process Stage가 올바르지 않습니다." }
            return
        }

        val shippingMessage = generateShippingMessage(orderId = outbox.orderId)
        shippingTemplate.executeInTransaction {
            it.send(KafkaTopicNames.SHIPPING, outbox.orderId, shippingMessage)
        }

        log.info { "${shippingMessage.orderId}가 배달 서비스로 퍼블리싱되었습니다." }

        foundOutbox.processStage = ProcessStage.PROCESSED
        orderOutboxRepository.save(foundOutbox)

        ack.acknowledge()
    }

    private fun generateShippingMessage(orderId: String): ShippingMessage {
        val order = orderRepository.findByOrderNumber(orderId)
            ?: throw RuntimeException("${orderId}에 해당하는 주문이 없습니다!")

        val sellerRestClient = RestClient.create("http://localhost:8080")
        val seller = sellerRestClient.get().uri("/service/seller?sellerId={sellerId}", order.sellerId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body<GetSellerResponse>()
            ?: throw RuntimeException("판매자 ${order.sellerId}에 해당하는 주소가 없습니다.")

        val userRestClient = RestClient.create("http://localhost:8080")
        val user = userRestClient.get().uri("/service/user?userId={userId}", order.userId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body<GetUserResponse>()
            ?: throw RuntimeException("유저 ${order.userId}에 해당하는 주소가 없습니다.")

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
            .build()
    }
}