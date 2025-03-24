package com.example.ordermicroservice.outbox

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.avro.specific.SpecificRecord

class AvroService {
    companion object {
        fun <T: SpecificRecord> getAvroSerde(specificAvroRecordName: String): SpecificAvroSerde<T> {
            val config = mapOf(
                KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://schema-registry:8081",
                KafkaAvroSerializerConfig.MAX_RETRIES_CONFIG to "10",
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_VALUE_TYPE_CONFIG to specificAvroRecordName
            )
            val avroSerde = SpecificAvroSerde<T>()
            avroSerde.configure(config, false)

            return avroSerde
        }
    }
}