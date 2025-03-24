package com.example.ordermicroservice.dto

data class GetUserResponse(
    val username: String,
    val location: List<Double>,
    val address: String,
) {
    companion object {
        fun of(username: String, location: List<Double>, address: String): GetUserResponse {
            return GetUserResponse(username, location, address)
        }
    }
}