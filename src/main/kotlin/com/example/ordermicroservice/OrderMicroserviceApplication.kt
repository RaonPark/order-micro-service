package com.example.ordermicroservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableMongoRepositories(basePackages = ["com.example.ordermicroservice.repository.mongo"])
@EnableJpaRepositories(basePackages = ["com.example.ordermicroservice.repository.mysql"])
@EntityScan(basePackages = ["com.example.ordermicroservice.entity"])
@ConfigurationPropertiesScan(basePackages = ["com.example.ordermicroservice.config"])
@EnableScheduling
class OrderMicroserviceApplication

fun main(args: Array<String>) {
    runApplication<OrderMicroserviceApplication>(*args)
}
