package com.example.ordermicroservice.controller

import com.example.ordermicroservice.dto.SavePayRequest
import com.example.ordermicroservice.dto.SavePayResponse
import com.example.ordermicroservice.service.PaymentService
import com.example.ordermicroservice.support.ServiceController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ServiceController
class PaymentController(
    private val paymentService: PaymentService,
) {
    @PostMapping("/savePaymentInfo")
    fun savePaymentInfo(@RequestBody payment: SavePayRequest): String {
        return paymentService.generatePaymentIntentToken(payment)
    }
}