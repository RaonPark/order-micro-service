package com.example.ordermicroservice.gateway

import com.avro.support.ThrottlingRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.common.serialization.Serdes.LongSerde
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.ProcessorSupplier
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.StoreBuilder
import org.apache.kafka.streams.state.Stores
import java.time.Duration

class FixedWindowThrottlingProcessor: ProcessorSupplier<String, ThrottlingRequest, String, ThrottlingRequest> {
    companion object {
        val log = KotlinLogging.logger {  }
    }
    private val STORE_NAME = "FIXED_WINDOW_THROTTLING_STORE"
    override fun get(): Processor<String, ThrottlingRequest, String, ThrottlingRequest> {
        return object : Processor<String, ThrottlingRequest, String, ThrottlingRequest> {
            private lateinit var context: ProcessorContext<String, ThrottlingRequest>
            private lateinit var store: KeyValueStore<String, Long>

            override fun init(context: ProcessorContext<String, ThrottlingRequest>?) {
                this.context = context ?: throw RuntimeException("Kafka Streams Node Error")
                this.store = context.getStateStore(STORE_NAME)

                context.schedule(
                    Duration.ofSeconds(3L),
                    PunctuationType.WALL_CLOCK_TIME
                ) { _ ->
                    val iterator = store.all()
                    while(iterator.hasNext()) {
                        val request = iterator.next()
                        if(request.value < System.currentTimeMillis() - 3000L) {
                            store.delete(request.key)
                        }
                    }
                }
            }

            override fun process(p0: Record<String, ThrottlingRequest>?) {
                if(p0 == null) {
                    log.info { "error has occurred." }
                    return
                }
                val request = p0.value()
                    ?: throw RuntimeException("Kafka Streams Node Error!")

                var requestAgg = store.get(p0.key())
                    ?: 0L

                if(GatewayRouter.isService(request.apiName)) {
                    requestAgg++
                }

                store.put(p0.key(), requestAgg)
                context.forward(p0.withValue(
                    ThrottlingRequest.newBuilder()
                        .setTimestamp(request.timestamp)
                        .setApiName(request.apiName)
                        .setRequestMethod(request.requestMethod)
                        .setBody(request.body)
                        .setHeader(request.header)
                        .setRequested(requestAgg)
                        .build()))
            }
        }
    }

    override fun stores(): MutableSet<StoreBuilder<*>> {
        return mutableSetOf(
            Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(STORE_NAME),
            StringSerde(),
            LongSerde()
        ))
    }
}