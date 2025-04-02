package com.example.ordermicroservice.constants

object KafkaTopicNames {
    const val ORDER_OUTBOX = "order-outbox.topic"
    const val PAYMENT_OUTBOX = "payment-outbox.topic"
    const val SHIPPING = "shipping.topic"
    const val PAYMENT_STATUS = "payment-status.topic"
    const val ORDER_OUTBOX_STATUS = "order-outbox-status.topic"
    const val ACCOUNT_REQUEST = "account-request.topic"
    const val ACCOUNT_REQUEST_RESPONSE = "account-request-response.topic"
    const val THROTTLING_REQUEST = "throttling.request.topic"
    const val THROTTLING_RESPONSE = "throttling.response.topic"
}