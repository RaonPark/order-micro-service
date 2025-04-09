package com.example.ordermicroservice.config.order.producer

import com.avro.order.OrderOutboxMessage
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.DefaultTransactionIdSuffixStrategy
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
class KafkaOrderOutboxProducerConfig {
    @Bean
    fun orderOutboxProducerFactory(): ProducerFactory<String, OrderOutboxMessage> {
        val config = mutableMapOf<String, Any>()
        config[ProducerConfig.CLIENT_ID_CONFIG] = "ORDER_OUTBOX_PRODUCER"
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "kafka1:9092,kafka2:9092,kafka3:9092"
        config[ProducerConfig.ACKS_CONFIG] = "all"
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
        config[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "http://schema-registry:8081"
        // https://learn.conduktor.io/kafka/kafka-producer-batching/
        // conduktor 에 의하면 배치를 32KB, 64KB 로 올리는 것은 괜찮다고 함
        // 하지만 너무 많은 숫자를 두게 되면 별로 좋지 않다고 함
        config[ProducerConfig.BATCH_SIZE_CONFIG] = 32 * 1024 // 32KB
        // https://learn.conduktor.io/kafka/kafka-message-compression/
        // conduktor 에서는 snappy 혹은 lz4 를 최적의 속도/압축을 위해 테스트해보라고 함
        config[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "snappy"
        // 20ms 의 레코드가 찰 시간을 준다.
        config[ProducerConfig.LINGER_MS_CONFIG] = 20
        // Guarantees Idempotent
        config[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
        config[ProducerConfig.TRANSACTIONAL_ID_CONFIG] = "order.outbox.tx"
        config[ProducerConfig.TRANSACTION_TIMEOUT_CONFIG] = "3000"

        val producerFactory = DefaultKafkaProducerFactory<String, OrderOutboxMessage>(config)
        producerFactory.setTransactionIdSuffixStrategy(DefaultTransactionIdSuffixStrategy(5))

        return producerFactory
    }

    @Bean
    fun orderOutboxTemplate(): KafkaTemplate<String, OrderOutboxMessage> {
        val kafkaTemplate = KafkaTemplate(orderOutboxProducerFactory())
        return kafkaTemplate
    }
}