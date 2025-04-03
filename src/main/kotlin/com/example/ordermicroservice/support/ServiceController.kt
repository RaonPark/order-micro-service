package com.example.ordermicroservice.support

import org.springframework.core.annotation.AliasFor
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@RestController
@RequestMapping("/service")
annotation class ServiceController(
    @get:AliasFor(
        annotation = RestController::class
    )
    val value: String = ""
)
