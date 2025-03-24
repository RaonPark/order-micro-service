package com.example.ordermicroservice.dto

data class RegisterSellerRequest(
    val address: String,
    val accountNumber: String,
    val phoneNumber: String,
    val businessName: String,
) {

}
