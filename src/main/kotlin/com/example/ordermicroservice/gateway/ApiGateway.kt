package com.example.ordermicroservice.gateway

import com.avro.support.ThrottlingRequest
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.dto.CreateOrderRequest
import com.example.ordermicroservice.dto.CreateOrderResponse
import com.example.ordermicroservice.dto.SavePayResponse
import com.example.ordermicroservice.service.RedisService
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.util.StreamUtils
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.ContentCachingRequestWrapper
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

@RestController
class ApiGateway (
    private val redisService: RedisService,
    private val objectMapper: ObjectMapper,
    private val throttlingRequestTemplate: KafkaTemplate<String, ThrottlingRequest>
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

        val header = getHeaders(httpServletRequest)
        log.info { header }

        val request = ThrottlingRequest.newBuilder()
            .setRequestMethod(httpServletRequest.method)
            .setApiName(requestUri)
            .setHeader(header)
            .setBody(body)
            .setRequested(1L)
            .setTimestamp(System.currentTimeMillis())
            .build()

        throttlingRequestTemplate.executeInTransaction {
            it.send(KafkaTopicNames.THROTTLING_REQUEST, requestUri, request)
        }
    }

    private fun getHeaders(httpServletRequest: HttpServletRequest): Map<String, String> {
        val headerMap = mutableMapOf<String, String>()
        httpServletRequest.headerNames.toList().forEach {headerName ->
            headerMap[headerName] = httpServletRequest.getHeader(headerName)
        }

        return headerMap
    }

    // inline 함수는 컴파일 시에 실제 코드가 들어가게 된다.
    // 거기에 reified 키워드를 사용하면 구체화된 DTO 클래스를 사용할 수 있는 것이다.
    // 원래라면 body 에는 제네릭을 사용할 수 없다.
    private inline fun <reified DTO> routePost(requestUri: String, headers: Consumer<HttpHeaders>, body: String): DTO {
        val result = restClient.post()
            .uri(requestUri)
            .headers(headers)
            .body(body)
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(DTO::class.java)
            ?: throw RuntimeException("$requestUri 의 응답이 없습니다.")

        return result
    }

    @KafkaListener(
        topics = [KafkaTopicNames.THROTTLING_RESPONSE],
        concurrency = "3",
        containerFactory = "throttlingResponseListenerContainer",
        groupId = "throttling.response.group"
    )
    fun fixedWindowThrottlingProcess(record: ConsumerRecord<String, ThrottlingRequest>) {
        val requests = record.value()

        if(requests.requested > 150L) {
            log.info { "일시적으로 사용량이 너무 많습니다. 다시 시도해주세요." }
            return
        } else {
            log.info { "API 호출 횟수 : ${requests.requested}" }

            if(GatewayRouter.orderRouter(requests.apiName)) {
                val header = map2HttpHeaderConsumer(requests.header)
                val result = routePost<CreateOrderResponse>(requests.apiName, header, requests.body)
                log.info { "결과 = $result" }
            } else if(GatewayRouter.payRouter(requests.apiName)) {
                val header = map2HttpHeaderConsumer(requests.header)
                val result = routePost<SavePayResponse>(requests.apiName, header, requests.body)
                log.info { "결과 = $result" }
            }
        }
    }

    private fun map2HttpHeaderConsumer(header: Map<String, String>): Consumer<HttpHeaders> {
        return Consumer { httpHeaders ->
            header.forEach { (key, value) -> httpHeaders.set(key, value) }
        }
    }
}
