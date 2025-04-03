package com.example.ordermicroservice.gateway

import com.example.ordermicroservice.document.Products
import com.example.ordermicroservice.dto.CreateOrderRequest
import com.example.ordermicroservice.dto.CreateOrderResponse
import com.example.ordermicroservice.service.RedisService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

class ApiGatewayTest: BehaviorSpec({
    extensions(SpringExtension)
    
    val redisService = mockk<RedisService>()
    val controller = ApiGateway(redisService)

    Given("controller") {
        val response = RestClient.create("http://localhost:8080")
            .post().uri("/createOrder")
            .body(CreateOrderRequest(
                userId = "raonpark",
                products = listOf(
                    Products(name = "Iphone 16 Pro", price = 2000, quantity = 2)
                ),
                sellerId = "1"
            ))
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body<CreateOrderResponse>()
        When("") {
            Then("") {
                response?.username shouldBe "raonpark"
            }
        }
    }
})