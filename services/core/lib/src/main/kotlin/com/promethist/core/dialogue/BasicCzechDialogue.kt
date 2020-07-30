package com.promethist.core.dialogue

import com.promethist.common.AppConfig
import com.promethist.core.model.DialogueEvent

abstract class BasicCzechDialogue() : BasicDialogue() {

    override fun evaluateTextTemplate(text: String) = super.evaluateTextTemplate(text).run {
        if (gender == "female") { //Inflections based on User Gender
            replace("l@", "la").
            replace("sám@", "sama").
            replace("ý@", "á").
            replace("@", "a")
        } else {
            replace("@", "")
        }. //Automatic addition of <s> tags
        replace(Regex("^"), "<s>").
        replace(Regex("$"), "</s>").
        replace(Regex("(?<=[\\!\\?])\\s+"), "</s> <s>").
        replace(Regex("(?<=\\.)\\s+(?=[A-ZĚŠČŘŽÝÁÍÉÚŮŤĎŇ])"), "</s> <s>")
    }

    var basicId = 1
    //Nodes
    val _goBack = GoBack(basicId++, repeat = true)
    val _basicVersionGlobalIntent = GlobalIntent(basicId++, "basicVersionGlobalIntent", 0.99F, "verze")
    val _basicVersionResponse = Response(basicId++, false,{ "Verze serveru ${AppConfig.version}, prostředí ${AppConfig.instance.get("namespace", "unknown")}, dialog $dialogueName, verze dialogu $version" })

    val _basicVolumeUpGlobalIntent = GlobalIntent(basicId++, "basicVolumeUpGlobalIntent", 0.99F, "zvýšit hlasitost", "mluv hlasitěji")
    val _basicVolumeUpResponse = Response(basicId++, false,{ "#volume_up zvyšuji hlasitost" })

    val _basicVolumeDownGlobalIntent = GlobalIntent(basicId++, "basicVolumeDownGlobalIntent", 0.99F, "snížit hlasitost", "mluv tišeji")
    val _basicVolumeDownResponse = Response(basicId++, false,{ "#volume_down snižuji hlasitost" })

    val _basicLogApplicationErrorGlobalIntent = GlobalIntent(basicId++, "basicLogApplicationErrorGlobalIntent", 0.99F, "chyba aplikace", "problém aplikace")
    val _basicLogApplicationErrorResponse1 = Response(basicId++, {"O co jde?"})
    val _basicLogApplicationErrorResponse2 = Response(basicId++, false, {"Díky, pojďme zpátky."})
    val _basicLogApplicationErrorUserInput = UserInput(basicId++, arrayOf(), arrayOf()) {
        val transition = Transition(_basicLogApplicationErrorResponse2)
        dialogueEvent = DialogueEvent(this, this@BasicCzechDialogue, DialogueEvent.Type.UserError, input.transcript.text)
        transition
    }

    val _basicLogApplicationCommentGlobalIntent = GlobalIntent(basicId++, "basicLogApplicationCommentGlobalIntent", 0.99F, "komentář aplikace")
    val _basicLogApplicationCommentResponse1 = Response(basicId++, {"O co jde?"})
    val _basicLogApplicationCommentResponse2 = Response(basicId++, false, {"Díky, pojďme zpátky."})
    val _basicLogApplicationCommentUserInput = UserInput(basicId++, arrayOf(), arrayOf()) {
        val transition = Transition(_basicLogApplicationCommentResponse2)
        dialogueEvent = DialogueEvent(this, this@BasicCzechDialogue, DialogueEvent.Type.UserComment, input.transcript.text)
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