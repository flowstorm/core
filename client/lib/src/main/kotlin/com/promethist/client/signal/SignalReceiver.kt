package com.promethist.client.signal

import com.promethist.core.type.PropertyMap

interface SignalReceiver {

    fun receive(values: PropertyMap)
}