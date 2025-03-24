package com.example.ordermicroservice.config

import com.avro.OrderOutboxMessage
import com.avro.PaymentOutboxMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.internals.Topic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafkaRetryTopic
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.kafka.retrytopic.RetryTopicConfiguration
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder

@EnableKafkaRetryTopic
@Configuration
class KafkaTopicConfig {
//    @Bean
//    fun retryOrderOutboxTopic(kafkaTemplate: KafkaTemplate<String, OrderOutboxMessage>): RetryTopicConfiguration =
//        RetryTopicConfigurationBuilder.newInstance()
//            .dltSuffix("-dlt")
//            .autoCreateTopicsWith(10, 3)
//            .concurrency(3)
//            // Exponential backoff with jitter
//            .exponentialBackoff(1000L, 2.0, 20000, true)
//            .dltProcessingFailureStrategy(DltStrategy.ALWAYS_RETRY_ON_ERROR)
//            .create(kafkaTemplate)

    @Bean
    fun retryPaymentOutboxTopic(kafkaTemplate: KafkaTemplate<String, PaymentOutboxMessage>): RetryTopicConfiguration =
        RetryTopicConfigurationBuilder.newInstance()
            .dltSuffix("-dlt")
            .autoCreateTopicsWith(10, 3)
            .concurrency(3)
            .exponentialBackoff(1000L, 2.0, 20000, true)
            .dltProcessingFailureStrategy(DltStrategy.ALWAYS_RETRY_ON_ERROR)
            .create(kafkaTemplate)

    @Bean
    fun paymentStatusTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.PAYMENT_STATUS)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun shippingTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.SHIPPING)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun orderStatusTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.ORDER_STATUS)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun orderOutboxTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.ORDER_OUTBOX)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun paymentOutboxTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.PAYMENT_OUTBOX)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .build()
}