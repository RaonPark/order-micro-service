package com.example.ordermicroservice.config.shipping

import com.avro.shipping.ShippingMessage
import com.example.ordermicroservice.config.jaas.JaasProperties
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.DefaultTransactionIdSuffixStrategy
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@EnableKafka
@Configuration
class KafkaShippingProducerConfig {
    @Bean
    fun shippingProducerFactory(): ProducerFactory<String, ShippingMessage> {
        val config = mutableMapOf<String, Any>()
        config[ProducerConfig.CLIENT_ID_CONFIG] = "Shipping_Producer"
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = KafkaBootstrapUrls.KAFKA_K8S_BOOTSTRAP_SERVERS
        config[ProducerConfig.ACKS_CONFIG] = "all"
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
        config[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "http://schema-registry:8081"
        config[ProducerConfig.BATCH_SIZE_CONFIG] = 32 * 1024 // 32KB
        config[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "snappy"
        config[ProducerConfig.LINGER_MS_CONFIG] = 20
        config[ProducerConfig.TRANSACTIONAL_ID_CONFIG] = "shipping-tx"
        config[ProducerConfig.RETRIES_CONFIG] = Integer.MAX_VALUE.toString()
        config[ProducerConfig.TRANSACTION_TIMEOUT_CONFIG] = "5000"
        config["sasl.jaas.config"] = JaasProperties.JAAS_CLIENT
        config["security.protocol"] = "SASL_PLAINTEXT"
        config["sasl.mechanism"] = "PLAIN"

        val producerFactory = DefaultKafkaProducerFactory<String, ShippingMessage>(config)
        producerFactory.setTransactionIdSuffixStrategy(DefaultTransactionIdSuffixStrategy(5))
        return producerFactory
    }

    @Bean
    fun shippingTemplate(): KafkaTemplate<String, ShippingMessage> {
        val kafkaTemplate = KafkaTemplate(shippingProducerFactory())
        return kafkaTemplate
    }
}