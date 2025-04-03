package com.example.ordermicroservice.service

import com.avro.shipping.ShippingMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.Shipping
import com.example.ordermicroservice.repository.mongo.ShippingRepository
import com.example.ordermicroservice.vo.OrderCompensation
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Service
class ShippingService(
    private val shippingRepository: ShippingRepository,
    private val redisService: RedisService,
    private val orderCompensationKafkaTemplate: KafkaTemplate<String, OrderCompensation>
) {
    companion object {
        val log = KotlinLogging.logger {  }
    }
    fun createShipping() {

    }

    @KafkaListener(topics = [KafkaTopicNames.SHIPPING],
        groupId = "Shipping_Consumer",
        containerFactory = "shippingListenerContainer",
        concurrency = "3")
    fun processShipping(record: ConsumerRecord<String, ShippingMessage>, ack: Acknowledgment) {
        val orderNumber = record.key()
        val shippingMessage = record.value()

        if(record.timestamp() % 2 == 0L) {
            orderCompensationKafkaTemplate.executeInTransaction {
                it.send(KafkaTopicNames.ORDER_COMPENSATION, orderNumber, OrderCompensation(
                    orderNumber = orderNumber, exceptionStep = "SHIPPING"
                ))
            }
            return
        }

        val shipping = buildShipping(orderNumber, shippingMessage)

        shippingRepository.save(shipping)
        log.info { "$shipping 에 대한 배송정보가 저장되었습니다." }

        val timestamp = redisService.getTimestamp()
        val ms = System.currentTimeMillis() - timestamp
        log.info { "걸린 시간 = $ms ms" }

        ack.acknowledge()
    }

    private fun buildShipping(orderNumber: String, shippingMessage: ShippingMessage): Shipping {
        return Shipping(
            orderNumber = orderNumber,
            address = GeoJsonPoint(shippingMessage.shippingLocation.longitude,
                shippingMessage.shippingLocation.latitude),
            userId = shippingMessage.username
        )
    }
}