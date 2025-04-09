package com.example.ordermicroservice.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "orderoutbox")
data class OrderOutbox(
    @Id val id: String? = null,
    var processStage: ProcessStage,
    val aggId: String,
    val orderId: String,
    val paymentIntentToken: String,
) {
    companion object {
        fun of(id: String? = null, processStage: ProcessStage, aggId: String, orderId: String, paymentIntentToken: String): OrderOutbox {
            return OrderOutbox(id, processStage, aggId, orderId, paymentIntentToken)
        }
    }
}