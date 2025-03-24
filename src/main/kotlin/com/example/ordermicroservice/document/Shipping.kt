package com.example.ordermicroservice.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "shipping")
data class Shipping(
    @Id val id: String? = null,
    val orderNumber: String,
    val address: GeoJsonPoint,
    val userId: String,
) {
}