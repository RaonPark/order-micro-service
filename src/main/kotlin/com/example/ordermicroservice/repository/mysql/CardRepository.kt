package com.example.ordermicroservice.repository.mysql

import com.example.ordermicroservice.entity.Cards
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CardRepository: JpaRepository<Cards, Long> {
    @Query("select c.accountNumber from Cards c where c.cardNumber = :cardNumber")
    fun findAccountNumberByCardNumber(cardNumber: String): String?
}