package com.example.ordermicroservice.controller

import com.example.ordermicroservice.dto.CreateOrderRequest
import com.example.ordermicroservice.dto.CreateOrderResponse
import com.example.ordermicroservice.service.OrderService
import com.example.ordermicroservice.service.RedisService
import com.example.ordermicroservice.support.ServiceController
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@ServiceController
class TestController(
    private val orderService: OrderService,
    private val redisService: RedisService
) {
    companion object {
        val log = KotlinLogging.logger {  }
    }
    @PostMapping("/testOrder")
    fun testOrder(@RequestBody orderRequest: CreateOrderRequest) {
        runBlocking {
            val startTime = System.currentTimeMillis()
            redisService.saveTimestamp(startTime)
            log.info { "start time = $startTime" }
            val jobs = List(1_000) {
                launch(Dispatchers.IO) {
                    orderService.createOrder(orderRequest)
                }
            }

            jobs.forEach { it.join() }
        }
    }
}