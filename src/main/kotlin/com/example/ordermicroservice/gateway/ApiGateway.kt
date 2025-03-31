package com.example.ordermicroservice.gateway

import com.example.ordermicroservice.dto.CreateOrderRequest
import com.example.ordermicroservice.dto.CreateOrderResponse
import com.example.ordermicroservice.service.RedisService
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.StreamUtils
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.ContentCachingRequestWrapper
import java.nio.charset.StandardCharsets

@RestController
class ApiGateway (
    private val redisService: RedisService,
    private val objectMapper: ObjectMapper
) {
    companion object {
        val restClient = RestClient.create("http://localhost:8080")
        val log = KotlinLogging.logger {  }
    }

    @PostMapping("/gateway/**")
    fun postGateway(httpServletRequest: HttpServletRequest) {
        if(httpServletRequest !is ContentCachingRequestWrapper) {
            log.info { "Request Servlet이 감싸져 있지 않습니다." }
            return
        }
        if(httpServletRequest.method != "POST") {
            throw RuntimeException("${httpServletRequest.requestURI} 는 POST가 아닙니다.")
        }
        val requestUri = httpServletRequest.requestURI.replace("/gateway", "")
        log.info { requestUri }

        val body = StreamUtils.copyToString(httpServletRequest.inputStream, StandardCharsets.UTF_8)
        log.info { body }

        if(requestUri.contains("Order")) {
            val result = restClient.post()
                .uri(requestUri)
                .headers {
                    for(headerName in httpServletRequest.headerNames) {
                        it.set(headerName, httpServletRequest.getHeader(headerName))
                    }
                }
                .body(body)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .body<CreateOrderResponse>()
            log.info { "결과 = $result" }
        } else if(requestUri.contains("pay")) {
            TODO("NOT implemented!")
        }
    }
}
