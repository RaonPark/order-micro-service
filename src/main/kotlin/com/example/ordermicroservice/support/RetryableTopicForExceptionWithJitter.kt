package com.example.ordermicroservice.support

import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy
import org.springframework.retry.annotation.Backoff

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@RetryableTopic(topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
    autoCreateTopics = "true",
    backoff = Backoff(
        delay = 1000,
        random = true,
        maxDelay = 10000,
        multiplier = 2.0
    ),
    attempts = "5",
    numPartitions = "10",
    kafkaTemplate = "shippingTemplate",
    replicationFactor = "3",
    concurrency = "3",
    dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR
)
annotation class RetryableTopicForShippingTopic(
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@RetryableTopic(topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
    autoCreateTopics = "true",
    backoff = Backoff(
        delay = 1000,
        random = true,
        maxDelay = 10000,
        multiplier = 2.0
    ),
    attempts = "5",
    numPartitions = "10",
    kafkaTemplate = "paymentOutboxTemplate",
    replicationFactor = "3",
    concurrency = "3",
    dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR
)
annotation class RetryableTopicForPaymentTopic(
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@RetryableTopic(topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
    autoCreateTopics = "true",
    backoff = Backoff(
        delay = 1000,
        random = true,
        maxDelay = 10000,
        multiplier = 2.0
    ),
    attempts = "5",
    numPartitions = "10",
    kafkaTemplate = "orderOutboxTemplate",
    replicationFactor = "3",
    concurrency = "3",
    dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR
)
annotation class RetryableTopicForOrderTopic(
)

