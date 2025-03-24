package com.example.ordermicroservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableMongoRepositories(basePackages = ["com.example.ordermicroservice.repository.mongo"])
@ConfigurationPropertiesScan(basePackages = ["com.example.ordermicroservice.config"])
@EnableScheduling
class OrderMicroserviceApplication

fun main(args: Array<String>) {
    runApplication<OrderMicroserviceApplication>(*args)
}
