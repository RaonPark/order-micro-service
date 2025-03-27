package com.example.ordermicroservice.service

import com.avro.account.AccountRequestMessage
import com.avro.account.AccountRequestType
import com.avro.account.AccountVoMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.Accounts
import com.example.ordermicroservice.dto.DepositRequest
import com.example.ordermicroservice.dto.DepositResponse
import com.mongodb.client.result.UpdateResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.update
import org.springframework.data.mongodb.core.updateFirst
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class AccountService(
    private val accountMongoTemplate: MongoTemplate,
    private val accountRequestTemplate: KafkaTemplate<String, AccountRequestMessage>,
    private val redisService: RedisService
) {
    companion object {
        val log = KotlinLogging.logger { }
    }

    @KafkaListener(topics = [KafkaTopicNames.ACCOUNT_REQUEST_RESPONSE],
        concurrency = "3",
        containerFactory = "accountResponseListenerContainer",
        groupId = "ACCOUNT_RESPONSE"
        )
    fun processAccountRequestResponse(record: ConsumerRecord<String, AccountVoMessage>) {
        val response = record.value()
        log.info { "$response 에 대한 스트림 처리가 완료되었습니다." }

        val redisBalance = redisService.getBalance(response.accountNumber)
        val findQuery = Query(Criteria.where("accountNumber").`is`(response.accountNumber))
        val dbBalance = accountMongoTemplate.findOne(findQuery, Accounts::class.java)?.balance
            ?: throw RuntimeException("${response.accountNumber}에 해당하는 계좌가 없습니다!")

        if(redisBalance != dbBalance + response.balance) {
            log.info { "${response.accountNumber}의 잔고가 다릅니다. Redis = $redisBalance vs. DB = $dbBalance and streams = ${response.balance}" }
            return
        }

        val updateQuery = Update.update("balance", redisBalance)
        val updateResult = accountMongoTemplate.updateFirst(findQuery, updateQuery, Accounts::class.java)

        if(updateResult.matchedCount != 1L && updateResult.modifiedCount != 1L) {
            log.info { "${response.accountNumber} 계좌 업데이트에 실패했습니다." }
        } else {
            log.info { " ${response.accountNumber} 계좌 업데이트가 되었습니다. " }
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun deposit(depositRequest: DepositRequest) = coroutineScope {
        launch {
            val balance = redisService.getBalance(depositRequest.accountNumber)

            log.info { "deposit : $balance" }

            if(balance == -1L) {
                val account = accountMongoTemplate.findOne(
                    Query(Criteria.where("accountNumber").`is`(depositRequest.accountNumber)),
                    Accounts::class.java
                ) ?: throw RuntimeException("${depositRequest.accountNumber}에 해당하는 계좌가 존재하지 않습니다.")
                log.info { "First Deposit! = $account" }
                redisService.saveBalance(accountNumber = account.accountNumber, balance = account.balance)
            }
        }

        launch {
            redisService.incrBalance(depositRequest.accountNumber, depositRequest.amount)
        }

        launch {
            val accountRequestMessage = buildAccountRequestMessage(
                accountNumber = depositRequest.accountNumber,
                amount = depositRequest.amount,
                type = AccountRequestType.DEPOSIT
            )

            accountRequestTemplate.send(KafkaTopicNames.ACCOUNT_REQUEST, depositRequest.accountNumber, accountRequestMessage)
        }

        coroutineContext[Job]
            ?.children
            ?.forEach { it.join() }
    }

    private fun buildAccountRequestMessage(accountNumber: String, amount: Long, type: AccountRequestType): AccountRequestMessage {
        return AccountRequestMessage.newBuilder()
            .setAccountNumber(accountNumber)
            .setAmount(amount)
            .setRequestType(type)
            .build()
    }
}