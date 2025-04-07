package com.example.ordermicroservice.service

import com.avro.account.AccountRequestMessage
import com.avro.account.AccountRequestType
import com.avro.account.AccountVoMessage
import com.example.ordermicroservice.constants.KafkaTopicNames
import com.example.ordermicroservice.document.Accounts
import com.example.ordermicroservice.dto.DepositRequest
import com.example.ordermicroservice.dto.WithdrawRequest
import com.example.ordermicroservice.dto.WithdrawResponse
import com.example.ordermicroservice.support.DateTimeSupport
import com.example.ordermicroservice.vo.AccountRequestChannelMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

@Service
class AccountService(
    private val accountMongoTemplate: MongoTemplate,
    private val accountRequestTemplate: KafkaTemplate<String, AccountRequestMessage>,
    private val redisService: RedisService
) {
    companion object {
        val log = KotlinLogging.logger { }
        val accountRequestChannel = ConcurrentHashMap<String, BlockingQueue<AccountRequestChannelMessage>>()
    }

    @KafkaListener(topics = [KafkaTopicNames.ACCOUNT_REQUEST_RESPONSE],
        concurrency = "3",
        containerFactory = "accountResponseListenerContainer",
        groupId = "ACCOUNT_RESPONSE"
    )
    fun processAccountRequestResponse(record: ConsumerRecord<String, AccountVoMessage>, ack: Acknowledgment) {
        val response = record.value()
        log.info { "$response 에 대한 스트림 처리가 완료되었습니다." }

        val redisBalance = redisService.getBalance(response.accountNumber)
        val findQuery = Query(Criteria.where("accountNumber").`is`(response.accountNumber))
        val dbBalance = accountMongoTemplate.findOne(findQuery, Accounts::class.java)?.balance
            ?: throw RuntimeException("${response.accountNumber}에 해당하는 계좌가 없습니다!")

        if(redisBalance != dbBalance + response.balance) {
            log.info { "${response.accountNumber}의 잔고가 다릅니다. Redis = $redisBalance vs. DB = $dbBalance and streams = ${response.balance}" }
            ack.acknowledge()
            return
        }

        val updateQuery = Update.update("balance", redisBalance)
        val updateResult = accountMongoTemplate.updateFirst(findQuery, updateQuery, Accounts::class.java)

        if(updateResult.matchedCount != 1L && updateResult.modifiedCount != 1L) {
            log.info { "${response.accountNumber} 계좌 업데이트에 실패했습니다." }
        } else {
            log.info { " ${response.accountNumber} 계좌 업데이트가 되었습니다. " }
        }

        ack.acknowledge()
    }

    @ExperimentalCoroutinesApi
    suspend fun deposit(depositRequest: DepositRequest) = coroutineScope {
        val balance = withContext(Dispatchers.IO) {
            redisService.getBalance(depositRequest.accountNumber)
        }

        log.info { "deposit : $balance" }

        if(balance == -1L) {
            val account = accountMongoTemplate.findOne(
                Query(Criteria.where("accountNumber").`is`(depositRequest.accountNumber)),
                Accounts::class.java
            ) ?: throw RuntimeException("${depositRequest.accountNumber}에 해당하는 계좌가 존재하지 않습니다.")
            log.info { "First Deposit! = $account" }
            redisService.saveBalance(accountNumber = account.accountNumber, balance = account.balance)
        }

        redisService.incrBalance(depositRequest.accountNumber, depositRequest.amount)

        val accountRequestMessage = buildAccountRequestMessage(
            accountNumber = depositRequest.accountNumber,
            amount = depositRequest.amount,
            type = AccountRequestType.DEPOSIT
        )

        accountRequestTemplate.executeInTransaction {
            it.send(KafkaTopicNames.ACCOUNT_REQUEST, depositRequest.accountNumber, accountRequestMessage)
        }
    }

    suspend fun depositNew(depositRequest: DepositRequest) {
        val balance = redisService.getBalance(depositRequest.accountNumber)

        log.info { "deposit : $balance" }

        if(balance == -1L) {
            val dbBalance = selectDBBalance(depositRequest.accountNumber)
                ?: throw RuntimeException("${depositRequest.accountNumber}에 해당하는 계좌가 존재하지 않습니다.")
            redisService.saveBalance(accountNumber = depositRequest.accountNumber, balance = dbBalance)
        }

        val cacheBalance = redisService.incrBalance(depositRequest.accountNumber, depositRequest.amount)

        if(!accountRequestChannel.containsKey(depositRequest.accountNumber)) {
            accountRequestChannel[depositRequest.accountNumber] = LinkedBlockingQueue()
        }

        accountRequestChannel[depositRequest.accountNumber]?.add(
            AccountRequestChannelMessage(
                amount = depositRequest.amount,
                accountNumber = depositRequest.accountNumber,
                requestType = "DEPOSIT",
                cacheBalance = cacheBalance
            )
        )

        val finalBalance = processAccountOperations(depositRequest.accountNumber)

        log.info { "계좌 ${depositRequest.accountNumber}에 대한 입금이 완료되었습니다. 계좌 잔고 = $finalBalance" }
    }


    fun withdrawNew(withdrawRequest: WithdrawRequest): WithdrawResponse {
        val balance = redisService.getBalance(withdrawRequest.accountNumber)

        if(balance == -1L) {
            val account = accountMongoTemplate.findOne(
                Query(Criteria.where("accountNumber").`is`(withdrawRequest.accountNumber)),
                Accounts::class.java
            ) ?: throw RuntimeException("${withdrawRequest.accountNumber}에 해당하는 계좌가 존재하지 않습니다.")

            log.info { "First Withdraw! = $account" }

            redisService.saveBalance(accountNumber = account.accountNumber, balance = account.balance)
        }

        val afterBalance = redisService.incrBalance(withdrawRequest.accountNumber, -withdrawRequest.amount)

        if(!accountRequestChannel.containsKey(withdrawRequest.accountNumber)) {
            accountRequestChannel[withdrawRequest.accountNumber] = LinkedBlockingQueue()
        }

        accountRequestChannel[withdrawRequest.accountNumber]?.add(
            AccountRequestChannelMessage(
                amount = withdrawRequest.amount,
                accountNumber = withdrawRequest.accountNumber,
                cacheBalance = afterBalance,
                requestType = "WITHDRAW"
            )
        )

        val finalBalance = processAccountOperations(accountNumber = withdrawRequest.accountNumber)

        log.info { "최종 계좌 ${withdrawRequest.accountNumber}의 잔금 = $finalBalance" }

        log.info { "계좌 ${withdrawRequest.accountNumber}에 대한 출금이 완료되었습니다." }

        return WithdrawResponse.of(
            isValid = true,
            isCompleted = true,
            balance = balance - withdrawRequest.amount,
            processedTime = DateTimeSupport.getNowTimeWithKoreaZoneAndFormatter()
        )
    }

    fun inquiry(accountNumber: String): Long {
        val cacheBalance = redisService.getBalance(accountNumber)

        if(!accountRequestChannel.containsKey(accountNumber)) {
            accountRequestChannel[accountNumber] = LinkedBlockingQueue()
        }

        accountRequestChannel[accountNumber]?.add(AccountRequestChannelMessage(
            amount = 0L,
            accountNumber = accountNumber,
            cacheBalance = cacheBalance,
            requestType = "INQUIRY"
        ))

        return processAccountOperations(accountNumber)
    }

    private fun processAccountOperations(accountNumber: String): Long {
        val requestQueue = accountRequestChannel[accountNumber]!!

        val requestMessage = requestQueue.poll()
        when (requestMessage.requestType) {
            "WITHDRAW" -> {
                val dbBalance = selectDBBalance(accountNumber)
                    ?: throw RuntimeException("${requestMessage.accountNumber}에 해당하는 계좌가 없습니다.")

                if (requestMessage.cacheBalance != dbBalance - requestMessage.amount) {
                    log.info { "cache : ${requestMessage.cacheBalance} != accountDB : $dbBalance and amount : ${-requestMessage.amount}" }
                } else {
                    accountMongoTemplate.updateFirst(
                        Query(Criteria.where("accountNumber").`is`(requestMessage.accountNumber)),
                        Update.update("balance", requestMessage.cacheBalance),
                        Accounts::class.java
                    )
                }

                return dbBalance - requestMessage.amount
            }

            "DEPOSIT" -> {
                val dbBalance = selectDBBalance(accountNumber)
                    ?: throw RuntimeException("${requestMessage.accountNumber}에 해당하는 계좌가 없습니다.")

                if (requestMessage.cacheBalance != dbBalance + requestMessage.amount) {
                    log.info { "cache : ${requestMessage.cacheBalance} != accountDB : $dbBalance and amount : ${-requestMessage.amount}" }
                } else {
                    accountMongoTemplate.updateFirst(
                        Query(Criteria.where("accountNumber").`is`(requestMessage.accountNumber)),
                        Update.update("balance", requestMessage.cacheBalance),
                        Accounts::class.java
                    )
                }

                return dbBalance + requestMessage.amount
            }

            "INQUIRY" -> {
                val dbBalance = selectDBBalance(accountNumber)
                    ?: throw RuntimeException("${requestMessage.accountNumber}에 해당하는 계좌가 없습니다.")

                if (requestMessage.cacheBalance != dbBalance + requestMessage.amount) {
                    log.info { "cache : ${requestMessage.cacheBalance} != accountDB : $dbBalance and amount : ${-requestMessage.amount}" }
                }

                return dbBalance - requestMessage.amount
            }

            else -> {
                log.info { "잘못된 Request Type 입니다 = ${requestMessage.requestType}" }

                return Long.MIN_VALUE
            }
        }
    }

    private fun buildAccountRequestMessage(accountNumber: String, amount: Long, type: AccountRequestType): AccountRequestMessage {
        return AccountRequestMessage.newBuilder()
            .setAccountNumber(accountNumber)
            .setAmount(amount)
            .setRequestType(type)
            .build()
    }

    private fun selectDBBalance(accountNumber: String): Long? {
        val findQuery = Query(Criteria.where("accountNumber").`is`(accountNumber))
        return accountMongoTemplate.findOne(findQuery, Accounts::class.java)?.balance
    }
}