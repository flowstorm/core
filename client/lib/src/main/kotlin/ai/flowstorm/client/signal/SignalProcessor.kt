package ai.flowstorm.client.signal

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.readValue
import ai.flowstorm.common.ObjectUtil.defaultMapper
import ai.flowstorm.core.type.Location
import ai.flowstorm.core.type.MutablePropertyMap
import ai.flowstorm.core.type.PropertyMap
import ai.flowstorm.util.LoggerDelegate
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.abs

class SignalProcessor(
    val groups: Array<SignalGroup>,
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
            JsonSubTypes.Type(value = SignalUrlProvider::class, name = "url"),
            JsonSubTypes.Type(value = SignalFileProvider::class, name = "file"),
            JsonSubTypes.Type(value = SignalSocketProvider::class, name = "socket"),
            JsonSubTypes.Type(value = SignalProcessProvider::class, name = "process")
    )
    val providers: MutableList<SignalProvider>) : Runnable {

    private val logger by LoggerDelegate()
    lateinit var emitter: ((SignalGroup, PropertyMap) -> Unit)
    private val signalValues = mutableMapOf<String, SignalValue>()
    var paused = false

    fun process(values: Map<String, Any>) {
        val time = System.currentTimeMillis()
        values.forEach { signal ->
            if (!signalValues.containsKey(signal.key))
                signalValues[signal.key] = SignalValue(signal.value)
            else
                signalValues[signal.key]!!.let {
                    it.value = signal.value
                    it.time = time
                }
        }

        groups.forEach { group ->
            val groupValues: MutablePropertyMap = mutableMapOf()
            group.signals.forEach { signal ->
                signalValues[signal.name]?.apply {
                    if (signalHasChanged(signal, this) &&
                            (lastEmittedTime == null || (time - lastEmittedTime!! > signal.timeout ))) {
                        groupValues[signal.name] = value
                        if (signal.resetValue) {
                            signalValues.remove(signal.name)
                        } else {
                            lastEmittedTime = time
                            lastEmittedValue = value
                        }
                    }
                }
            }
            if (groupValues.isNotEmpty()) {
                logger.info("Signal group '${group.name}' type ${group.type} values $groupValues")
                try {
                    emitter(group, groupValues)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun signalHasChanged(signal: Signal, signalValue: SignalValue) =
            if (signal.requiredValue != null)
                signal.requiredValue == signalValue.value
            else
                signalValue.lastEmittedValue == null || signalValue.value.let { value ->
                    val lastValue = signalValue.lastEmittedValue!!
                    when (value) {
                        is Int ->
                            abs(value - lastValue as Int) >= signal.threshold.toInt()
                        is Double ->
                            abs(value - lastValue as Double) >= signal.threshold
                        is Location -> {
                            abs(value.latitude!! - (lastValue as Location).latitude!!) >= signal.threshold ||
                            abs(value.longitude!! - lastValue.longitude!!) >= signal.threshold
                        } else ->
                            lastValue != value
                    }
                }

    override fun run() = providers.forEach { provider ->
        provider.processor = this
        Thread(provider).start()
    }

    class Test(val signalProcessor: SignalProcessor)

    companion object {

        fun create(config: InputStream, emitter: (SignalGroup, PropertyMap) -> Unit) =
                defaultMapper.readValue<SignalProcessor>(config).apply {
                    this.emitter = emitter
                }

        fun test(config: String) = with (defaultMapper.readValue<Test>(FileInputStream(config)).signalProcessor) {
            emitter = { group, values ->
                println("signal group '${group.name}' type ${group.type} values $values")
            }
            run()
        }

        @JvmStatic
        fun main(args: Array<String>) = test("signal-test.json")
    }
}