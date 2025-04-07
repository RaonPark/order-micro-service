package com.example.ordermicroservice.repository.mongo

import com.example.ordermicroservice.entity.Accounts
import org.springframework.data.mongodb.repository.MongoRepository

interface AccountRepository: MongoRepository<Accounts, String> {
    fun findByAccountNumber(accountNumber: String): Accounts
}