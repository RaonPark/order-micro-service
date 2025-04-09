package com.example.ordermicroservice.vo

data class PaymentCompensation(
    val exceptionStep: String,
    val orderNumber: String
) {
}