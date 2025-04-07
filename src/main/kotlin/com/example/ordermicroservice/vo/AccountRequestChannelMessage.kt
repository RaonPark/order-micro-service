package com.example.ordermicroservice.vo

data class AccountRequestChannelMessage(
    val amount: Long,
    val requestType: String,
    val accountNumber: String,
    val cacheBalance: Long,
) {
}