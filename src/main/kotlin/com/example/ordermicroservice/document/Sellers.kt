package com.example.ordermicroservice.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "sellers")
data class Sellers(
    @Id val id: String? = null,
    val sellerId: String,
    val accountNumber: String,
    val address: String,
    val phoneNumber: String,
    val registeredDate: String,
    val businessName: String,
) {
}