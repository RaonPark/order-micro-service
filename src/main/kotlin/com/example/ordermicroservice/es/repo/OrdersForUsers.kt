package com.example.ordermicroservice.es.repo

import com.example.ordermicroservice.document.Products
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

@Document(indexName = "orders_for_users", createIndex = true)
class OrdersForUsers(
    @Id
    private val id: String? = null,

    @Field(name = "user_id", type = FieldType.Text)
    private val userId: String,

    @Field(name = "order_number", type = FieldType.Text)
    private val orderNumber: String,

    @Field(name = "ordered_time", type = FieldType.Text)
    private val orderedTime: String,

    @Field(name = "products", type = FieldType.Object, store = false)
    private val products: List<Products>,
) {
}