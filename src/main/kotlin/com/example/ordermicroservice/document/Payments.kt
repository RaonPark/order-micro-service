package com.example.ordermicroservice.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "payments")
data class Payments(
    @Id val id: String? = null,
    val paymentId: String,
    val paymentType: PaymentType,
    val cardNumber: String? = null,
    val cardCvc: String? = null,
    val amount: Long,
    var processStage: ProcessStage
) {
    companion object {
        fun of(id: String? = null, paymentId: String, paymentType: PaymentType, cardNumber: String?, cardCvc: String?, amount: Long, processStage: ProcessStage): Payments {
            return Payments(id, paymentId, paymentType, cardNumber, cardCvc, amount, processStage)
        }
    }
}