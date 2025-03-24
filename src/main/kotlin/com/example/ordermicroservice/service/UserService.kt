package com.example.ordermicroservice.service

import com.example.ordermicroservice.dto.GetUserResponse
import com.example.ordermicroservice.dto.LoginRequest
import com.example.ordermicroservice.dto.LoginResponse
import com.example.ordermicroservice.repository.mongo.UserRepository
import com.example.ordermicroservice.vo.UserVo
import org.springframework.data.mongodb.core.geo.GeoJson
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val redisService: RedisService
) {
    fun login(login: LoginRequest): LoginResponse {
        val user = userRepository.findByUserId(login.userId)
            ?: throw RuntimeException("${login.userId}에 해당하는 유저가 없습니다!")
        val userVo = UserVo.of(
            username = user.name
        )
        redisService.saveUserVo(login.userId, userVo)

        return LoginResponse(userId = user.userId)
    }

    fun getUserInfo(userId: String): GetUserResponse {
        val user = userRepository.findByUserId(userId)
            ?: throw RuntimeException("${userId}에 해당하는 유저가 없습니다!")

        return GetUserResponse.of(
            username = user.name,
            address = user.addressList[0].stringAddress,
            location = listOf(user.addressList[0].address.x, user.addressList[0].address.y)
        )
    }
}