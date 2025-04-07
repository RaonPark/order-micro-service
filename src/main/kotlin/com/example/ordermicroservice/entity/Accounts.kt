package com.example.ordermicroservice.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.util.*

@Entity(name = "Accounts")
class Accounts(
    accountNumber: String,
    accountPassword: String,
    userId: String,
    balance: Long,
): Persistable<UUID> {
    @Id
    private val id: UUID = UUID.randomUUID()

    @Column(nullable = false)
    var accountNumber = accountNumber
    @Column(nullable = false)
    var accountPassword = accountPassword
    @Column(nullable = false)
    var userId = userId
    @Column(nullable = false)
    var balance = balance


    override fun getId(): UUID = id

    @Transient
    private var _isNew = true

    override fun isNew(): Boolean = _isNew

    @PostPersist
    @PostLoad
    protected fun load() {
        _isNew = false
    }
}