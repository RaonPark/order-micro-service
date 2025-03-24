package com.example.ordermicroservice.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "paymentoutbox")
data class PaymentOutbox(
    @Id val id: String? = null,
    val aggId: String,
    var processStage: ProcessStage,
    val paymentId: String,
) {
    companion object {
        fun of(id: String? = null, aggId: String, processStage: ProcessStage, paymentId: String): PaymentOutbox {
            return PaymentOutbox(id, aggId, processStage, paymentId)
        }
    }
}