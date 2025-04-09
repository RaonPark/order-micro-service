package com.example.ordermicroservice.config.payment.consumer

import com.example.ordermicroservice.vo.PaymentIntentTokenVo
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer

@EnableKafka
@Configuration
class KafkaPaymentRequestConsumerConfig {
    @Bean
    fun paymentRequestConsumerFactory(): ConsumerFactory<String, PaymentIntentTokenVo> {
        val config = mutableMapOf<String, Any>()
        config[ConsumerConfig.GROUP_ID_CONFIG] = "PAYMENT_REQUEST"
        config[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        config[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        config[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = JsonDeserializer::class.java
        config[JsonDeserializer.TRUSTED_PACKAGES] = "*"
        config[JsonDeserializer.VALUE_DEFAULT_TYPE] = PaymentIntentTokenVo::class.java
        config["spring.kafka.consumer.properties.spring.json.encoding"] = "UTF-8"
        config[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        config[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "kafka1:9092,kafka2:9092,kafka3:9092"
        config[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"
        config[ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG] = "true"
        config[ConsumerConfig.ISOLATION_LEVEL_CONFIG] = "read_committed"

        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun processPaymentListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, PaymentIntentTokenVo> {
        val listener = ConcurrentKafkaListenerContainerFactory<String, PaymentIntentTokenVo>()
        listener.setConcurrency(3)
        listener.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        listener.consumerFactory = paymentRequestConsumerFactory()
        listener.containerProperties.eosMode = ContainerProperties.EOSMode.V2

        return listener
    }
}