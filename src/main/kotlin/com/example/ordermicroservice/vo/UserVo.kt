package com.example.ordermicroservice.vo

data class UserVo(
    val username: String,
) {
    companion object {
        fun of(username: String): UserVo =
            UserVo(username)
    }
}