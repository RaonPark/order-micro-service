package com.example.ordermicroservice.service

import com.avro.payment.PaymentOutboxMessage
import com.avro.payment.PaymentStatusMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.PaymentOutbox
import com.example.ordermicroservice.document.PaymentType
import com.example.ordermicroservice.document.Payments
import com.example.ordermicroservice.document.ProcessStage
import com.example.ordermicroservice.dto.*
import com.example.ordermicroservice.repository.mongo.PaymentOutboxRepository
import com.example.ordermicroservice.repository.mongo.PaymentRepository
import com.example.ordermicroservice.support.DateTimeSupport
import com.example.ordermicroservice.vo.PaymentVo
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val paymentOutboxRepository: PaymentOutboxRepository,
    private val redisService: RedisService,
    private val paymentOutboxTemplate: KafkaTemplate<String, PaymentOutboxMessage>,
    private val paymentStatusTemplate: KafkaTemplate<String, PaymentStatusMessage>,
) {
    companion object {
        val log = KotlinLogging.logger {  }
        val restClient = RestClient.create()
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

    @RetryableTopic(
        retryTopicSuffix = "-retry",
        dltTopicSuffix = "-dlt",
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        concurrency = "3",
        numPartitions = "10",
        replicationFactor = "3",
        dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
        backoff = Backoff(value = 1000L, multiplier = 2.0, maxDelay = 20000L, random = true),
        kafkaTemplate = "paymentOutboxTemplate"
    )
    @KafkaListener(topics = [KafkaTopicNames.PAYMENT_OUTBOX],
        groupId = "PAYMENT_OUTBOX",
        containerFactory = "paymentOutboxListenerContainer",
        concurrency = "3")
    fun processOutbox(record: ConsumerRecord<String, PaymentOutboxMessage>, ack: Acknowledgment) {
        val outbox = record.value()

        if(ProcessStage.of(outbox.processStage.name) != ProcessStage.PENDING) {
            return
        }

        log.info { "${outbox.paymentId}에 해당하는 결제정보 프로세싱" }
        val payment = paymentRepository.findByPaymentId(outbox.paymentId)
            ?: throw RuntimeException("${outbox.paymentId}에 해당하는 결제정보가 없습니다!")

        var processedTime = ""
        if(payment.paymentType != PaymentType.CASH) {
            //  TODO: payment 알고리즘 추가 필요!
            // TODO: return 값은 여러 가지를 포함하겠지만 타임스탬프는 항상 포함!
            val payResponse = restClient.post()
                .uri("http://localhost:8080/withdraw")
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
        paymentStatusTemplate.send(KafkaTopicNames.PAYMENT_STATUS, payment.paymentId, paymentStatus)

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
    fun sendPaymentStatus(
        record: ConsumerRecord<String, PaymentStatusMessage>,
    ) {
        val paymentStatus = record.value()

        log.info { "${paymentStatus.paymentId}에 대한 결제가 완료되었습니다." }
    }
}