package com.example.ordermicroservice.repository.mongo

import com.example.ordermicroservice.document.Orders
import org.springframework.data.mongodb.repository.MongoRepository

interface OrderRepository: MongoRepository<Orders, String> {
    fun findByOrderNumber(orderNumber: String): Orders?
}