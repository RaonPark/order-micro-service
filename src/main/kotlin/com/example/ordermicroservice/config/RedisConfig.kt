package com.example.ordermicroservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
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
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer(Charsets.UTF_8)
        template.hashValueSerializer = GenericJackson2JsonRedisSerializer()

        return template
    }
}