package com.example.ordermicroservice.dto

data class WithdrawRequest(
    val accountNumber: String,
    val amount: Long,
) {
}