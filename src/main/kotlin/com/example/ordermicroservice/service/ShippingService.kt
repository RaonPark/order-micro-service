package com.example.ordermicroservice.service

import com.avro.ShippingMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.Shipping
import com.example.ordermicroservice.repository.mongo.ShippingRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class ShippingService(
    private val shippingRepository: ShippingRepository,
    private val redisService: RedisService,
    private val shippingTemplate: KafkaTemplate<String, ShippingMessage>
) {
    companion object {
        val log = KotlinLogging.logger {  }
    }
    fun createShipping() {

    }

    @KafkaListener(topics = [KafkaTopicNames.SHIPPING],
        groupId = "SHIPPING",
        containerFactory = "shippingListenerContainer")
    fun processShipping(record: ConsumerRecord<String, ShippingMessage>) {
        val orderNumber = record.key()
        val shippingMessage = record.value()
        val shipping = buildShipping(orderNumber, shippingMessage)

        shippingRepository.save(shipping)
        log.info { "$shipping 에 대한 배송정보가 저장되었습니다." }
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