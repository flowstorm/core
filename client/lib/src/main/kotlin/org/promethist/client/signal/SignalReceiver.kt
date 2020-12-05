package org.promethist.client.signal

import org.promethist.core.type.PropertyMap

interface SignalReceiver {

    fun receive(values: PropertyMap)
}