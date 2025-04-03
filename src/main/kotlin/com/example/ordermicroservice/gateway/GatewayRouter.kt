package com.example.ordermicroservice.gateway

object GatewayRouter {
    fun orderRouter(requestUri: String): Boolean =
        requestUri.contains("order") || requestUri.contains("Order")

    fun payRouter(requestUri: String): Boolean =
        requestUri.contains("pay") || requestUri.contains("Pay")

    fun isService(requestUri: String): Boolean =
        requestUri.contains("service")
}