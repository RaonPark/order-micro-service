package com.example.ordermicroservice.dto

import com.example.ordermicroservice.document.Products

data class RetrieveOrdersForSellerResponse(
    val orderNumber: String,
    val products: List<Products>,
    val userId: String,
    val shippingLocation: String,
    val orderedTime: String,
    val sellerId: String,
) {

}