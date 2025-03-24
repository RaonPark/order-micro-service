package com.example.ordermicroservice.repository.mongo

import com.example.ordermicroservice.document.Shipping
import org.springframework.data.mongodb.repository.MongoRepository

interface ShippingRepository: MongoRepository<Shipping, String> {
}