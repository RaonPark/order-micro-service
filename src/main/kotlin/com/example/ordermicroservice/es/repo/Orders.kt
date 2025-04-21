package com.example.ordermicroservice.es.repo

import com.example.ordermicroservice.document.Products
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field

@Document(indexName = "Orders")
class Orders(
    @Id
    private val id: String? = null,

    @Field
    private val orderNumber: String,

    @Field
    private val products: List<Products>
) {

}