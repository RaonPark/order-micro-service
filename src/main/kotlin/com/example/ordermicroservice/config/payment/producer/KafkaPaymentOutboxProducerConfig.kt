package com.example.ordermicroservice.config.payment.producer

import com.avro.payment.PaymentOutboxMessage
import com.example.ordermicroservice.config.jaas.JaasProperties
import com.example.ordermicroservice.constants.KafkaBootstrapUrls
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
class KafkaPaymentOutboxProducerConfig {
    @Bean
    fun paymentOutboxProducerFactory(): ProducerFactory<String, PaymentOutboxMessage> {
        val config = mutableMapOf<String, Any>()
        config[ProducerConfig.CLIENT_ID_CONFIG] = "PAYMENT_OUTBOX_PRODUCER"
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = KafkaBootstrapUrls.KAFKA_K8S_BOOTSTRAP_SERVERS
        config[ProducerConfig.ACKS_CONFIG] = "all"
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
        config[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "http://schema-registry:8081"
        config[ProducerConfig.BATCH_SIZE_CONFIG] = 32 * 1024
        config[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "snappy"
        config[ProducerConfig.LINGER_MS_CONFIG] = 20
        config[ProducerConfig.TRANSACTIONAL_ID_CONFIG] = "payment.outbox.tx"
        config[ProducerConfig.TRANSACTION_TIMEOUT_CONFIG] = "5000"
        config[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
        config[ProducerConfig.RETRIES_CONFIG] = Integer.MAX_VALUE.toString()
        config["sasl.jaas.config"] = JaasProperties.JAAS_CLIENT
        config["security.protocol"] = "SASL_PLAINTEXT"
        config["sasl.mechanism"] = "PLAIN"

        val producerFactory = DefaultKafkaProducerFactory<String, PaymentOutboxMessage>(config)
        // Greater Than Concurrency if ConcurrentKafkaContainerListener used.
        producerFactory.setTransactionIdSuffixStrategy(DefaultTransactionIdSuffixStrategy(5))
        return producerFactory
    }

    @Bean
    fun paymentOutboxTemplate(): KafkaTemplate<String, PaymentOutboxMessage> {
        return KafkaTemplate(paymentOutboxProducerFactory())
    }
}