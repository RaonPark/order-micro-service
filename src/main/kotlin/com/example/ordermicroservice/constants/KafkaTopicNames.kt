package com.example.ordermicroservice.constants

object KafkaTopicNames {
    const val THROTTLING_REQUEST = "throttling.request.topic"
    const val THROTTLING_RESPONSE = "throttling.response.topic"

    const val ORDER_OUTBOX = "order-outbox.topic"
    const val ORDER_OUTBOX_STATUS = "order-outbox-status.topic"
    const val ORDER_REQUEST = "order.request.topic"
    const val ORDER_REFUND = "order.refund.topic"
    const val ORDER_RETRIEVE = "order.retrieve.topic"

    const val PAYMENT_OUTBOX = "payment-outbox.topic"
    const val PAYMENT_REQUEST = "payment.request.topic"
    const val PAYMENT_CANCEL = "payment.cancel.topic"
    const val PAYMENT_STATUS = "payment-status.topic"

    const val SHIPPING = "shipping.topic"
    const val SHIPPING_CANCEL = "shipping.cancel.topic"

    const val ACCOUNT_REQUEST = "account-request.topic"
    const val ACCOUNT_REQUEST_RESPONSE = "account-request-response.topic"

    const val COMPENSATION_REQUEST = "compensation.request.topic"
}