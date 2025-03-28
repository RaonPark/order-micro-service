package com.example.ordermicroservice.controller

import com.example.ordermicroservice.document.DepositResult
import com.example.ordermicroservice.dto.DepositRequest
import com.example.ordermicroservice.dto.DepositResponse
import com.example.ordermicroservice.service.AccountService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AccountController(
    private val accountService: AccountService
) {
    @PostMapping("/deposit")
    @ExperimentalCoroutinesApi
    suspend fun deposit(@RequestBody depositRequest: DepositRequest): DepositResponse {
        accountService.deposit(depositRequest)

        return DepositResponse.of(
            depositResult = DepositResult.SUCCESS,
            accountNumber = depositRequest.accountNumber
        )
    }
}