package com.example.ordermicroservice.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * https://www.jetbrains.com/help/inspectopedia/DataClassPrivateConstructor.html
 * Data Class 는 constructor 와 같은 기능을 하는 copy() 메소드를 가지고 있다.
 * 따라서 굳이 data class 의 constructor 를 private 으로 할 필요가 없다.
 */
@Document(collection = "orders")
data class Orders (
    @Id val id: String? = null,
    val userId: String,
    val orderNumber: String,
    val orderedTime: String,
    val products: List<Products>,
    val sellerId: String,
) {
    companion object {
        fun of(id: String? = null, userId: String, orderNumber: String, orderedTime: String, products: List<Products>, sellerId: String): Orders {
            return Orders(id, userId, orderNumber, orderedTime, products, sellerId)
        }

        fun noop(): Orders {
            return Orders(null, "noop", "", "", listOf(), "")
        }
    }
}