package com.example.ordermicroservice.dto

import com.example.ordermicroservice.document.Products

data class CreateOrderRequest(
    val userId: String,
    val products: List<Products>,
    val sellerId: String
) {
}