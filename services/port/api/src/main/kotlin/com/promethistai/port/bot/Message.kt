package com.promethistai.port.bot

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.promethistai.common.DataObject
import java.io.Serializable

@JsonDeserialize(using = Message.Deserializer::class)
class Message : DataObject {

    class Deserializer: DataObject.Deserializer<Message>(Message::class.java)

    constructor(): super()

    constructor(props: Map<String, Serializable>): super(props)

    constructor(text: String, props: Map<String, Serializable> = emptyMap()): super(props) {
        this.text = text
    }

    var text: String
        get() = if ( this["text"] != null) (this["text"] as String) else ""
        set(value) {
            this["text"] = value
        }

}