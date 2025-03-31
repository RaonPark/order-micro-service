package com.example.ordermicroservice.service

import com.avro.account.AccountRequestMessage
import com.example.ordermicroservice.document.Accounts
import com.example.ordermicroservice.dto.DepositRequest
import com.example.ordermicroservice.repository.mongo.AccountRepository
import com.mongodb.client.result.UpdateResult
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.kafka.core.KafkaTemplate

class AccountServiceTest: BehaviorSpec({
    extensions(SpringExtension)

    val ACCOUNT_NUMBER = "123-123-123-123"
    val ACCOUNT_PASSWORD = "1234"
    val USER_ID = "raonpark"
    val AMOUNT = 10000L
    val BALANCE = 100000L

    val accountRepository = mockk<AccountRepository>()
    val kafkaTemplate = mockk<KafkaTemplate<String, AccountRequestMessage>>()
    val accountTemplate = mockk<MongoTemplate>()
    val redisService = mockk<RedisService>()
    val accountService = AccountService(accountTemplate, kafkaTemplate, redisService)

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
            every { accountTemplate.updateFirst(any(Query::class), any(Update::class), Accounts::class.java) } answers { UpdateResult.acknowledged(1L, 1L, null) }
            val depositResponse = accountService.deposit(depositRequest)

            Then("계좌를 확인해본다.") {
            }
        }
    }
})