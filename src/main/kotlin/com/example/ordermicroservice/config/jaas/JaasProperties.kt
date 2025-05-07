package com.example.ordermicroservice.config.jaas

class JaasProperties {
    companion object {
        const val JAAS_CLIENT = "org.apache.kafka.common.security.plain.PlainLoginModule required username='admin' password='admin-secret';"
    }
}