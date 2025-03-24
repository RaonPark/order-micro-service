package com.example.ordermicroservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@EnableWebSocketMessageBroker
@Configuration
class WebsocketConfig: WebSocketMessageBrokerConfigurer {
    // message broker 의 이름이 /topic 이다. 따라서 백엔드에서 메세지를 보내줄 때 /topic/* 으로 보내준다.
    // stomp broker 의 이름이 /app 이다. 따라서 프론트엔드에서 메세지를 받을 때 (subscribe) /app/* 으로 받는다.
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")
        registry.setApplicationDestinationPrefixes("/app")
    }

    // stomp 를 연결하기 위한 endpoint 로 이용된다.
    // 프론트엔드에서는 /outbox 를 통해 connect 를 진행한다.
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/outbox")
    }
}