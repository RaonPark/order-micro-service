package com.example.ordermicroservice.controller

import com.example.ordermicroservice.dto.GetUserResponse
import com.example.ordermicroservice.service.UserService
import com.example.ordermicroservice.support.ServiceController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ServiceController
class UserController(
    private val userService: UserService
) {
    @GetMapping("/user")
    fun getUserInfo(@RequestParam("userId") userId: String): GetUserResponse {
        return userService.getUserInfo(userId)
    }
}