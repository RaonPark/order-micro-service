package com.example.ordermicroservice.document

enum class ProcessStage(val stage: String) {
    BEFORE_PROCESS("처리전"),
    PENDING("처리중"),
    PROCESSED("처리됨"),
    BEFORE_CANCEL("취소전"),
    CANCELED("취소됨"),
    NO_OP("아무동작하지않음"),
    EXCEPTION("예외");

    companion object {
        fun of(enumName: String): ProcessStage =
            entries.findLast { it.name == enumName } ?: NO_OP
    }

    fun toAvro(): com.avro.support.ProcessStage {
        return com.avro.support.ProcessStage.entries.zip(entries).findLast {
            it.first.name == it.second.name
        }?.first ?: com.avro.support.ProcessStage.NO_OP
    }
}