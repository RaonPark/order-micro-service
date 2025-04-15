package com.example.ordermicroservice.config.order.producer

import com.avro.order.OrderRefundMessage
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
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
class KafkaRefundOrderProducerConfig {
    @Bean
    fun refundOrderProducerFactory(): ProducerFactory<String, OrderRefundMessage> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka1:9092,kafka2:9092,kafka3:9092",
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.LINGER_MS_CONFIG to 20,
            ProducerConfig.BATCH_SIZE_CONFIG to 32 * 1024,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to SpecificAvroSerializer::class.java,
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://schema-registry:8081",
            ProducerConfig.COMPRESSION_TYPE_CONFIG to "snappy",
            ProducerConfig.TRANSACTIONAL_ID_CONFIG to "order-refund-tx",
            ProducerConfig.TRANSACTION_TIMEOUT_CONFIG to "5000",
        )

        val producerFactory = DefaultKafkaProducerFactory<String, OrderRefundMessage>(config)
        producerFactory.setTransactionIdSuffixStrategy(DefaultTransactionIdSuffixStrategy(5))

        return producerFactory
    }

    @Bean
    fun refundOrderKafkaTemplate(): KafkaTemplate<String, OrderRefundMessage> =
        KafkaTemplate(refundOrderProducerFactory())
}