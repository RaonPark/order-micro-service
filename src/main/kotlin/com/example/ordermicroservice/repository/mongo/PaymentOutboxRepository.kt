package com.example.ordermicroservice.repository.mongo

import com.example.ordermicroservice.document.PaymentOutbox
import com.example.ordermicroservice.document.ProcessStage
import org.springframework.data.mongodb.repository.MongoRepository

interface PaymentOutboxRepository: MongoRepository<PaymentOutbox, String> {
    fun findByProcessStage(processStage: ProcessStage): List<PaymentOutbox>
}