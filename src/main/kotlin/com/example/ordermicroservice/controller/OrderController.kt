package com.example.ordermicroservice.controller

import com.example.ordermicroservice.dto.CreateOrderRequest
import com.example.ordermicroservice.dto.CreateOrderResponse
import com.example.ordermicroservice.dto.RetrieveOrdersForSellerListResponse
import com.example.ordermicroservice.dto.RetrieveOrdersForUserListResponse
import com.example.ordermicroservice.service.OrderService
import com.example.ordermicroservice.support.ServiceController
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import java.util.concurrent.CompletableFuture

@ServiceController
class OrderController(
    private val orderService: OrderService
) {

    @GetMapping("/retrieveOrdersForSeller")
    fun retrieveOrdersForSeller(@RequestParam("sellerId")sellerId: String): RetrieveOrdersForSellerListResponse {
        return orderService.retrieveOrdersForSeller(sellerId)
    }

    @GetMapping("/retrieveOrdersForUser")
    fun retrieveOrdersForUser(@RequestParam("userId")userId: String): RetrieveOrdersForUserListResponse {
        return orderService.retrieveOrdersForUser(userId)
    }
}