package com.example.ordermicroservice.dto

data class SavePayRequest(
    val amount: Long,
    val paymentType: String,
    val cardNumber: String?,
    val cardCvc: String?
) {

}
