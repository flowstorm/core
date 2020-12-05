package org.promethist.client.signal

interface SignalProvider : Runnable {

    var processor: SignalProcessor
}