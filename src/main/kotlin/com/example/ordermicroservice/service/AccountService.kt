package com.example.ordermicroservice.service

import com.example.ordermicroservice.dto.DepositRequest
import com.example.ordermicroservice.dto.DepositResponse
import com.example.ordermicroservice.dto.WithdrawRequest
import com.example.ordermicroservice.dto.WithdrawResponse
import com.example.ordermicroservice.repository.mysql.AccountRepository
import com.example.ordermicroservice.support.DateTimeSupport
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccountService(
    @Qualifier("JpaAccountRepository") private val accountRepository: AccountRepository,
    private val redisService: RedisService
) {
    companion object {
        val log = KotlinLogging.logger { }
    }

    @Transactional
    fun depositNew(depositRequest: DepositRequest): DepositResponse {
        val account = accountRepository.findWithPessimisticLockByAccountNumber(depositRequest.accountNumber)
            ?: throw RuntimeException("${depositRequest.accountNumber}에 해당하는 계좌가 존재하지 않습니다.")

        account.balance += depositRequest.amount

        redisService.saveBalance(depositRequest.accountNumber, account.balance)

        log.info { "최종 계좌 ${depositRequest.accountNumber}의 잔금 = ${account.balance}" }

        log.info { "계좌 ${depositRequest.accountNumber}에 대한 입금이 완료되었습니다. 계좌 잔고 = ${account.balance}" }

        return DepositResponse.of(
            accountNumber = depositRequest.accountNumber,
            depositResult = true,
            processedTime = DateTimeSupport.getNowTimeWithKoreaZoneAndFormatter()
        )
    }

    @Transactional
    fun withdrawNew(withdrawRequest: WithdrawRequest): WithdrawResponse {
        val account = accountRepository.findWithPessimisticLockByAccountNumber(withdrawRequest.accountNumber)
            ?: throw RuntimeException("${withdrawRequest.accountNumber}에 해당하는 계좌가 존재하지 않습니다.")

        account.balance -= withdrawRequest.amount

        redisService.saveBalance(withdrawRequest.accountNumber, account.balance)

        log.info { "최종 계좌 ${withdrawRequest.accountNumber}의 잔금 = ${account.balance}" }

        log.info { "계좌 ${withdrawRequest.accountNumber}에 대한 출금이 완료되었습니다." }

        return WithdrawResponse.of(
            isValid = true,
            isCompleted = true,
            balance = account.balance,
            processedTime = DateTimeSupport.getNowTimeWithKoreaZoneAndFormatter()
        )
    }

    fun inquiry(accountNumber: String): Long {
        return loadBalanceCacheIfMiss(accountNumber)
    }

    private fun loadBalanceCacheIfMiss(accountNumber: String): Long {
        val balance = redisService.getBalance(accountNumber)

        if(balance == -1L) {
            val account = accountRepository.findByAccountNumber(accountNumber)
                ?: throw RuntimeException("${accountNumber}에 해당하는 계좌가 존재하지 않습니다.")

            redisService.saveBalance(accountNumber = account.accountNumber, balance = account.balance)
        }

        return balance
    }
}