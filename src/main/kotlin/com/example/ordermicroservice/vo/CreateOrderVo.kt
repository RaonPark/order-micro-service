package com.example.ordermicroservice.vo

import com.example.ordermicroservice.document.Products
import com.example.ordermicroservice.dto.CreateOrderRequest

data class CreateOrderVo(
    val userId: String,
    val products: List<Products>,
    val paymentIntentToken: String,
) {
    companion object {
        fun convertDto2Vo(orderRequest: CreateOrderRequest, paymentIntentToken: String): CreateOrderVo {
            return CreateOrderVo(orderRequest.userId, orderRequest.products, paymentIntentToken)
        }
    }
}