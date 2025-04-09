package com.example.ordermicroservice.outbox

import com.avro.order.OrderOutboxMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.OrderOutbox
import com.example.ordermicroservice.document.ProcessStage
import com.example.ordermicroservice.repository.mongo.OrderOutboxRepository
import com.example.ordermicroservice.service.RedisService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId

@Service
class OrderOutboxService(
    private val orderOutboxRepository: OrderOutboxRepository,
    private val redisService: RedisService,
    private val orderOutboxTemplate: KafkaTemplate<String, OrderOutboxMessage>
) {
    companion object {
        val log = KotlinLogging.logger {  }
    }
    fun outboxPolling(): List<OrderOutbox> =
        orderOutboxRepository.findByProcessStage(ProcessStage.BEFORE_PROCESS)

    fun outboxPublishing(orderOutbox: OrderOutbox) {
        val orderOutboxMessage = outboxToAvro(orderOutbox)
        val sendResult = orderOutboxTemplate.executeInTransaction {
            it.send(KafkaTopicNames.ORDER_OUTBOX, orderOutbox.orderId, orderOutboxMessage)
        }.whenComplete { result, exception ->
            if(exception != null) {
                log.info { "$exception 발생" }
            }
            log.info { "$orderOutboxMessage 가 카프카 프로듀서에 의해 퍼블리싱 됩니다. => processStage = ${orderOutboxMessage.processStage}" }
            log.info { "Order Outbox Producer Metadata: ${Instant.ofEpochMilli(result.recordMetadata.timestamp()).atZone(
                ZoneId.of("Asia/Seoul"))}" }
        }

        sendResult.join()
    }

    private fun outboxToAvro(orderOutbox: OrderOutbox): OrderOutboxMessage {
        return OrderOutboxMessage.newBuilder()
            .setAggId(orderOutbox.aggId)
            .setProcessStage(com.avro.support.ProcessStage.PENDING)
            .setOrderId(orderOutbox.orderId)
            .setPaymentIntentToken(orderOutbox.paymentIntentToken)
            .build()
    }

    @Scheduled(fixedRate = 3000L)
    fun processOrderOutbox() {
        val orderOutboxes = outboxPolling()

        for(orderOutbox in orderOutboxes) {
            orderOutbox.processStage = ProcessStage.PENDING

            log.info { "${orderOutbox.orderId} 주문이 폴링되었습니다." }

            outboxPublishing(orderOutbox)

            orderOutboxRepository.save(orderOutbox)
        }
    }
}