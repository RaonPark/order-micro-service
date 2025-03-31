package com.example.ordermicroservice.service

import com.avro.account.AccountRequestMessage
import com.example.ordermicroservice.vo.PaymentVo
import com.example.ordermicroservice.vo.UserVo
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
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
    companion object {
        val log = KotlinLogging.logger {  }
    }

    fun saveUserVo(userId: String, user: UserVo) {
        val userVo = objectMapper.writeValueAsString(user)
        redisTemplate.opsForValue().setIfAbsent(userId, userVo, Duration.ofMinutes(30))
    }

    fun getUserVo(userId: String): UserVo {
        val userString = redisTemplate.opsForValue().get(userId).toString()
        val userVo = objectMapper.readValue(userString, UserVo::class.java)

        return userVo
    }

    fun saveBalance(accountNumber: String, balance: Long) {
        numericRedisTemplate.opsForValue().set(accountNumber, balance)
    }

    fun incrBalance(accountNumber: String, amount: Long) {
        numericRedisTemplate.opsForValue().increment(accountNumber, amount)
    }

    fun getBalance(accountNumber: String): Long {
        return numericRedisTemplate.opsForValue().get(accountNumber) ?: -1L
    }

    fun savePayment(aggId: String, payment: PaymentVo) {
        val paymentString = objectMapper.writeValueAsString(payment)
        redisTemplate.opsForHash<String, Any>().putIfAbsent("payment", aggId, paymentString)
    }

    fun deletePayment(aggId: String): PaymentVo {
        val payment = redisTemplate.opsForHash<String, Any>().get("payment", aggId) as String
        return objectMapper.readValue(payment, PaymentVo::class.java)
    }

    fun saveGatewayNumber(userId: String, gateway: String) {
        redisTemplate.opsForHash<String, String>().put("gateway", userId, gateway)
    }

    fun getGatewayNumber(userId: String, gateway: String): String {
        return redisTemplate.opsForHash<String, String>().get("gateway", userId)
            ?: throw RuntimeException("$userId 는 $gateway 에 연결되어 있지 않습니다.")
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