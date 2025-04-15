package com.example.ordermicroservice.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("ShippingOutbox")
data class ShippingOutbox(
    @Id
    val id: String? = null,
    val orderNumber: String,
    val paymentIntentToken: String,
    var processStage: ProcessStage,
) {
    companion object {
        fun of(id: String? = null, orderNumber: String, paymentIntentToken: String, processStage: ProcessStage): ShippingOutbox {
            return ShippingOutbox(id, orderNumber, paymentIntentToken, processStage)
        }
    }
}