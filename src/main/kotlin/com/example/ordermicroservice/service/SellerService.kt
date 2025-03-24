package com.example.ordermicroservice.service

import com.example.ordermicroservice.document.Sellers
import com.example.ordermicroservice.dto.GetSellerResponse
import com.example.ordermicroservice.dto.RegisterSellerRequest
import com.example.ordermicroservice.dto.RegisterSellerResponse
import com.example.ordermicroservice.repository.mongo.SellerRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class SellerService(
    private val sellerRepository: SellerRepository,
    private val redisService: RedisService
) {
    fun register(seller: RegisterSellerRequest): RegisterSellerResponse {
        val sellerId = generateSellerId(seller.businessName)
        val createSeller = Sellers(
            sellerId = sellerId,
            address = seller.address,
            accountNumber = seller.accountNumber,
            phoneNumber = seller.phoneNumber,
            registeredDate = getNowTime(),
            businessName = seller.businessName,
            id = ""
        )

        val savedSeller = sellerRepository.save(createSeller)

        return RegisterSellerResponse(
            sellerId = savedSeller.sellerId,
            businessName = savedSeller.businessName,
            created = true
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun generateSellerId(businessName: String): String {
        val nowTime = Instant.now().toEpochMilli().toHexString()
        val snowFlake = redisService.generateRandomNumber().toHexString()

        return nowTime.plus(businessName.last()).plus(snowFlake)
    }

    private fun getNowTime(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val nowTime = Instant.now().atZone(ZoneId.of("Asia/Seoul")).format(formatter)

        return nowTime
    }

    fun getSellerInfo(sellerId: String): GetSellerResponse {
        val seller = sellerRepository.findBySellerId(sellerId)
            ?: throw RuntimeException("${sellerId}에 해당하는 사람이 없습니다.")

        return GetSellerResponse.of(sellerName = seller.businessName, address = seller.address)
    }
}