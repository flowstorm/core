package com.promethist.client.signal

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.readValue
import com.promethist.common.ObjectUtil.defaultMapper
import jdk.nashorn.internal.runtime.PropertyMap
import java.io.FileInputStream
import java.io.InputStream

class SignalProcessor(
    val groups: Array<SignalGroup>,
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
            JsonSubTypes.Type(value = SignalUrlProvider::class, name = "SignalUrlProvider"),
            JsonSubTypes.Type(value = SignalFileProvider::class, name = "SignalFileProvider"),
            JsonSubTypes.Type(value = SignalProcessProvider::class, name = "SignalProcessProvider")
    )
    val providers: MutableList<SignalProvider>) : Runnable {

    private var receiver: ((String, PropertyMap) -> Unit)? = null
    private val values = mutableMapOf<String, Any>()

    fun pass(values: PropertyMap) {

    }

    override fun run() = providers.forEach { it.run() }

    companion object {

        fun create(config: InputStream, receiver: (String, PropertyMap) -> Unit) =
                defaultMapper.readValue<SignalProcessor>(config).apply {
                    this.receiver = receiver
                }

        @JvmStatic
        fun main(args: Array<String>) {
            val processor = create(FileInputStream("signal-config.json")) { text, values ->

            }
            processor.run()
        }
    }
}