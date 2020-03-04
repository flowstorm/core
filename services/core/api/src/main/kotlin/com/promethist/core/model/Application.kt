package com.promethist.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId

class Application(
        var _id: Id<Application> = newId(),
        var name: String,
        var dialogueName: String,
        open var ttsVoice: String/*, properties: <Map, Any>*/)