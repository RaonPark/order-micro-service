package com.example.ordermicroservice.support

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DateTimeSupport {
    companion object {
        fun getNowTimeWithKoreaZoneAndFormatter(): String {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            return Instant.now().atZone(ZoneId.of("Asia/Seoul")).format(formatter)
        }
    }
}