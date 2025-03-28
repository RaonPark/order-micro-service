package com.example.ordermicroservice.document

import lombok.Data
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "accounts")
data class Accounts(
    @Id val id: String? = null,
    val accountNumber: String,
    val accountPassword: String,
    val userId: String,
    val balance: Long
) {
    companion object {
        fun of(id: String? = null, accountNumber: String, accountPassword: String, userId: String, balance: Long): Accounts {
            return Accounts(id, accountNumber, accountPassword, userId, balance)
        }
    }
}