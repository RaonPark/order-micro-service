package com.example.ordermicroservice.repository.mongo

import com.example.ordermicroservice.document.Accounts
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update

interface AccountRepository: MongoRepository<Accounts, String> {
    fun findByAccountNumber(accountNumber: String): Accounts
}