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

    @Bean
    fun accountRequestTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.ACCOUNT_REQUEST)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun accountResponseTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.ACCOUNT_REQUEST_RESPONSE)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun throttlingRequestTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.THROTTLING_REQUEST)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun throttlingResponseTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.THROTTLING_RESPONSE)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun compensationTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.COMPENSATION_REQUEST)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun createOrderTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.ORDER_REQUEST)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun processPaymentTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.PAYMENT_REQUEST)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun refundOrderTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.ORDER_REFUND)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun cancelShippingTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.SHIPPING_CANCEL)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()

    @Bean
    fun cancelPaymentTopic(): NewTopic =
        TopicBuilder.name(KafkaTopicNames.PAYMENT_CANCEL)
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "3")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build()
}