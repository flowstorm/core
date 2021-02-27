package ai.flowstorm.client.signal

import ai.flowstorm.core.type.PropertyMap

interface SignalReceiver {

    fun receive(values: PropertyMap)
}