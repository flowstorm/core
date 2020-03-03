package com.promethist.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId

open class Application(open var _id: Id<Application> = newId(), open var name: String, open var dialogueName: String, open var ttsVoice: String/*, properties: <Map, Any>*/)