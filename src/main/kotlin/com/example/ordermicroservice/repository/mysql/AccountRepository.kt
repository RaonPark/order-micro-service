package com.example.ordermicroservice.repository.mysql

import com.example.ordermicroservice.entity.Accounts
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository(value = "JpaAccountRepository")
interface AccountRepository: JpaRepository<Accounts, UUID> {
    fun findByAccountNumber(accountNumber: String): Accounts?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Accounts a where a.accountNumber = :accountNumber")
    fun findWithPessimisticLockByAccountNumber(accountNumber: String): Accounts?
}