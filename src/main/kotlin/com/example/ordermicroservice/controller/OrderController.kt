package com.example.ordermicroservice.controller

import com.avro.OrderOutboxMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.dto.CreateOrderRequest
import com.example.ordermicroservice.dto.CreateOrderResponse
import com.example.ordermicroservice.service.OrderService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.http.ResponseEntity
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

@RestController
class OrderController(
    private val orderService: OrderService
) {
    @PostMapping("/createOrder")
    fun createOrder(@RequestBody orderRequest: CreateOrderRequest): ResponseEntity<CreateOrderResponse> {
        val orderFuture = CompletableFuture.supplyAsync {
            orderService.createOrder(orderRequest)
        }

        val orderResponse = orderFuture.join()

        return ResponseEntity.ok(orderResponse)
    }

    @SendTo("/topic/receipt")
    @KafkaListener(topics = [KafkaTopicNames.ORDER_OUTBOX], containerFactory = "orderListenerContainer")
    fun sendReceipt(record: ConsumerRecord<String, OrderOutboxMessage>) {

    }
}