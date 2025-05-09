package com.example.ordermicroservice.outbox

import com.avro.payment.PaymentOutboxMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.PaymentOutbox
import com.example.ordermicroservice.document.ProcessStage
import com.example.ordermicroservice.repository.mongo.PaymentOutboxRepository
import com.example.ordermicroservice.service.RedisService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class PaymentOutboxService(
    private val paymentOutboxRepository: PaymentOutboxRepository,
    private val redisService: RedisService,
    private val paymentOutboxTemplate: KafkaTemplate<String, PaymentOutboxMessage>,
) {
    companion object {
        val log = KotlinLogging.logger {  }
    }
    fun pollingOutbox(): List<PaymentOutbox> {
        return paymentOutboxRepository.findByProcessStage(ProcessStage.BEFORE_PROCESS)
    }

    fun publishingOutbox(paymentOutbox: PaymentOutbox) {
        val outboxMessage = PaymentOutboxMessage.newBuilder()
            .setAggId(paymentOutbox.aggId)
            .setProcessStage(com.avro.support.ProcessStage.PENDING)
            .setPaymentId(paymentOutbox.paymentId)
            .build()

        paymentOutboxTemplate.executeInTransaction {
            it.send(KafkaTopicNames.PAYMENT_OUTBOX, outboxMessage.aggId, outboxMessage)
        }
    }

    @Scheduled(fixedRate = 3000L)
    fun processPayment() {
        val outboxes = pollingOutbox()

        for(outbox in outboxes) {
            outbox.processStage = ProcessStage.PENDING

            publishingOutbox(paymentOutbox = outbox)

            paymentOutboxRepository.save(outbox)
        }
    }

    fun pollingRefundOutbox(): List<PaymentOutbox> {
        return paymentOutboxRepository.findByProcessStage(ProcessStage.BEFORE_CANCEL)
    }

    fun publishRefundOutbox(outbox: PaymentOutbox) {
        val outboxMessage = PaymentOutboxMessage.newBuilder()
            .setPaymentId(outbox.paymentId)
            .setAggId(outbox.aggId)
            .setProcessStage(com.avro.support.ProcessStage.BEFORE_CANCEL)
            .build()

        paymentOutboxTemplate.executeInTransaction {
            it.send(KafkaTopicNames.PAYMENT_OUTBOX, outboxMessage.aggId, outboxMessage)
        }
    }

    @Scheduled(fixedRate = 5000)
    fun processRefundPayment() {
        val outboxes = pollingRefundOutbox()

        for(outbox in outboxes) {
            publishRefundOutbox(outbox)
        }
    }
}