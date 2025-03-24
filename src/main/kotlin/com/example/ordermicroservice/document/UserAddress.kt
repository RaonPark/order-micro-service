package com.example.ordermicroservice.document

import org.springframework.data.mongodb.core.geo.GeoJsonPoint

data class UserAddress(
    val name: String,
    val stringAddress: String,
    val address: GeoJsonPoint
) {
}