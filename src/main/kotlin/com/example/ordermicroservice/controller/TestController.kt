package com.example.ordermicroservice.controller

import com.example.ordermicroservice.dto.CreateOrderRequest
import com.example.ordermicroservice.dto.DepositRequest
import com.example.ordermicroservice.dto.WithdrawRequest
import com.example.ordermicroservice.service.AccountService
import com.example.ordermicroservice.service.OrderService
import com.example.ordermicroservice.service.RedisService
import com.example.ordermicroservice.support.ServiceController
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import kotlin.random.Random

@ServiceController
class TestController(
    private val orderService: OrderService,
    private val redisService: RedisService,
    private val accountService: AccountService,
    private val numericRedisTemplate: RedisTemplate<String, Long>
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
                    orderService.createOrder(orderRequest,)
                }
            }

            jobs.forEach { it.join() }
        }
    }

    @GetMapping("/testAccount")
    fun testAccount() {
        val rand = Random(2000)
        val order = rand.nextInt()

        runBlocking {
            if(order % 2 == 0) {
                accountService.withdrawNew(WithdrawRequest(accountNumber = "123-123-123-123", amount = 10))
            } else if(order % 3 == 0) {
                accountService.depositNew(DepositRequest(accountNumber = "123-123-123-123", amount = 100))
            } else {
                accountService.inquiry("123-123-123-123")
            }
        }

    }
}