package com.example.ordermicroservice.repository.mongo

import com.example.ordermicroservice.document.OrderOutbox
import com.example.ordermicroservice.document.ProcessStage
import org.springframework.data.mongodb.repository.MongoRepository

interface OrderOutboxRepository: MongoRepository<OrderOutbox, String> {
    fun findByProcessStage(processStage: ProcessStage): List<OrderOutbox>
    fun findByOrderId(orderId: String): OrderOutbox?
}