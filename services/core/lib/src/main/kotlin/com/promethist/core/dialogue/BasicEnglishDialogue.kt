package com.promethist.core.dialogue

import com.promethist.core.model.DialogueEvent
import java.util.*

abstract class BasicEnglishDialogue() : BasicDialogue() {

    var basicId = 1
    //Nodes
    val _goBack = GoBack(basicId++, repeat = true)
    val _basicVersionGlobalIntent = GlobalIntent(basicId++, "basicVolumeUpGlobalIntent", "version")
    val _basicVersionResponse = Response(basicId++, { "\$version, dialogue $dialogueName" })

    val _basicVolumeUpGlobalIntent = GlobalIntent(basicId++, "basicVolumeUpGlobalIntent", "volume up", "louder")
    val _basicVolumeUpResponse = Response(basicId++, { "\$volume_up setting volume up" })

    val _basicVolumeDownGlobalIntent = GlobalIntent(basicId++, "basicVolumeDownGlobalIntent", "volume down", "quieter")
    val _basicVolumeDownResponse = Response(basicId++, { "\$volume_down setting volume down" })

    val _basicLogApplicationErrorGlobalIntent = GlobalIntent(basicId++, "basicLogApplicationErrorGlobalIntent", "application error", "application problem")
    val _basicLogApplicationErrorResponse1 = Response(basicId++, {"What is the problem?"})
    val _basicLogApplicationErrorResponse2 = Response(basicId++, {"Thank you for the report. Let's get back now"})
    val _basicLogApplicationErrorUserInputTransition = Transition(_basicLogApplicationErrorResponse2)
    val _basicLogApplicationErrorUserInput = UserInput(basicId++, arrayOf()) {
        val transition = Transition(_basicLogApplicationErrorResponse2)
        dialogueEvent = DialogueEvent(datetime = Date(), type = DialogueEvent.Type.userError, userId = user._id, sessionId = session._id, applicationName = application.name, dialogueName = application.dialogueName, nodeId = turn.endFrame?.nodeId, text = input.transcript.text)
        transition
    }

    val _basicLogApplicationCommentGlobalIntent = GlobalIntent(basicId++, "basicLogApplicationCommentGlobalIntent", "application command", "application comment")
    val _basicLogApplicationCommentResponse1 = Response(basicId++, {"What would you like to comment?"})
    val _basicLogApplicationCommentResponse2 = Response(basicId++, {"Thank you for the report. Let's get back now"})
    val _basicLogApplicationCommentUserInputTransition = Transition(_basicLogApplicationCommentResponse2)
    val _basicLogApplicationCommentUserInput = UserInput(basicId++, arrayOf()) {
        val transition = Transition(_basicLogApplicationCommentResponse2)
        dialogueEvent = DialogueEvent(datetime = Date(), type = DialogueEvent.Type.userComment, userId = user._id, sessionId = session._id, applicationName = application.name, dialogueName = application.dialogueName, nodeId = turn.endFrame?.nodeId, text = input.transcript.text)
        transition
    }

    init {
        _basicVersionGlobalIntent.next = _basicVersionResponse
        _basicVersionResponse.next = _goBack

        _basicVolumeUpGlobalIntent.next = _basicVolumeUpResponse
        _basicVolumeUpResponse.next = _goBack

        _basicVolumeDownGlobalIntent.next = _basicVolumeDownResponse
        _basicVolumeDownResponse.next = _goBack

        _basicLogApplicationErrorGlobalIntent.next = _basicLogApplicationErrorResponse1
        _basicLogApplicationErrorResponse1.next = _basicLogApplicationErrorUserInput
        _basicLogApplicationErrorResponse2.next = _goBack

        _basicLogApplicationCommentGlobalIntent.next = _basicLogApplicationCommentResponse1
        _basicLogApplicationCommentResponse1.next = _basicLogApplicationCommentUserInput
        _basicLogApplicationCommentResponse2.next = _goBack
    }
}