package com.example.ordermicroservice.dto

data class RetrieveOrdersForSellerListResponse(
    val orders: List<RetrieveOrdersForSellerDTO>
) {
}