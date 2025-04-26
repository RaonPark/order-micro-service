package com.example.ordermicroservice.es.repo

enum class ShippingStatus(private val text: String) {
    BEFORE_SHIPPING("배송전"),
    SHIPPING("배송중"),
    SHIPPING_COMPLETED("배송완료");

    fun getShippingStatusAsString(): String {
        return text
    }
}