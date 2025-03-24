package com.example.ordermicroservice.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "users")
data class Users(
    @Id val id: String? = null,
    val name: String,
    @Indexed(unique = true)
    val userId: String,
    val password: String,
    val addressList: List<UserAddress>
) {
}