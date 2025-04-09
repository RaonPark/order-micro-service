package com.example.ordermicroservice.service

import com.avro.payment.PaymentOutboxMessage
import com.avro.payment.PaymentStatusMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.PaymentOutbox
import com.example.ordermicroservice.document.PaymentType
import com.example.ordermicroservice.document.Payments
import com.example.ordermicroservice.document.ProcessStage
import com.example.ordermicroservice.dto.SavePayRequest
import com.example.ordermicroservice.dto.SavePayResponse
import com.example.ordermicroservice.dto.WithdrawRequest
import com.example.ordermicroservice.dto.WithdrawResponse
import com.example.ordermicroservice.repository.mongo.PaymentOutboxRepository
import com.example.ordermicroservice.repository.mongo.PaymentRepository
import com.example.ordermicroservice.support.DateTimeSupport
import com.example.ordermicroservice.support.MachineIdGenerator
import com.example.ordermicroservice.support.RetryableTopicForPaymentTopic
import com.example.ordermicroservice.support.SnowflakeIdGenerator
import com.example.ordermicroservice.vo.Compensation
import com.example.ordermicroservice.vo.PaymentIntentTokenVo
import com.example.ordermicroservice.vo.PaymentVo
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.kafka.annotation.DltHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Instant

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val paymentOutboxRepository: PaymentOutboxRepository,
    private val redisService: RedisService,
    private val paymentStatusTemplate: KafkaTemplate<String, PaymentStatusMessage>,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    @Qualifier("readTimeoutExceptionRetryTemplate") private val readTimeoutExceptionRetryTemplate: RetryTemplate
) {
    companion object {
        val log = KotlinLogging.logger {  }
        val restClient = RestClient.create()
    }

    fun generatePaymentIntentToken(payment: SavePayRequest): String {
       val tokenKey = SnowflakeIdGenerator(MachineIdGenerator.machineId()).nextId()

        redisService.savePayment(tokenKey.toString(),
            PaymentVo(paymentId = tokenKey.toString(),
                amount = payment.amount,
                cardNumber = payment.cardNumber,
                cardCvc = payment.cardCvc,
                paymentType = PaymentType.of(payment.paymentType)
            )
        )

        return tokenKey.toString()
    }

    @KafkaListener(
        topics = [KafkaTopicNames.PAYMENT_REQUEST],
        concurrency = "3",
        containerFactory = "processPaymentListenerContainerFactory",
        groupId = "PROCESS_PAYMENT"
    )
    fun savePay(record: ConsumerRecord<String, PaymentIntentTokenVo>) {
        val paymentIntentToken = record.value().paymentIntentToken

        val paymentVo = redisService.getPayment(paymentIntentToken)

        val paymentEntity = Payments.of(
            id = null,
            amount = paymentVo.amount,
            cardNumber = paymentVo.cardNumber,
            cardCvc = paymentVo.cardCvc,
            paymentType = paymentVo.paymentType,
            paymentId = paymentIntentToken,
            processStage = ProcessStage.BEFORE_PROCESS
        )

        val savedPayment = paymentRepository.save(paymentEntity)

        val outbox = PaymentOutbox.of(id = null, aggId = paymentIntentToken,
            processStage = savedPayment.processStage, paymentId = paymentIntentToken)

        paymentOutboxRepository.save(outbox)
    }

    @RetryableTopicForPaymentTopic
    @KafkaListener(topics = [KafkaTopicNames.PAYMENT_OUTBOX],
        groupId = "PAYMENT_OUTBOX",
        containerFactory = "paymentOutboxListenerContainer",
        concurrency = "3"
    )
    fun processOutbox(record: ConsumerRecord<String, PaymentOutboxMessage>, ack: Acknowledgment) {
        val outbox = record.value()

        if(ProcessStage.of(outbox.processStage.name) != ProcessStage.PENDING) {
            return
        }

        log.info { "${outbox.paymentId}에 해당하는 결제정보 프로세싱" }
        val payment = paymentRepository.findByPaymentId(outbox.paymentId)
            ?: throw RuntimeException("${outbox.paymentId}에 해당하는 결제정보가 없습니다!")

        val processedTime: String
        if(payment.paymentType != PaymentType.CASH) {
            val payResponse = readTimeoutExceptionRetryTemplate.execute<WithdrawResponse, Throwable> {
                restClient.post()
                    .uri("http://localhost:8080/service/withdraw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        WithdrawRequest(
                            accountNumber = "123-123-123-123",
                            amount = payment.amount
                        )
                    )
                    .retrieve()
                    .body(WithdrawResponse::class.java)
                    ?: throw RuntimeException("페이 서비스가 올바르게 수행되고 있지 않습니다.")
            }

            if (!payResponse.isValid || !payResponse.isCompleted) {
                throw RuntimeException("페이 서비스에서 문제가 생겼습니다.")
            }

            processedTime = payResponse.processedTime
        } else {
            processedTime = DateTimeSupport.getNowTimeWithKoreaZoneAndFormatter()
        }

        payment.processStage = ProcessStage.PROCESSED
        paymentRepository.save(payment)

        // send payment is finished.
        outbox.processStage = com.avro.support.ProcessStage.PROCESSED

        val paymentStatus = buildPaymentStatus(payment.paymentId, processedTime)
        paymentStatusTemplate.executeInTransaction {
            it.send(KafkaTopicNames.PAYMENT_STATUS, payment.paymentId, paymentStatus)
        }

        ack.acknowledge()
    }

    private fun buildPaymentStatus(paymentId: String, processedTime: String): PaymentStatusMessage {
        return PaymentStatusMessage.newBuilder()
            .setPaymentId(paymentId)
            .setCompleted(true)
            .setProcessedTime(processedTime)
            .build()
    }

    @KafkaListener(topics = [KafkaTopicNames.PAYMENT_STATUS],
        groupId = "PAYMENT_STATUS",
        concurrency = "3",
        containerFactory = "paymentStatusListenerContainer"
    )
    fun sendPaymentStatus(record: ConsumerRecord<String, PaymentStatusMessage>) {
        val paymentStatus = record.value()

        log.info { "${paymentStatus.paymentId}에 대한 결제가 완료되었습니다." }

        simpMessagingTemplate.convertAndSend("/topic/paymentState", "{status: true}")
    }

    @DltHandler
    fun processCompensation(compensation: Compensation) {
        val orderNumber = compensation.orderNumber

    }
}