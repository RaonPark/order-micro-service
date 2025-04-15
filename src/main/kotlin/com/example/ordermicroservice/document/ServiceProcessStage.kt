package com.example.ordermicroservice.document

enum class ServiceProcessStage {
    NOT_PROCESS,
    CONFIRM,
    CANCELED,
    EXCEPTION;
}