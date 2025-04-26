package com.example.ordermicroservice.es.repo

import com.example.ordermicroservice.document.Products
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

@Document(indexName = "orders_for_user", createIndex = true)
class OrdersForUser(
    @Id
    private val id: String? = null,

    @Field(name = "user_id", type = FieldType.Text)
    val userId: String,

    @Field(name = "order_number", type = FieldType.Text)
    val orderNumber: String,

    @Field(name = "ordered_time", type = FieldType.Text)
    val orderedTime: String,

    @Field(name = "products", type = FieldType.Object, store = false)
    val products: List<Products>,

    @Field(name = "shipping_status", type = FieldType.Object, store = false)
    val shippingStatus: ShippingStatus = ShippingStatus.BEFORE_SHIPPING
) {
}