package com.example.ordermicroservice.dto

data class DepositRequest(
    val accountNumber: String,
    val amount: Long
) {
    companion object {
        fun of(accountNumber: String, amount: Long): DepositRequest {
            return DepositRequest(accountNumber, amount)
        }
    }
}