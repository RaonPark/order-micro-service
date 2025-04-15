package com.example.ordermicroservice.service

import com.avro.order.OrderRefundMessage
import com.avro.payment.PAYMENT_TYPE
import com.avro.payment.PaymentRequestMessage
import com.avro.shipping.ShippingMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.ServiceProcessStage
import com.example.ordermicroservice.document.Shipping
import com.example.ordermicroservice.repository.mongo.ShippingRepository
import com.example.ordermicroservice.support.RetryableTopicForShippingTopic
import com.example.ordermicroservice.vo.Compensation
import com.example.ordermicroservice.vo.OrderCompensation
import com.example.ordermicroservice.vo.PaymentCompensation
import com.example.ordermicroservice.vo.PaymentIntentTokenVo
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.kafka.annotation.DltHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Service
class ShippingService(
    private val shippingRepository: ShippingRepository,
    private val redisService: RedisService,
    private val compensationKafkaTemplate: KafkaTemplate<String, Compensation>,
    private val paymentRequestKafkaTemplate: KafkaTemplate<String, PaymentRequestMessage>,
) {
    companion object {
        val log = KotlinLogging.logger {  }
    }
    fun createShipping() {

    }


    @RetryableTopicForShippingTopic
    @KafkaListener(topics = [KafkaTopicNames.SHIPPING],
        groupId = "Shipping_Consumer",
        containerFactory = "shippingListenerContainer",
        concurrency = "3")
    fun processShipping(record: ConsumerRecord<String, ShippingMessage>, ack: Acknowledgment) {
        val orderNumber = record.key()
        val shippingMessage = record.value()

        val shipping = buildShipping(orderNumber, shippingMessage)

        shippingRepository.save(shipping)
        log.info { "$shipping 에 대한 배송정보가 저장되었습니다." }

        paymentRequestKafkaTemplate.executeInTransaction {
            val paymentRequest = PaymentRequestMessage.newBuilder()
                .setPaymentIntentToken(shippingMessage.paymentIntentToken)
                .setType(PAYMENT_TYPE.PAY)
                .build()

            it.send(KafkaTopicNames.PAYMENT_REQUEST, shippingMessage.paymentIntentToken, paymentRequest)
        }

        ack.acknowledge()
    }

    @DltHandler
    fun shippingCompensation(shippingMessage: ShippingMessage) {
        val compensationVo = Compensation(
            orderNumber = shippingMessage.orderId,
            exceptStep = "SHIPPING"
        )

        val shipping = shippingRepository.findByOrderNumber(shippingMessage.orderId)

        if(shipping != null) {
            shippingRepository.deleteById(shipping.id!!)
        }

        log.info { "${shippingMessage.orderId} 에 해당하는 배송 서비스 오류로 인한 보상 로직 처리!" }

        compensationKafkaTemplate.executeInTransaction {
            it.send(KafkaTopicNames.COMPENSATION_REQUEST, "SHIPPING", compensationVo)
        }
    }

    private fun buildShipping(orderNumber: String, shippingMessage: ShippingMessage): Shipping {
        return Shipping(
            orderNumber = orderNumber,
            address = GeoJsonPoint(shippingMessage.shippingLocation.longitude,
                shippingMessage.shippingLocation.latitude),
            userId = shippingMessage.username,
            processStage = ServiceProcessStage.CONFIRM
        )
    }

    @KafkaListener(
        topics = [KafkaTopicNames.SHIPPING_CANCEL],
        concurrency = "3",
        groupId = "ORDER_REFUND",
        containerFactory = "refundOrderListenerContainerFactory"
    )
    fun processRefundShipping(record: ConsumerRecord<String, OrderRefundMessage>, ack: Acknowledgment) {
        val orderRefund = record.value()

        val shippingRefund = shippingRepository.findByOrderNumber(orderId = orderRefund.orderNumber)
            ?: throw RuntimeException("해당하는 배송정보가 없습니다.")

        shippingRefund.processStage = ServiceProcessStage.CANCELED

        shippingRepository.save(shippingRefund)

        paymentRequestKafkaTemplate.executeInTransaction {
            val paymentRequest = PaymentRequestMessage.newBuilder()
                .setPaymentIntentToken(orderRefund.paymentNumber)
                .setType(PAYMENT_TYPE.REFUND)
                .build()

            it.send(KafkaTopicNames.PAYMENT_REQUEST, orderRefund.paymentNumber, paymentRequest)
        }

        ack.acknowledge()
    }
}