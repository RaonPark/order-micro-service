package com.example.ordermicroservice.service

import com.avro.order.OrderOutboxMessage
import com.avro.order.OrderRefundMessage
import com.avro.shipping.ShippingMessage
import com.avro.support.geojson.Location
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.*
import com.example.ordermicroservice.dto.*
import com.example.ordermicroservice.es.repo.OrdersForSeller
import com.example.ordermicroservice.es.repo.OrdersForUser
import com.example.ordermicroservice.repository.mongo.OrderOutboxRepository
import com.example.ordermicroservice.repository.mongo.OrderRepository
import com.example.ordermicroservice.support.MachineIdGenerator
import com.example.ordermicroservice.support.RetryableTopicForOrderTopic
import com.example.ordermicroservice.support.SnowflakeIdGenerator
import com.example.ordermicroservice.vo.CreateOrderVo
import com.example.ordermicroservice.vo.OrderCompensation
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.SendResult
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderOutboxRepository: OrderOutboxRepository,
    private val redisService: RedisService,
    private val shippingTemplate: KafkaTemplate<String, ShippingMessage>,
    private val orderRefundKafkaTemplate: KafkaTemplate<String, OrderRefundMessage>,
    @Qualifier("readTimeoutExceptionRetryTemplate") private val readTimeoutExceptionRetryTemplate: RetryTemplate,
    @Qualifier("noProducerAvailableExceptionRetryTemplate") private val noProducerAvailableExceptionRetryTemplate: RetryTemplate,
    private val elasticsearchOperations: ElasticsearchOperations
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

        // TODO("비동기로 처리하기: Kafka로 처리할 지 아니면 코루틴을 사용할지...")
        // 우선은 코루틴을 사용해보자.. IO Dispatcher 내부 함수가 restClient 를 사용하므로 Dispatcher.IO를 쓰는 것이 더 옳아보인다..
        runBlocking {
            withContext(Dispatchers.IO) {
                saveOrdersForSellerInElasticsearch(orderEntity)
                saveOrdersForUserInElasticsearch(orderEntity)
            }
        }


        ack.acknowledge()
    }

    private fun getNowTime(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return Instant.now().atZone(ZoneId.of("Asia/Seoul")).format(formatter)
    }

    private fun saveOrdersForSellerInElasticsearch(order: Orders) {
        val ordersForSellerMap = hashMapOf<String, MutableList<Products>>()

        val orderedUser = RestClient.create("http://localhost:8080")
            .get()
            .uri("/service/user?userId=${order.userId}")
            .retrieve()
            .body(GetUserResponse::class.java)
            ?: throw RuntimeException("${order.userId}에 해당하는 유저가 없습니다.")

        order.products.forEach { product ->
            ordersForSellerMap.getOrPut(product.sellerId) { mutableListOf() }
                .add(product)
        }

        ordersForSellerMap.forEach {
            val sellerId = it.key
            val products = it.value

            val orderForSeller = OrdersForSeller(
                orderNumber = order.orderNumber,
                orderedTime = order.orderedTime,
                products = products,
                shippingLocation = orderedUser.address,
                userId = order.userId,
                sellerId = sellerId
            )

            elasticsearchOperations.save(orderForSeller)
        }
    }

    private fun saveOrdersForUserInElasticsearch(orderEntity: Orders) {
        val orderForUser = OrdersForUser(
            userId = orderEntity.userId,
            orderNumber = orderEntity.orderNumber,
            orderedTime = orderEntity.orderedTime,
            products = orderEntity.products
        )
        elasticsearchOperations.save(orderForUser)
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

        when(ProcessStage.of(outbox.processStage.name)) {
            ProcessStage.PENDING -> {
                processOrderUsingOutbox(outbox)
            }
            ProcessStage.CANCELED -> {
                refundOrderUsingOutbox(outbox)
            }
            else -> {
                log.info { "outbox 상태가 올바르지 않습니다." }
                ack.acknowledge()
                return
            }
        }

        ack.acknowledge()
    }

    private fun processOrderUsingOutbox(outbox: OrderOutboxMessage) {
        val foundOutbox = orderOutboxRepository.findByOrderIdAndProcessStage(outbox.orderId, ProcessStage.PENDING)
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
    }

    private fun refundOrderUsingOutbox(outbox: OrderOutboxMessage) {
        log.info { "환불 처리를 진행합니다 : [주문] 환불 완료" }

        val dbOutbox = orderOutboxRepository.findByOrderIdAndProcessStage(outbox.orderId, ProcessStage.BEFORE_CANCEL)
            ?: throw RuntimeException("주문을 찾을 수 없습니다!")

        dbOutbox.processStage = ProcessStage.CANCELED

        log.info { dbOutbox }

        orderOutboxRepository.save(dbOutbox)

        noProducerAvailableExceptionRetryTemplate.execute<CompletableFuture<SendResult<String, OrderRefundMessage>>, Throwable> {
            val orderRefund = OrderRefundMessage.newBuilder()
                .setOrderNumber(outbox.orderId)
                .setPaymentNumber(outbox.paymentIntentToken)
                .build()
            orderRefundKafkaTemplate.executeInTransaction {
                it.send(KafkaTopicNames.SHIPPING_CANCEL, outbox.paymentIntentToken, orderRefund)
            }
        }
    }

    private fun generateShippingMessage(orderId: String, paymentIntentToken: String): ShippingMessage {
        val order = orderRepository.findByOrderNumber(orderId)
            ?: throw RuntimeException("${orderId}에 해당하는 주문이 없습니다!")

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

        val outbox = orderOutboxRepository.findByOrderIdAndProcessStage(order.orderNumber, ProcessStage.PROCESSED)
            ?: throw RuntimeException("Order Outbox 에 해당 $order.orderNumber 주문이 없습니다.")

        outbox.processStage = ProcessStage.EXCEPTION

        orderOutboxRepository.save(outbox)

        ack.acknowledge()
    }


    @KafkaListener(
        topics = [KafkaTopicNames.ORDER_REFUND],
        concurrency = "3",
        groupId = "REFUND_ORDER",
        containerFactory = "refundOrderListenerContainerFactory"
    )
    fun processRefund(record: ConsumerRecord<String, OrderRefundMessage>) {
        val refund = record.value()

        val orderOutbox = OrderOutbox.of(
            id = null,
            aggId = refund.orderNumber,
            orderId = refund.orderNumber,
            paymentIntentToken = refund.paymentNumber,
            processStage = ProcessStage.BEFORE_CANCEL
        )

        val savedOrder = orderRepository.findByOrderNumber(orderNumber = refund.orderNumber)
            ?: throw RuntimeException("${refund.orderNumber}에 해당하는 주문이 없습니다.")
        savedOrder.processed = ServiceProcessStage.CANCELED
        orderRepository.save(savedOrder)

        orderOutboxRepository.save(orderOutbox)
    }

    fun retrieveOrdersForSeller(sellerId: String): RetrieveOrdersForSellerListResponse {
        val searchOrdersForSellerQuery = CriteriaQuery(Criteria.where("sellerId").`is`(sellerId))
        val ordersForSellerList = mutableListOf<RetrieveOrdersForSellerDTO>()
        elasticsearchOperations.search(searchOrdersForSellerQuery, OrdersForSeller::class.java)
            .forEach {
                val ordersForSeller = it.content

                ordersForSellerList.add(
                    RetrieveOrdersForSellerDTO(
                        orderNumber = ordersForSeller.orderNumber,
                        products = ordersForSeller.products,
                        userId = ordersForSeller.userId,
                        shippingLocation = ordersForSeller.shippingLocation,
                        orderedTime = ordersForSeller.orderedTime,
                        sellerId = ordersForSeller.sellerId
                    )
                )
            }

        return RetrieveOrdersForSellerListResponse(ordersForSellerList)
    }

    fun retrieveOrdersForUser(userId: String): RetrieveOrdersForUserListResponse {
        val searchOrderForUserQuery = CriteriaQuery(Criteria.where("userId").`is`(userId))
        val ordersForUserList = mutableListOf<RetrieveOrdersForUserDTO>()

        elasticsearchOperations.search(searchOrderForUserQuery, OrdersForUser::class.java)
            .forEach { indexes ->
                val order = indexes.content

                ordersForUserList.add(
                    RetrieveOrdersForUserDTO(
                        orderNumber = order.orderNumber,
                        orderedTime = order.orderedTime,
                        products = order.products,
                        shippingStatus = order.shippingStatus
                    )
                )
            }

        return RetrieveOrdersForUserListResponse(ordersForUserList)
    }

}