package com.promethist.core.runtime

import com.promethist.core.dialogue.BasicDialogue
import com.promethist.core.type.*

class SampleApi(dialogue: BasicDialogue) : DialogueApi(dialogue) {

    data class Application(val name: String, val dialogueName: String)
    class ApplicationList : ArrayList<Application>() // must be defined to be used as generic type

    private val target get() = target("https://admin.promethist.com/user/applications") // base target, to be extended by params per call
    private val headers get() = mapOf("X-header" to "value") // headers for all calls

    fun applications(): ApplicationList = get(target, headers) // simple call, no parameters, no dialogue required

    fun applications2(par1: String = ""): DynamicMutableList = with (dialogue) { // if you want to work in scope of dialogue to access its methods and properties (attributes)
        get(target
                .queryParam("par1", par1)
                .queryParam("lat", clientLocation.latitude)
                .queryParam("lng", clientLocation.longitude)
                , headers)
    }

    fun test1(): Dynamic = load("/test/wcities/test1.json")
}

val BasicDialogue.sample get() = DialogueApi.get<SampleApi>(this)