package com.example.ordermicroservice.dto

data class ProduceOrderRequest(
    val orderRequest: CreateOrderRequest,
    val payRequest: SavePayRequest
) {
}