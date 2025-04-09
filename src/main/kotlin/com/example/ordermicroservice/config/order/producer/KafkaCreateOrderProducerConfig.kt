package com.example.ordermicroservice.config.order.producer

import com.example.ordermicroservice.vo.CreateOrderVo
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
class KafkaCreateOrderProducerConfig {
    @Bean
    fun createOrderProducerFactory(): ProducerFactory<String, CreateOrderVo> {
        val config = mutableMapOf<String, Any>()
        config[ProducerConfig.CLIENT_ID_CONFIG] = "CREATE_ORDER_PRODUCER"
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "kafka1:9092,kafka2:9092,kafka3:9092"
        config[ProducerConfig.ACKS_CONFIG] = "all"
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        config[JsonSerializer.ADD_TYPE_INFO_HEADERS] = "false"
        config[ProducerConfig.BATCH_SIZE_CONFIG] = 32 * 1024
        config[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "snappy"
        config[ProducerConfig.LINGER_MS_CONFIG] = 20
        config[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
        config[ProducerConfig.TRANSACTIONAL_ID_CONFIG] = "order.request.tx"
        config[ProducerConfig.TRANSACTION_TIMEOUT_CONFIG] = "3000"

        val producerFactory = DefaultKafkaProducerFactory<String, CreateOrderVo>(config)
        producerFactory.setTransactionIdSuffixStrategy(DefaultTransactionIdSuffixStrategy(5))

        return producerFactory
    }

    @Bean
    fun createOrderKafkaTemplate(): KafkaTemplate<String, CreateOrderVo> {
        val kafkaTemplate = KafkaTemplate(createOrderProducerFactory())
        return kafkaTemplate
    }
}