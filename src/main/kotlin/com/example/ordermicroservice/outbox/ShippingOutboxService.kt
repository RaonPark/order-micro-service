package com.example.ordermicroservice.outbox

import com.avro.payment.PAYMENT_TYPE
import com.avro.payment.PaymentRequestMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.ProcessStage
import com.example.ordermicroservice.document.ShippingOutbox
import com.example.ordermicroservice.vo.PaymentIntentTokenVo
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class ShippingOutboxService(
    private val mongoTemplate: MongoTemplate,
    private val paymentRequestKafkaTemplate: KafkaTemplate<String, PaymentRequestMessage>,
    @Qualifier("noProducerAvailableExceptionRetryTemplate") private val noProducerAvailableExceptionRetryTemplate: RetryTemplate
) {
    fun cancelShippingOutboxPoller(): List<ShippingOutbox> {
        return mongoTemplate.find(Query(Criteria.where("processStage").`is`(ProcessStage.CANCELED)), ShippingOutbox::class.java)
    }

    fun cancelShippingOutboxPublisher(shippingOutbox: ShippingOutbox) {
        noProducerAvailableExceptionRetryTemplate.execute<CompletableFuture<SendResult<String, PaymentRequestMessage>>, Throwable> {
            paymentRequestKafkaTemplate.executeInTransaction {
                val paymentRequest = PaymentRequestMessage.newBuilder()
                    .setPaymentIntentToken(shippingOutbox.paymentIntentToken)
                    .setType(PAYMENT_TYPE.REFUND)
                    .build()
                it.send(KafkaTopicNames.PAYMENT_REQUEST, shippingOutbox.paymentIntentToken, paymentRequest)
            }
        }
    }

    @Scheduled(fixedRate = 10000)
    fun processCanceledShippingOutbox() {
        val outboxes = cancelShippingOutboxPoller()

        for(outbox in outboxes) {
            outbox.processStage = ProcessStage.CANCELED

            cancelShippingOutboxPublisher(outbox)

            mongoTemplate.save(outbox)
        }
    }
}