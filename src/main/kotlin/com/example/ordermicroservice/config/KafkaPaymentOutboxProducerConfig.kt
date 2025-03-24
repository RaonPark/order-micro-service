package com.example.ordermicroservice.config

import com.avro.PaymentOutboxMessage
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
class KafkaPaymentOutboxProducerConfig {
    @Bean
    fun paymentProducerFactory(): ProducerFactory<String, PaymentOutboxMessage> {
        val config = mutableMapOf<String, Any>()
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "kafka1:9092,kafka2:9092,kafka3:9092"
        config[ProducerConfig.ACKS_CONFIG] = "all"
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
        config[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "http://schema-registry:8081"
        config[ProducerConfig.BATCH_SIZE_CONFIG] = 16 * 1024
        config[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "snappy"
        config[ProducerConfig.LINGER_MS_CONFIG] = 10

        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun paymentKafkaTemplate(): KafkaTemplate<String, PaymentOutboxMessage> {
        return KafkaTemplate(paymentProducerFactory())
    }
}