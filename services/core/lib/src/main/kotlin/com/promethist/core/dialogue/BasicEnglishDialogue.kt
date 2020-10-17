package com.promethist.core.dialogue

import com.promethist.common.AppConfig
import com.promethist.core.model.DialogueEvent

abstract class BasicEnglishDialogue : BasicDialogue() {

    //Nodes
    val _goBack = GoBack(_basicId++, repeat = true)
    val _basicVersionGlobalIntent = GlobalIntent(_basicId++, "basicVersionGlobalIntent", 0.99F, "application version")
    val _basicVersionResponse = Response(_basicId++, false, { "Server version ${AppConfig.version}, environment ${AppConfig.instance.get("namespace", "unknown")}, dialogue model $dialogueName version $version" })

    val _basicVolumeUpGlobalIntent = GlobalIntent(_basicId++, "basicVolumeUpGlobalIntent", 0.99F, "volume up", "louder")
    val _basicVolumeUpCommand = Command(_basicId++, "volume", "up")
    val _basicVolumeUpResponse = Response(_basicId++, false, { "volume is up" })

    val _basicVolumeDownGlobalIntent = GlobalIntent(_basicId++, "basicVolumeDownGlobalIntent", 0.99F, "volume down", "quieter")
    val _basicVolumeDownCommand = Command(_basicId++, "volume", "down")
    val _basicVolumeDownResponse = Response(_basicId++, false, { "volume is down" })

    val _basicLogApplicationErrorGlobalIntent = GlobalIntent(_basicId++, "basicLogApplicationErrorGlobalIntent", 0.99F,  "application error", "application problem")
    val _basicLogApplicationErrorResponse1 = Response(_basicId++, { "What's the problem?" })
    val _basicLogApplicationErrorResponse2 = Response(_basicId++, false, { "Thanks. Let's get back." })
    val _basicLogApplicationErrorUserInput = UserInput(_basicId++, arrayOf(), arrayOf()) {
        val transition = Transition(_basicLogApplicationErrorResponse2)
        dialogueEvent = DialogueEvent(this, this@BasicEnglishDialogue, DialogueEvent.Type.UserError, input.alternatives[0].text)
        transition
    }

    val _basicLogApplicationCommentGlobalIntent = GlobalIntent(_basicId++, "basicLogApplicationCommentGlobalIntent", 0.99F, "application command", "application comment")
    val _basicLogApplicationCommentResponse1 = Response(_basicId++, { "What's the comment?" })
    val _basicLogApplicationCommentResponse2 = Response(_basicId++, false, { "Thanks. Let's get back" })
    val _basicLogApplicationCommentUserInput = UserInput(_basicId++, arrayOf(), arrayOf()) {
        val transition = Transition(_basicLogApplicationCommentResponse2)
        dialogueEvent = DialogueEvent(this, this@BasicEnglishDialogue, DialogueEvent.Type.UserComment, input.alternatives[0].text)
        transition
    }

    init {
        _basicVersionGlobalIntent.next = _basicVersionResponse
        _basicVersionResponse.next = _goBack

        _basicVolumeUpGlobalIntent.next = _basicVolumeUpCommand
        _basicVolumeUpCommand.next = _basicVolumeUpResponse
        _basicVolumeUpResponse.next = _goBack

        _basicVolumeDownGlobalIntent.next = _basicVolumeDownCommand
        _basicVolumeDownCommand.next = _basicVolumeDownResponse
        _basicVolumeDownResponse.next = _goBack

        _basicLogApplicationErrorGlobalIntent.next = _basicLogApplicationErrorResponse1
        _basicLogApplicationErrorResponse1.next = _basicLogApplicationErrorUserInput
        _basicLogApplicationErrorResponse2.next = _goBack

        _basicLogApplicationCommentGlobalIntent.next = _basicLogApplicationCommentResponse1
        _basicLogApplicationCommentResponse1.next = _basicLogApplicationCommentUserInput
        _basicLogApplicationCommentResponse2.next = _goBack
    }
}