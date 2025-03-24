package com.example.ordermicroservice.document

/**
 * enum class 와 sealed class 의 차이
 * https://stackoverflow.com/questions/49169086/sealed-class-vs-enum-when-using-associated-type
 */
enum class PaymentType(private val type: String) {
    CASH("현금"),
    CREDIT("신용카드"),
    DEBIT("체크카드"),
    COUPON("쿠폰"),
    NO_OP("알수없음");

    companion object {
        fun of(type: String): PaymentType {
            return entries.findLast { it.type == type } ?: NO_OP
        }
    }

    fun getPaymentType(): String = type
}