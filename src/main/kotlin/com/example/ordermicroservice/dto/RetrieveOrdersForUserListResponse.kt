package com.example.ordermicroservice.dto

data class RetrieveOrdersForUserListResponse(
    val order: List<RetrieveOrdersForUserDTO>
)