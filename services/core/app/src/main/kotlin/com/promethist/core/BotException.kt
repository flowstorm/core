package com.promethist.core

class BotException(var type: Type, var id: String, override var message: String? = null) : Exception(message) {
    enum class Type {
        NO_VALID_USER,
        NO_VALID_DEVICE,
        NO_APPLICATIONS
    }
}