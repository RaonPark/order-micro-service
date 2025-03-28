package com.example.ordermicroservice.dto

data class WithdrawResponse(
    val isValid: Boolean,
    val isCompleted: Boolean,
    val balance: Long,
    val processedTime: String
) {
    companion object {
        fun of(isValid: Boolean, isCompleted: Boolean, balance: Long, processedTime: String): WithdrawResponse {
            return WithdrawResponse(isValid, isCompleted, balance, processedTime)
        }
    }
}