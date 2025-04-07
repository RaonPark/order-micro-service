package com.example.ordermicroservice.config

import com.example.ordermicroservice.vo.OrderCompensation
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
@EnableKafka
class KafkaOrderCompensationConfig {
    @Bean
    fun orderCompensationConsumerFactory(): ConsumerFactory<String, OrderCompensation> {
        val config = mutableMapOf<String, Any>()
        config[ConsumerConfig.GROUP_ID_CONFIG] = "ORDER_COMPENSATION"
        config[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        config[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        config[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = JsonDeserializer::class.java
        config[JsonDeserializer.TRUSTED_PACKAGES] = "*"
        config[JsonDeserializer.USE_TYPE_INFO_HEADERS] = "false"
        config[JsonDeserializer.VALUE_DEFAULT_TYPE] = OrderCompensation::class.java
        config[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        config[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "kafka1:9092,kafka2:9092,kafka3:9092"
        config[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"
        config[ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG] = "true"
        config[ConsumerConfig.ISOLATION_LEVEL_CONFIG] = "read_committed"

        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun orderCompensationListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, OrderCompensation> {
        val containerFactory = ConcurrentKafkaListenerContainerFactory<String, OrderCompensation>()
        containerFactory.containerProperties.eosMode = ContainerProperties.EOSMode.V2
        containerFactory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        containerFactory.consumerFactory = orderCompensationConsumerFactory()
        containerFactory.setConcurrency(3)

        return containerFactory
    }

    @Bean
    fun orderCompensationProducerFactory(): ProducerFactory<String, OrderCompensation> {
        val config = mutableMapOf<String, Any>()
        config[ProducerConfig.CLIENT_ID_CONFIG] = "ORDER_COMPENSATION_PRODUCER"
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "kafka1:9092,kafka2:9092,kafka3:9092"
        config[ProducerConfig.ACKS_CONFIG] = "all"
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        config[ProducerConfig.BATCH_SIZE_CONFIG] = 64 * 1024
        config[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "snappy"
        config[ProducerConfig.LINGER_MS_CONFIG] = 10
        config[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
        config[ProducerConfig.TRANSACTIONAL_ID_CONFIG] = "order.compensation.tx"
        config[ProducerConfig.TRANSACTION_TIMEOUT_CONFIG] = "3000"

        val producerFactory = DefaultKafkaProducerFactory<String, OrderCompensation>(config)
        producerFactory.setTransactionIdSuffixStrategy(DefaultTransactionIdSuffixStrategy(5))

        return producerFactory
    }

    @Bean
    fun orderCompensationTemplate(): KafkaTemplate<String, OrderCompensation> {
        return KafkaTemplate(orderCompensationProducerFactory())
    }
}