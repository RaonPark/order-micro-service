package com.example.ordermicroservice.dto

import com.example.ordermicroservice.document.DepositResult

data class DepositResponse(
    val accountNumber: String,
    val result: DepositResult
) {
    companion object {
        fun of(accountNumber: String, depositResult: DepositResult): DepositResponse {
            return DepositResponse(accountNumber, depositResult)
        }
    }
}