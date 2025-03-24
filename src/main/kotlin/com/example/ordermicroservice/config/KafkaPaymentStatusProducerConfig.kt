package com.example.ordermicroservice.config

import com.avro.payment.PaymentStatusMessage
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
class KafkaPaymentStatusProducerConfig {
    @Bean
    fun paymentStatusProducerFactory(): ProducerFactory<String, PaymentStatusMessage> {
        val config = mutableMapOf<String, Any>()
        config[ProducerConfig.CLIENT_ID_CONFIG] = "PAYMENT_STATUS"
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "kafka1:9092,kafka2:9092,kafka3:9092"
        config[ProducerConfig.ACKS_CONFIG] = "all"
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
        config[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "http://schema-registry:8081"
        config[ProducerConfig.BATCH_SIZE_CONFIG] = 32 * 1024
        config[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "snappy"
        config[ProducerConfig.LINGER_MS_CONFIG] = 20

        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun paymentStatusTemplate(): KafkaTemplate<String, PaymentStatusMessage> {
        return KafkaTemplate(paymentStatusProducerFactory())
    }
}