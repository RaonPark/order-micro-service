package com.example.ordermicroservice.controller

import com.example.ordermicroservice.document.DepositResult
import com.example.ordermicroservice.dto.DepositRequest
import com.example.ordermicroservice.dto.DepositResponse
import com.example.ordermicroservice.dto.WithdrawRequest
import com.example.ordermicroservice.dto.WithdrawResponse
import com.example.ordermicroservice.service.AccountService
import com.example.ordermicroservice.support.ServiceController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ServiceController
class AccountController(
    private val accountService: AccountService
) {
    @PostMapping("/deposit")
    fun deposit(@RequestBody depositRequest: DepositRequest): ResponseEntity<DepositResponse> {
        return ResponseEntity.ok(accountService.depositNew(depositRequest))
    }

    @PostMapping("/withdraw")
    fun withdraw(@RequestBody withdrawRequest: WithdrawRequest): ResponseEntity<WithdrawResponse> {
        val response = accountService.withdrawNew(withdrawRequest)
        return ResponseEntity.ok(response)
    }
}