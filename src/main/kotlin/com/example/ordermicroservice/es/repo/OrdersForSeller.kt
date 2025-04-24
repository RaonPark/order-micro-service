package com.example.ordermicroservice.es.repo

import com.example.ordermicroservice.document.Products
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

@Document(indexName = "orders_for_seller", createIndex = true)
class OrdersForSeller(
    @Id
    private val id: String? = null,

    @Field(name = "order_number", type = FieldType.Text)
    val orderNumber: String,

    @Field(name = "products", type = FieldType.Object, store = false)
    val products: MutableList<Products>,

    @Field(name = "user_id", type = FieldType.Text)
    val userId: String,

    @Field(name = "shipping_location", type = FieldType.Text)
    val shippingLocation: String,

    @Field(name = "ordered_time", type = FieldType.Text)
    val orderedTime: String,

    @Field(name = "seller_id", type = FieldType.Text)
    val sellerId: String,
)

