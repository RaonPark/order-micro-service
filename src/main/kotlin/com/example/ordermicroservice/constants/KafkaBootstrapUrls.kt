package com.example.ordermicroservice.constants

object KafkaBootstrapUrls {
    const val KAFKA_K8S_BOOTSTRAP_SERVERS = "kafka-0.kafka-headless.kafka.svc.cluster.local:9092,kafka-1.kafka-headless.kafka.svc.cluster.local:9092,kafka-2.kafka-headless.kafka.svc.cluster.local:9092"
    const val KAFKA_DOCKER_BOOTSTRAP_SERVERS = "kafka1:9092,kafka2:9092,kafka3:09092"
}