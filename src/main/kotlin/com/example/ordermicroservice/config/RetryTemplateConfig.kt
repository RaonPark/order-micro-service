package com.example.ordermicroservice.config

import io.netty.handler.timeout.ReadTimeoutException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.NoProducerAvailableException
import org.springframework.retry.annotation.EnableRetry
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy
import org.springframework.retry.support.RetryTemplate

@Configuration
@EnableRetry
class RetryTemplateConfig {
    @Bean
    fun noProducerAvailableExceptionRetryTemplate(): RetryTemplate {
        return RetryTemplate.builder()
            .retryOn(NoProducerAvailableException::class.java)
            .exponentialBackoff(100, 2.0, 3000)
            .maxAttempts(100)
            .build()
    }

    @Bean
    fun readTimeoutExceptionRetryTemplate(): RetryTemplate {
        return RetryTemplate.builder()
            .retryOn(ReadTimeoutException::class.java)
            .exponentialBackoff(1000, 2.0, 5000)
            .maxAttempts(10)
            .build()
    }
}