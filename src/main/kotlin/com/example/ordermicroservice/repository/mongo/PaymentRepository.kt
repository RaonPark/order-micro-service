package com.example.ordermicroservice.repository.mongo

import com.example.ordermicroservice.document.Payments
import org.springframework.data.mongodb.repository.MongoRepository

interface PaymentRepository: MongoRepository<Payments, String> {
    fun findByPaymentId(paymentId: String): Payments?
}