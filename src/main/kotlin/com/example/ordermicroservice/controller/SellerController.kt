package com.example.ordermicroservice.controller

import com.example.ordermicroservice.dto.GetSellerResponse
import com.example.ordermicroservice.service.SellerService
import com.example.ordermicroservice.support.ServiceController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ServiceController
class SellerController(
    private val sellerService: SellerService
) {
    @GetMapping("/seller")
    fun getSellerInfo(@RequestParam("sellerId") sellerId: String): GetSellerResponse {
        return sellerService.getSellerInfo(sellerId = sellerId)
    }
}