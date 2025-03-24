package com.example.ordermicroservice.dto

import org.springframework.data.mongodb.core.geo.GeoJsonPoint

data class GetSellerResponse(
    val sellerName: String,
    val address: String
) {
    companion object {
        fun of(sellerName: String, address: String): GetSellerResponse {
            return GetSellerResponse(sellerName, address)
        }
    }
}