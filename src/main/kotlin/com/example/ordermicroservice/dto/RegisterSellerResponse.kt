package com.example.ordermicroservice.dto

data class RegisterSellerResponse(
    val sellerId: String,
    val businessName: String,
    val created: Boolean
) {

}
