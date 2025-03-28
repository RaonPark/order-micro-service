package com.example.ordermicroservice.config

import com.avro.account.AccountRequestMessage
import com.avro.account.AccountRequestType
import com.avro.account.AccountVoMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.outbox.AvroService
import com.example.ordermicroservice.vo.AccountVo
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serdes.LongSerde
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.state.WindowStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonSerde
import java.time.Duration

@EnableKafkaStreams
@Configuration
class KafkaAccountRequestStreamsConfig {
    @Bean(name = [KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME])
    fun accountRequestStreamsConfig(): KafkaStreamsConfiguration {
        val config = mapOf(
            StreamsConfig.CLIENT_ID_CONFIG to "ACCOUNT_REQUEST",
            StreamsConfig.APPLICATION_ID_CONFIG to "ACCOUNT_REQUEST_APP",
            StreamsConfig.PROCESSING_GUARANTEE_CONFIG to StreamsConfig.EXACTLY_ONCE_V2,
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka1:9092,kafka2:9092,kafka3:9092",
            StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to StringSerde::class.java,
            StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to SpecificAvroSerde::class.java,
            StreamsConfig.producerPrefix(ProducerConfig.ACKS_CONFIG) to "all",
            StreamsConfig.producerPrefix(ProducerConfig.LINGER_MS_CONFIG) to "20",
            StreamsConfig.producerPrefix(ProducerConfig.BATCH_SIZE_CONFIG) to 32 * 1024,
            StreamsConfig.producerPrefix(ProducerConfig.COMPRESSION_TYPE_CONFIG) to "snappy",
            StreamsConfig.producerPrefix(ProducerConfig.TRANSACTIONAL_ID_CONFIG) to "ACCOUNT_REQUEST_TX",
            StreamsConfig.producerPrefix(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG) to true,
            StreamsConfig.producerPrefix(ProducerConfig.RETRIES_CONFIG) to "5",
            StreamsConfig.consumerPrefix(ConsumerConfig.ISOLATION_LEVEL_CONFIG) to "read_committed",
            AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://schema-registry:8081",
        )

        return KafkaStreamsConfiguration(config)
    }

    @Bean
    fun accountRequestQueueStreams(streamsBuilder: StreamsBuilder): KStream<String, AccountRequestMessage> {
        val accountRequestMessageAvroSerde = AvroService.getAvroSerde<AccountRequestMessage>(AccountRequestMessage::class.java.name)
        val accountVoAvroSerde = AvroService.getAvroSerde<AccountVoMessage>(AccountVoMessage::class.java.name)
        val stream = streamsBuilder.stream(KafkaTopicNames.ACCOUNT_REQUEST,
            Consumed.with(StringSerde(), accountRequestMessageAvroSerde))

        stream.groupBy({ _, accountRequestMessage ->
            accountRequestMessage.accountNumber
        }, Grouped.with(Serdes.String(), accountRequestMessageAvroSerde))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(3L)).advanceBy(Duration.ofSeconds(3L)))
            .aggregate(
                { 0L },
                {
                    _, message, balance ->
                    when(message.requestType) {
                        AccountRequestType.DEPOSIT -> message.amount
                        AccountRequestType.WITHDRAW -> -message.amount
                        else -> balance
                    }
                },
                Materialized.`as`<String?, Long?, WindowStore<Bytes, ByteArray>?>("ACCOUNT_BALANCE")
                    .withKeySerde(StringSerde())
                    .withValueSerde(LongSerde())
            )
            .toStream()
            .map { wk, balance -> KeyValue.pair(wk.key(), AccountVoMessage(wk.key(), balance)) }
            .to(KafkaTopicNames.ACCOUNT_REQUEST_RESPONSE, Produced.with(StringSerde(), accountVoAvroSerde))

        return stream
    }

    @Bean
    fun accountRequestProducerFactory(): ProducerFactory<String, AccountRequestMessage> {
        val config = mapOf(
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.LINGER_MS_CONFIG to "10",
            ProducerConfig.BATCH_SIZE_CONFIG to 32 * 1024,
            ProducerConfig.COMPRESSION_TYPE_CONFIG to "snappy",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://schema-registry:8081",
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka1:9092,kafka2:9092,kafka3:9092",
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to "true"
        )

        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun accountRequestTemplate(): KafkaTemplate<String, AccountRequestMessage> {
        return KafkaTemplate(accountRequestProducerFactory())
    }

    @Bean
    fun accountResponseConsumerFactory(): ConsumerFactory<String, AccountVoMessage> {
        val config = mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to "ACCOUNT_RESPONSE",
            ConsumerConfig.ISOLATION_LEVEL_CONFIG to "read_committed",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
            ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG to "true",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka1:9092,kafka2:9092,kafka3:9092",
            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://schema-registry:8081",
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_VALUE_TYPE_CONFIG to AccountVoMessage::class.java
        )

        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun accountResponseListenerContainer(): ConcurrentKafkaListenerContainerFactory<String, AccountVoMessage> {
        val listenerContainer = ConcurrentKafkaListenerContainerFactory<String, AccountVoMessage>()
        listenerContainer.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        listenerContainer.consumerFactory = accountResponseConsumerFactory()
        listenerContainer.setConcurrency(3)

        return listenerContainer
    }
}