package com.example.ordermicroservice.repository.mongo

import com.example.ordermicroservice.document.Users
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository: MongoRepository<Users, String> {
    fun findByUserId(userId: String): Users?
}