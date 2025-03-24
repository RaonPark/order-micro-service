package com.example.ordermicroservice.service

import com.avro.PaymentOutboxMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.PaymentOutbox
import com.example.ordermicroservice.document.PaymentType
import com.example.ordermicroservice.document.Payments
import com.example.ordermicroservice.document.ProcessStage
import com.example.ordermicroservice.dto.SavePayRequest
import com.example.ordermicroservice.dto.SavePayResponse
import com.example.ordermicroservice.repository.mongo.PaymentOutboxRepository
import com.example.ordermicroservice.repository.mongo.PaymentRepository
import com.example.ordermicroservice.vo.PaymentVo
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val paymentOutboxRepository: PaymentOutboxRepository,
    private val redisService: RedisService,
    private val paymentOutboxTemplate: KafkaTemplate<String, PaymentOutboxMessage>
) {
    companion object {
        val log = KotlinLogging.logger {  }
    }
    fun savePay(payment: SavePayRequest): SavePayResponse {
        val paymentId = generatePaymentId(payment)
        val paymentType = PaymentType.of(payment.paymentType)
        val createPayment = Payments.of(null, paymentId, paymentType, payment.cardNumber, payment.cardCvc, payment.amount, processStage = ProcessStage.BEFORE_PROCESS)
        val aggId = redisService.getAggregatorId()

        val savedPayment = CompletableFuture.supplyAsync {
            val savedPayment = paymentRepository.save(createPayment)
            redisService.savePayment(aggId.toString(), PaymentVo.document2Pojo(savedPayment))

            savedPayment
        }.thenApply {
            val processStage = ProcessStage.BEFORE_PROCESS
            val outbox = PaymentOutbox.of(id = null, aggId = aggId.toString(),
                processStage = processStage, paymentId = paymentId)

            paymentOutboxRepository.save(outbox)

            it
        }.join()

        return SavePayResponse(
            amount = savedPayment.amount,
            paymentId = savedPayment.paymentId
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun generatePaymentId(payment: SavePayRequest): String {
        val nowTime = Instant.now().toEpochMilli().toHexString()
        val amount = payment.amount.toHexString()
        val snowFlake = redisService.generateRandomNumber().toHexString()

        return nowTime.plus(snowFlake).plus(amount)
    }

    @KafkaListener(topics = [KafkaTopicNames.PAYMENT_OUTBOX],
        groupId = "PAYMENT_OUTBOX",
        containerFactory = "paymentOutboxListenerContainer")
    fun processOutbox(record: ConsumerRecord<String, PaymentOutboxMessage>, ack: Acknowledgment) {
        val outbox = record.value()

        if(ProcessStage.of(outbox.processStage.name) != ProcessStage.PENDING) {
            return
        }

        log.info { "${outbox.paymentId}에 해당하는 결제정보 프로세싱" }
        val payment = paymentRepository.findByPaymentId(outbox.paymentId)
            ?: throw RuntimeException("${outbox.paymentId}에 해당하는 결제정보가 없습니다!")

        //  TODO: payment 알고리즘 추가 필요!

        payment.processStage = ProcessStage.PROCESSED
        paymentRepository.save(payment)

        // send payment is finished.
        outbox.processStage = com.avro.support.ProcessStage.PROCESSED
        paymentOutboxTemplate.send(KafkaTopicNames.PAYMENT_STATUS, outbox.aggId, outbox)

        ack.acknowledge()
    }
}