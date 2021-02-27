package ai.flowstorm.client.signal

interface SignalProvider : Runnable {

    var processor: SignalProcessor
}