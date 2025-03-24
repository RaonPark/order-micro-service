package com.example.ordermicroservice.config

import com.example.ordermicroservice.constants.KafkaTopicNames
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.config.TopicConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafkaRetryTopic
import org.springframework.kafka.config.TopicBuilder

@EnableKafkaRetryTopic
@Configuration
class KafkaTopicConfig {
    @Bean
    fun orderOutboxTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.ORDER_OUTBOX)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun shippingTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.SHIPPING)
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
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun paymentStatusTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.PAYMENT_STATUS)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()
}