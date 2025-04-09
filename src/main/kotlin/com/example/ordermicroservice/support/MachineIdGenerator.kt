package com.example.ordermicroservice.support

import java.net.InetAddress

class MachineIdGenerator {
    companion object {
        fun machineId(): Long {
            val hostname = InetAddress.getLocalHost().hostName
            return (hostname.hashCode() and 0x3FF).toLong()
        }
    }
}