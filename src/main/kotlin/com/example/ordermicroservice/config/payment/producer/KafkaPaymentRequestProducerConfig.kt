package com.example.ordermicroservice.config.payment.producer

import com.avro.payment.PaymentRequestMessage
import com.example.ordermicroservice.config.jaas.JaasProperties
import com.example.ordermicroservice.vo.PaymentIntentTokenVo
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
import org.springframework.kafka.support.serializer.JsonSerializer

@EnableKafka
@Configuration
class KafkaPaymentRequestProducerConfig {
    @Bean
    fun paymentRequestProducerFactory(): ProducerFactory<String, PaymentRequestMessage> {
        val config = mutableMapOf<String, Any>()
        config[ProducerConfig.CLIENT_ID_CONFIG] = "PAYMENT_REQUEST"
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "kafka1:9092,kafka2:9092,kafka3:9092"
        config[ProducerConfig.ACKS_CONFIG] = "all"
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
        config[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "http://schema-registry:8081"
        config[ProducerConfig.BATCH_SIZE_CONFIG] = 32 * 1024
        config[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "snappy"
        config[ProducerConfig.LINGER_MS_CONFIG] = 20
        config[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
        config[ProducerConfig.TRANSACTIONAL_ID_CONFIG] = "Payment.Request.tx"
        config[ProducerConfig.TRANSACTION_TIMEOUT_CONFIG] = "3000"
        config["sasl.jaas.config"] = JaasProperties.JAAS_CLIENT
        config["security.protocol"] = "SASL_PLAINTEXT"
        config["sasl.mechanism"] = "PLAIN"

        val producerFactory = DefaultKafkaProducerFactory<String, PaymentRequestMessage>(config)
        producerFactory.setTransactionIdSuffixStrategy(DefaultTransactionIdSuffixStrategy(5))
        return producerFactory
    }

    @Bean
    fun paymentRequestKafkaTemplate(): KafkaTemplate<String, PaymentRequestMessage> {
        return KafkaTemplate(paymentRequestProducerFactory())
    }
}