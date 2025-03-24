package com.example.ordermicroservice.vo

import com.example.ordermicroservice.document.PaymentType
import com.example.ordermicroservice.document.Payments

data class PaymentVo(
    val paymentId: String,
    val paymentType: PaymentType,
    val cardNumber: String? = null,
    val cardCvc: String? = null,
    val amount: Long
) {
    companion object {
        fun document2Pojo(payment: Payments): PaymentVo {
            return PaymentVo(paymentId = payment.paymentId, paymentType = payment.paymentType,
                cardNumber = payment.cardNumber, cardCvc = payment.cardCvc, amount = payment.amount)
        }
    }
}