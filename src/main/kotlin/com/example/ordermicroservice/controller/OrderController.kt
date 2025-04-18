package com.example.ordermicroservice.controller

import com.example.ordermicroservice.dto.CreateOrderRequest
import com.example.ordermicroservice.dto.CreateOrderResponse
import com.example.ordermicroservice.service.OrderService
import com.example.ordermicroservice.support.ServiceController
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.concurrent.CompletableFuture

@ServiceController
class OrderController(
    private val orderService: OrderService
) {
}