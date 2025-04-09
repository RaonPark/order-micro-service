package com.example.ordermicroservice.support

class SnowflakeIdGenerator(
    private val machineId: Long
) {
    // epoch 41 Bits
    // machine 10 Bits
    // sequence 12 Bits
    // = 63 Bits Long
    // 0 for sign, but snowflake always use positive number.
    private val machineIdBits = 10L
    private val sequenceBits = 12L
    private val epochMillis = 1712652345678L

    // maxMachineId = -1 ^ (-1 << 10L) = 아래 10비트만 남음
    private val maxMachineId = -1L xor (-1L shl machineIdBits.toInt())
    // maxSequenceId = -1 ^ (-1 << 12L) = 아래 12비트만 남음
    private val maxSequenceId = -1L xor (-1L shl sequenceBits.toInt())

    private val machineIdShift = sequenceBits.toInt()
    private val timestampShift = (sequenceBits + machineIdBits).toInt()

    @Volatile private var lastTimestamp = -1L
    @Volatile private var sequenceId = 0L

    init {
        require(machineId in 0..maxMachineId) {
            "SnowFlakeIdGenerator 를 위한 최대 machineId 를 넘었습니다!"
        }
    }

    @Synchronized
    fun nextId(): Long {
        var timestamp = timeGen()

        if(timestamp < lastTimestamp) {
            throw RuntimeException("SnowFlake Id Generator Error!")
        }

        if(timestamp == lastTimestamp) {
            sequenceId = (sequenceId + 1) and maxSequenceId
            if(sequenceId == 0L)
                timestamp = tilNextMillis(lastTimestamp)
        } else {
            sequenceId = 0L
        }

        lastTimestamp = timestamp
        return (((timestamp - epochMillis) shl timestampShift) or
                (machineId shl machineIdShift) or
                sequenceId)
    }

    private fun tilNextMillis(lastTimestamp: Long): Long {
        var timestamp = timeGen()
        while(timestamp <= lastTimestamp) {
            timestamp = timeGen()
        }

        return timestamp
    }

    private fun timeGen() = System.currentTimeMillis()
}