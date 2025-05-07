package com.example.ordermicroservice.config

import com.example.ordermicroservice.config.jaas.JaasProperties
import com.example.ordermicroservice.constants.KafkaBootstrapUrls
import org.apache.kafka.clients.admin.AdminClientConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaAdmin

@Configuration
class KafkaAdminConfig {
    @Bean
    @Primary
    fun admin() = KafkaAdmin(
        mapOf(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to KafkaBootstrapUrls.KAFKA_K8S_BOOTSTRAP_SERVERS,
            AdminClientConfig.CLIENT_ID_CONFIG to "KAFKA_ADMIN",
            AdminClientConfig.SECURITY_PROTOCOL_CONFIG to "SASL_PLAINTEXT",
            "sasl.jaas.config" to JaasProperties.JAAS_CLIENT,
            "sasl.mechanism" to "PLAIN"
        )
    )
}