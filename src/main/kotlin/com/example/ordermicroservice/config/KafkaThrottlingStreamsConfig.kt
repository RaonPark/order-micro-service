package com.example.ordermicroservice.config

import com.avro.support.ThrottlingRequest
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.gateway.FixedWindowThrottlingProcessor
import com.example.ordermicroservice.outbox.AvroService
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.Serdes.LongSerde
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

@Configuration
class KafkaThrottlingStreamsConfig {
    companion object {
        const val THROTTLING_STREAMS_BUILDER = "throttlingStreamsBuilder"
    }
    @Bean(name = [THROTTLING_STREAMS_BUILDER])
    fun throttlingStreamsBuilder(): StreamsBuilderFactoryBean {
        val config = mapOf(
            StreamsConfig.CLIENT_ID_CONFIG to "Throttling.Streams.id",
            StreamsConfig.APPLICATION_ID_CONFIG to "THROTTLING.STREAMS.APP",
            StreamsConfig.PROCESSING_GUARANTEE_CONFIG to StreamsConfig.EXACTLY_ONCE_V2,
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka1:9092,kafka2:9092,kafka3:9092",
            StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to StringSerde::class.java,
            StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to SpecificAvroSerde::class.java,
            StreamsConfig.producerPrefix(ProducerConfig.ACKS_CONFIG) to "all",
            StreamsConfig.producerPrefix(ProducerConfig.LINGER_MS_CONFIG) to "20",
            StreamsConfig.producerPrefix(ProducerConfig.BATCH_SIZE_CONFIG) to 32 * 1024,
            StreamsConfig.producerPrefix(ProducerConfig.COMPRESSION_TYPE_CONFIG) to "snappy",
            StreamsConfig.producerPrefix(ProducerConfig.TRANSACTIONAL_ID_CONFIG) to "THROTTLING.TX",
            StreamsConfig.producerPrefix(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG) to true,
            StreamsConfig.producerPrefix(ProducerConfig.RETRIES_CONFIG) to Integer.MAX_VALUE.toString(),
            StreamsConfig.consumerPrefix(ConsumerConfig.GROUP_ID_CONFIG) to "throttling.streams.group",
            StreamsConfig.consumerPrefix(ConsumerConfig.ISOLATION_LEVEL_CONFIG) to "read_committed",
            AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://schema-registry:8081",
        )

        return StreamsBuilderFactoryBean(KafkaStreamsConfiguration(config))
    }

    @Bean
    fun fixedWindowThrottlingStreams(
        @Qualifier(THROTTLING_STREAMS_BUILDER) throttlingStreamsBuilder: StreamsBuilder
    ): KStream<String, ThrottlingRequest> {
        val throttlingRequestAvroSerde = AvroService.getAvroSerde<ThrottlingRequest>()
        val streams = throttlingStreamsBuilder.stream(KafkaTopicNames.THROTTLING_REQUEST,
            Consumed.with(StringSerde(), throttlingRequestAvroSerde))

        streams.process(FixedWindowThrottlingProcessor())
            .filter { _, value -> value != null }
            .to(KafkaTopicNames.THROTTLING_RESPONSE, Produced.with(StringSerde(), throttlingRequestAvroSerde))

        return streams
    }

    @Bean
    fun fixedWindowThrottlingProducerFactory(): ProducerFactory<String, ThrottlingRequest> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka1:9092,kafka2:9092,kafka3:9092",
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.LINGER_MS_CONFIG to 20,
            ProducerConfig.BATCH_SIZE_CONFIG to 32 * 1024,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to SpecificAvroSerializer::class.java,
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://schema-registry:8081",
            ProducerConfig.COMPRESSION_TYPE_CONFIG to "snappy",
            ProducerConfig.TRANSACTIONAL_ID_CONFIG to "throttling-tx",
        )

        val producerFactory = DefaultKafkaProducerFactory<String, ThrottlingRequest>(config)
        producerFactory.setTransactionIdSuffixStrategy(DefaultTransactionIdSuffixStrategy(5))

        return producerFactory
    }

    @Bean
    fun throttlingRequestTemplate(): KafkaTemplate<String, ThrottlingRequest> {
        return KafkaTemplate(fixedWindowThrottlingProducerFactory())
    }

    @Bean
    fun throttlingResponseConsumerFactory(): ConsumerFactory<String, ThrottlingRequest> {
        val config = mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to "throttling.response.group",
            ConsumerConfig.ISOLATION_LEVEL_CONFIG to "read_committed",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to KafkaAvroDeserializer::class.java,
            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://schema-registry:8081",
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to "true",
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_VALUE_TYPE_CONFIG to ThrottlingRequest::class.java,
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka1:9092,kafka2:9092,kafka3:9092",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
            ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG to "true",
            ConsumerConfig.ISOLATION_LEVEL_CONFIG to "read_committed",
        )

        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun throttlingResponseListenerContainer(): ConcurrentKafkaListenerContainerFactory<String, ThrottlingRequest> {
        val listenerContainer = ConcurrentKafkaListenerContainerFactory<String, ThrottlingRequest>()
        listenerContainer.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        listenerContainer.consumerFactory = throttlingResponseConsumerFactory()
        listenerContainer.setConcurrency(3)
        listenerContainer.containerProperties.eosMode = ContainerProperties.EOSMode.V2

        return listenerContainer
    }
}