package com.example.ordermicroservice.service

import com.example.ordermicroservice.document.Accounts
import com.example.ordermicroservice.dto.DepositRequest
import com.example.ordermicroservice.repository.mongo.AccountRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class AccountServiceTest: BehaviorSpec({
    extensions(SpringExtension)

    val ACCOUNT_NUMBER = "123-123-123-123"
    val ACCOUNT_PASSWORD = "1234"
    val USER_ID = "raonpark"
    val AMOUNT = 10000L
    val BALANCE = 100000L

    val accountRepository = mockk<AccountRepository>()
    val accountService = AccountService(accountRepository)

    Given("계정이 주어집니다.") {
        val account = Accounts.of(
            id = null,
            accountNumber = ACCOUNT_NUMBER,
            accountPassword = ACCOUNT_PASSWORD,
            userId = USER_ID,
            balance = 0L
        )

        val depositRequest = DepositRequest.of(
            accountNumber = ACCOUNT_NUMBER,
            amount = AMOUNT,
        )

        val savedAccount = Accounts.of(
            id = "1",
            accountNumber = ACCOUNT_NUMBER,
            accountPassword = ACCOUNT_PASSWORD,
            userId = USER_ID,
            balance = BALANCE + AMOUNT
        )

        When("계좌에 입금합니다.") {
            every { accountRepository.updateByBalance(depositRequest.amount) } answers { savedAccount }
            val depositResponse = accountService.deposit(depositRequest)

            Then("계좌를 확인해본다.") {
                depositResponse.balance shouldBe 110000L
            }
        }
    }
})