package com.example.ordermicroservice.document

import lombok.Data
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Data
@Document
data class PaymentStatus(
    @Id val id: String? = null,
    val completed: Boolean,
    val processedTime: String,
    val paymentId: String,
) {
    companion object {
        fun of(id: String? = null, completed: Boolean, processedTime: String, paymentId: String): PaymentStatus {
            return PaymentStatus(id, completed, processedTime, paymentId)
        }
    }
}