package com.example.ordermicroservice.config

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MongoConfig {
    companion object {
        @ConfigurationProperties(prefix = "spring.data.mongodb")
        data class MongoProperties(
            val uri: String,
            val host: String,
            val password: String,
            val database: String
        )
    }

    @Bean
    fun mongoClient(mongoProperties: MongoProperties): MongoClient {
        return MongoClients.create(mongoProperties.uri)
    }
}