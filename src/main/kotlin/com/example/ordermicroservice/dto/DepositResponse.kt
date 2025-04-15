package com.example.ordermicroservice.dto

import com.example.ordermicroservice.document.DepositResult

data class DepositResponse(
    val accountNumber: String,
    val result: Boolean,
    val processedTime: String
) {
    companion object {
        fun of(accountNumber: String, depositResult: Boolean, processedTime: String): DepositResponse {
            return DepositResponse(accountNumber, depositResult, processedTime)
        }
    }
}