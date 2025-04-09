package com.example.ordermicroservice.config.order.consumer

import com.example.ordermicroservice.vo.CreateOrderVo
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
class KafkaCreateOrderConsumerConfig {
    @Bean
    fun createOrderConsumerFactory(): ConsumerFactory<String, CreateOrderVo> {
        val config = mutableMapOf<String, Any>()
        config[ConsumerConfig.GROUP_ID_CONFIG] = "CREATE_ORDER"
        config[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        config[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        config[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = JsonDeserializer::class.java
        config[JsonDeserializer.TRUSTED_PACKAGES] = "*"
        config[JsonDeserializer.VALUE_DEFAULT_TYPE] = CreateOrderVo::class.java
        config["spring.kafka.consumer.properties.spring.json.encoding"] = "UTF-8"
        config[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        config[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "kafka1:9092,kafka2:9092,kafka3:9092"
        config[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"
        config[ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG] = "true"
        config[ConsumerConfig.ISOLATION_LEVEL_CONFIG] = "read_committed"

        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun createOrderListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, CreateOrderVo> {
        val listener = ConcurrentKafkaListenerContainerFactory<String, CreateOrderVo>()
        listener.consumerFactory = createOrderConsumerFactory()
        // enable.auto.commit 을 false 로 설정했고, ack 을 매뉴얼하게 보내기 위해 설정한다.
        listener.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        listener.setConcurrency(3)
        listener.containerProperties.eosMode = ContainerProperties.EOSMode.V2

        return listener
    }
}