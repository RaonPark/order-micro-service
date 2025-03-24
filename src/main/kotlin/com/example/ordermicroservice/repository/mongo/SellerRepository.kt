package com.example.ordermicroservice.repository.mongo

import com.example.ordermicroservice.document.Sellers
import org.springframework.data.mongodb.repository.MongoRepository

interface SellerRepository: MongoRepository<Sellers, String> {
    fun findBySellerId(sellerId: String): Sellers?
}