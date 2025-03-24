package com.example.ordermicroservice.dto

import com.example.ordermicroservice.document.Products

data class CreateOrderResponse(
    val orderNumber: String,
    val username: String,
    val products: List<Products>,
    val amount: Long,
    val address: String,
    val sellerName: String,
    val orderedTime: String,
) {
    companion object {
        fun of(orderNumber: String, username: String, products: List<Products>,
               amount: Long, address: String, sellerName: String, orderedTime: String): CreateOrderResponse =
            CreateOrderResponse(orderNumber, username, products, amount, address, sellerName, orderedTime)
    }
}