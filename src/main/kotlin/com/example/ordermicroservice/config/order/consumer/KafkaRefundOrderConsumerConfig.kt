package com.example.ordermicroservice.config.order.consumer

import com.avro.order.OrderRefundMessage
import com.example.ordermicroservice.config.jaas.JaasProperties
import com.example.ordermicroservice.constants.KafkaBootstrapUrls
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
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

@EnableKafka
@Configuration
class KafkaRefundOrderConsumerConfig {

    @Bean
    fun refundOrderConsumerFactory(): ConsumerFactory<String, OrderRefundMessage> {
        val config = mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to "ORDER_REFUND",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to KafkaAvroDeserializer::class.java,
            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://schema-registry:8081",
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_VALUE_TYPE_CONFIG to OrderRefundMessage::class.java,
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to KafkaBootstrapUrls.KAFKA_K8S_BOOTSTRAP_SERVERS,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
            ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG to "true",
            ConsumerConfig.ISOLATION_LEVEL_CONFIG to "read_committed",
            "sasl.jaas.config" to JaasProperties.JAAS_CLIENT,
            "security.protocol" to "SASL_PLAINTEXT",
            "sasl.mechanism" to "PLAIN"
        )

        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun refundOrderListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, OrderRefundMessage> {
        val containerFactory = ConcurrentKafkaListenerContainerFactory<String, OrderRefundMessage>()

        containerFactory.containerProperties.eosMode = ContainerProperties.EOSMode.V2
        containerFactory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        containerFactory.consumerFactory = refundOrderConsumerFactory()
        containerFactory.setConcurrency(3)

        return containerFactory
    }
}