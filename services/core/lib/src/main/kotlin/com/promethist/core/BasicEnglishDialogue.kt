package com.promethist.core

import com.promethist.core.Dialogue
import com.promethist.core.runtime.Loader

class BasicEnglishDialogue(loader: Loader, name: String) : Dialogue(loader, name) {

    var basicId = 1
    val basicVersionGlobalIntent = GlobalIntent(basicId++, "basicVolumeUpGlobalIntent", "version")
    val basicVersionSubDialogue = SubDialogue(basicId++, "\$basic/version/1") {
        object : Dialogue(loader, name) {
            val response = Response(nextId--, { "\$version, dialogue $name" })
            init {
                start.next = response
                response.next = stop
            }
        }
    }
    val basicVolumeUpGlobalIntent = GlobalIntent(basicId++, "basicVolumeUpGlobalIntent", "volume up", "louder")
    val basicVolumeUpSubDialogue = SubDialogue(basicId++, "\$basic/volumeUp/1") {
        object : Dialogue(loader, name) {
            val response = Response(nextId--, { "\$volume_up setting volume up" })
            init {
                start.next = response
                response.next = stop
            }
        }
    }
    val basicVolumeDownGlobalIntent = GlobalIntent(basicId++, "basicVolumeDownGlobalIntent", "volume down", "quieter")
    val basicVolumeDownSubDialogue = SubDialogue(basicId++, "\$basic/volumeDown/1") {
        object : Dialogue(loader, name) {
            val response = Response(nextId--, { "\$volume_up setting volume down" })
            init {
                start.next = response
                response.next = stop
            }
        }
    }

    init {
        basicVersionGlobalIntent.next = basicVersionSubDialogue
        basicVolumeUpGlobalIntent.next = basicVolumeUpSubDialogue
        basicVolumeDownGlobalIntent.next = basicVolumeDownSubDialogue
        basicVersionSubDialogue.next = repeat
        basicVolumeUpSubDialogue.next = repeat
        basicVolumeDownSubDialogue.next = repeat
    }
}