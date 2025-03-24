package com.example.ordermicroservice.service

import com.example.ordermicroservice.vo.PaymentVo
import com.example.ordermicroservice.vo.UserVo
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RedisService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val numericRedisTemplate: RedisTemplate<String, Long>,
    private val objectMapper: ObjectMapper
) {
    fun saveUserVo(userId: String, user: UserVo) {
        val userVo = objectMapper.writeValueAsString(user)
        redisTemplate.opsForValue().setIfAbsent(userId, userVo, Duration.ofMinutes(30))
    }

    fun getUserVo(userId: String): UserVo {
        val userString = redisTemplate.opsForValue().get(userId).toString()
        val userVo = objectMapper.readValue(userString, UserVo::class.java)

        return userVo
    }

    fun savePayment(aggId: String, payment: PaymentVo) {
        val paymentString = objectMapper.writeValueAsString(payment)
        redisTemplate.opsForHash<String, Any>().putIfAbsent("payment", aggId, paymentString)
    }

    fun deletePayment(aggId: String): PaymentVo {
        val payment = redisTemplate.opsForHash<String, Any>().get("payment", aggId) as String
        return objectMapper.readValue(payment, PaymentVo::class.java)
    }

    @Scheduled(fixedRate = 1000 * 3600)
    fun initiateRandom() = numericRedisTemplate.opsForValue().set("random", 0L)

    fun generateRandomNumber(): Long =
        numericRedisTemplate.opsForValue().increment("random")!!

    fun getAggregatorId(): Long {
        numericRedisTemplate.opsForValue().setIfAbsent("aggId", 0L)
        return numericRedisTemplate.opsForValue().increment("aggId") ?: 0L
    }
}