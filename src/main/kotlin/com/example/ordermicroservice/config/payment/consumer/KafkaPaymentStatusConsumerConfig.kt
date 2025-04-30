package com.example.ordermicroservice.config.payment.consumer

import com.avro.payment.PaymentStatusMessage
import com.example.ordermicroservice.config.jaas.JaasProperties
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
class KafkaPaymentStatusConsumerConfig {
    @Bean
    fun paymentStatusConsumerFactory(): ConsumerFactory<String, PaymentStatusMessage> {
        val config = mutableMapOf<String, Any>()
        config[ConsumerConfig.GROUP_ID_CONFIG] = "PAYMENT_STATUS"
        config[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        config[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        config[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = KafkaAvroDeserializer::class.java
        config[KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "http://schema-registry:8081"
        config[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_VALUE_TYPE_CONFIG] = PaymentStatusMessage::class.java
        config[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = true
        config[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        config[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "kafka1:9092,kafka2:9092,kafka3:9092"
        config[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"
        config[ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG] = "true"
        config[ConsumerConfig.ISOLATION_LEVEL_CONFIG] = "read_committed"
        config["sasl.jaas.config"] = JaasProperties.JAAS_CLIENT
        config["security.protocol"] = "SASL_PLAINTEXT"
        config["sasl.mechanism"] = "PLAIN"

        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun paymentStatusListenerContainer(): ConcurrentKafkaListenerContainerFactory<String, PaymentStatusMessage> {
        val listener = ConcurrentKafkaListenerContainerFactory<String, PaymentStatusMessage>()
        listener.setConcurrency(3)
        listener.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        listener.consumerFactory = paymentStatusConsumerFactory()
        listener.containerProperties.eosMode = ContainerProperties.EOSMode.V2

        return listener
    }
}