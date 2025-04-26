package com.example.ordermicroservice.dto

import com.example.ordermicroservice.document.Products
import com.example.ordermicroservice.es.repo.ShippingStatus

data class RetrieveOrdersForUserDTO(
    val orderNumber: String,
    val orderedTime: String,
    val products: List<Products>,
    val shippingStatus: ShippingStatus = ShippingStatus.BEFORE_SHIPPING
)