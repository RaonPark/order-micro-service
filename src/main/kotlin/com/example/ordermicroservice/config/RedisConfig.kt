package com.example.ordermicroservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
class RedisConfig {
    @Bean
    fun redisConnectionFactory(): LettuceConnectionFactory {
        return LettuceConnectionFactory(RedisStandaloneConfiguration("redis", 6379))
    }

    @Bean
    fun redisCacheManager(redisConnectionFactory: LettuceConnectionFactory): RedisCacheManager {
        val config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(config)
            .build()
    }

    @Bean(name = ["balanceAtomicScript"])
    fun balanceAtomicScript(): RedisScript<Long> {
        return RedisScript.of(ClassPathResource("scripts/BalanceAtomicScript.lua"), Long::class.java)
    }

    @Bean(name = ["accountOperationCounterScript"])
    fun accountOperationCounterScript(): RedisScript<Long> {
        return RedisScript.of(ClassPathResource("scripts/AccountOperationCounter.lua"), Long::class.java)
    }

    @Bean
    fun redisTemplate(redisConnectionFactory: LettuceConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()

        template.connectionFactory = redisConnectionFactory
        template.keySerializer = StringRedisSerializer(Charsets.UTF_8)
        template.valueSerializer = StringRedisSerializer(Charsets.UTF_8)
        template.hashKeySerializer = StringRedisSerializer(Charsets.UTF_8)
        template.hashValueSerializer = StringRedisSerializer(Charsets.UTF_8)

        return template
    }

    @Bean
    fun numericRedisTemplate(redisConnectionFactory: LettuceConnectionFactory): RedisTemplate<String, Long> {
        val template = RedisTemplate<String, Long>()

        template.connectionFactory = redisConnectionFactory
        template.keySerializer = StringRedisSerializer(Charsets.UTF_8)
        template.valueSerializer = Jackson2JsonRedisSerializer(Long::class.java)
        template.hashKeySerializer = StringRedisSerializer(Charsets.UTF_8)
        template.hashValueSerializer = Jackson2JsonRedisSerializer(Long::class.java)

        return template
    }
}