package com.example.ordermicroservice.service

import com.avro.payment.PAYMENT_TYPE
import com.avro.payment.PaymentOutboxMessage
import com.avro.payment.PaymentRequestMessage
import com.avro.payment.PaymentStatusMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.PaymentOutbox
import com.example.ordermicroservice.document.PaymentType
import com.example.ordermicroservice.document.Payments
import com.example.ordermicroservice.document.ProcessStage
import com.example.ordermicroservice.dto.*
import com.example.ordermicroservice.repository.mongo.PaymentOutboxRepository
import com.example.ordermicroservice.repository.mongo.PaymentRepository
import com.example.ordermicroservice.repository.mysql.CardRepository
import com.example.ordermicroservice.support.DateTimeSupport
import com.example.ordermicroservice.support.MachineIdGenerator
import com.example.ordermicroservice.support.RetryableTopicForPaymentTopic
import com.example.ordermicroservice.support.SnowflakeIdGenerator
import com.example.ordermicroservice.vo.Compensation
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

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val paymentOutboxRepository: PaymentOutboxRepository,
    private val redisService: RedisService,
    private val paymentStatusTemplate: KafkaTemplate<String, PaymentStatusMessage>,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    @Qualifier("readTimeoutExceptionRetryTemplate") private val readTimeoutExceptionRetryTemplate: RetryTemplate,
    private val cardRepository: CardRepository,
) {
    companion object {
        val log = KotlinLogging.logger {  }
        val restClient = RestClient.create("http://localhost:8080")
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
        containerFactory = "paymentRequestListenerContainerFactory",
        groupId = "PROCESS_PAYMENT"
    )
    fun requestPay(record: ConsumerRecord<String, PaymentRequestMessage>) {
        val paymentRequest = record.value()

        log.info { "paymentRequest is here ! = $paymentRequest" }

        when(paymentRequest.type) {
            PAYMENT_TYPE.PAY -> {
                savePay(paymentRequest.paymentIntentToken)
            }
            PAYMENT_TYPE.REFUND -> {
                refundPayment(paymentIntentToken = paymentRequest.paymentIntentToken)
            }
            else -> {
                log.info { "옳지 않은 결제 타입입니다. ${paymentRequest.type}" }
            }
        }
    }

    private fun savePay(paymentIntentToken: String) {
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

    private fun refundPayment(paymentIntentToken: String) {
        val payment = paymentRepository.findByPaymentId(paymentIntentToken)
            ?: throw RuntimeException("$paymentIntentToken 에 해당하는 거래가 없습니다.")

        if(payment.processStage == ProcessStage.PROCESSED) {
            payment.processStage = ProcessStage.BEFORE_CANCEL

            val outbox = PaymentOutbox.of(id = null, aggId = paymentIntentToken,
                processStage = payment.processStage, paymentId = paymentIntentToken)

            paymentOutboxRepository.save(outbox)
            paymentRepository.save(payment)
        }
    }

    @RetryableTopicForPaymentTopic
    @KafkaListener(topics = [KafkaTopicNames.PAYMENT_OUTBOX],
        groupId = "PAYMENT_OUTBOX",
        containerFactory = "paymentOutboxListenerContainer",
        concurrency = "3"
    )
    fun processOutbox(record: ConsumerRecord<String, PaymentOutboxMessage>, ack: Acknowledgment) {
        val outbox = record.value()

        val outboxStage = ProcessStage.of(outbox.processStage.name)

        log.info { "결제정보 : $outbox, outbox stage = $outboxStage" }

        if(!(outboxStage == ProcessStage.PENDING || outboxStage == ProcessStage.BEFORE_CANCEL)) {
            return
        }

        log.info { "${outbox.paymentId}에 해당하는 결제정보 프로세싱" }
        val payment = paymentRepository.findByPaymentId(outbox.paymentId)
            ?: throw RuntimeException("${outbox.paymentId}에 해당하는 결제정보가 없습니다!")

        log.info { "결제정보 = $payment" }

        val processedTime: String
        if(payment.paymentType == PaymentType.DEBIT || payment.paymentType == PaymentType.CREDIT) {
            val accountNumber = cardRepository.findAccountNumberByCardNumber(payment.cardNumber!!)
                ?: throw RuntimeException("${payment.cardNumber}에 해당하는 계좌가 존재하지 않습니다!")

            log.info { "$accountNumber 에 출금 요청 진행" }

            when(outbox.processStage) {
                com.avro.support.ProcessStage.PENDING -> {
                    val payResponse = readTimeoutExceptionRetryTemplate.execute<WithdrawResponse, Throwable> {
                        restClient.post()
                            .uri("/service/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(
                                WithdrawRequest(
                                    accountNumber = accountNumber,
                                    amount = payment.amount
                                )
                            )
                            .retrieve()
                            .body(WithdrawResponse::class.java)
                            ?: throw RuntimeException("페이 서비스가 올바르게 수행되고 있지 않습니다.")
                    }

                    log.info { "$accountNumber 에 출금 요청 완료. ${payResponse.processedTime}" }

                    if (!payResponse.isValid || !payResponse.isCompleted) {
                        throw RuntimeException("페이 서비스에서 문제가 생겼습니다.")
                    }

                    processedTime = payResponse.processedTime
                    payment.processStage = ProcessStage.PROCESSED
                    outbox.processStage = com.avro.support.ProcessStage.PROCESSED
                }
                com.avro.support.ProcessStage.BEFORE_CANCEL -> {
                    val payResponse = readTimeoutExceptionRetryTemplate.execute<DepositResponse, Throwable> {
                        restClient.post()
                            .uri("/service/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(
                                DepositRequest.of(
                                    accountNumber = accountNumber,
                                    amount = payment.amount
                                )
                            )
                            .retrieve()
                            .body(DepositResponse::class.java)
                            ?: throw RuntimeException("페이 서비스가 올바르게 수행되고 있지 않습니다.")
                    }

                    log.info { "$accountNumber 에 입금 요청 완료. ${payResponse.processedTime}" }

                    if (!payResponse.result) {
                        throw RuntimeException("페이 서비스에서 문제가 생겼습니다.")
                    }

                    processedTime = payResponse.processedTime

                    payment.processStage = ProcessStage.CANCELED
                    outbox.processStage = com.avro.support.ProcessStage.CANCELED
                }
                else -> {
                    log.info { "error!" }
                    return
                }
            }
        } else {
            processedTime = DateTimeSupport.getNowTimeWithKoreaZoneAndFormatter()
        }

        paymentRepository.save(payment)
        val dbOutbox = paymentOutboxRepository.findByPaymentIdAndProcessStage(paymentId = outbox.paymentId, processStage = outboxStage)
            ?: throw RuntimeException("Error in Payment!")

        dbOutbox.processStage = ProcessStage.of(outbox.processStage.name)
        paymentOutboxRepository.save(dbOutbox)

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