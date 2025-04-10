package com.example.ordermicroservice.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable

@Entity
class Cards (
    cardNumber: String,
    cardCvc: String,
    cardPassword: String,
    username: String,
    accountNumber: String,
): Persistable<Long> {
    @Id
    @GeneratedValue
    private val id: Long = 0L

    @Column(nullable = false)
    private val cardNumber = cardNumber

    @Column(nullable = false)
    private val cardCvc = cardCvc

    @Column(nullable = false)
    private val cardPassword = cardPassword

    @Column(nullable = false)
    private val username = username

    @Column(nullable = false)
    private val accountNumber = accountNumber

    override fun getId(): Long = id

    @Transient
    private var _isNew = true

    override fun isNew(): Boolean = _isNew

    @PostPersist
    @PostLoad
    protected fun load() {
        _isNew = false
    }
}