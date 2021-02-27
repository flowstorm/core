package ai.flowstorm.core.dialogue

import ai.flowstorm.common.AppConfig
import ai.flowstorm.core.model.DialogueEvent

abstract class BasicCzechDialogue : BasicDialogue() {

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

    //Nodes
    val _goBack = GoBack(_basicId++, repeat = true)
    val _basicVersionGlobalIntent = GlobalIntent(_basicId++, "basicVersionGlobalIntent", 0.99F, "verze aplikace")
    val _basicVersionResponse = Response(_basicId++, false, { "Verze serveru ${AppConfig.version}, prostředí ${AppConfig.instance.get("namespace", "unknown")}, dialogový model $dialogueName verze $version" })

    val _basicVolumeUpGlobalIntent = GlobalIntent(_basicId++, "basicVolumeUpGlobalIntent", 0.99F, "zvýšit hlasitost", "mluv hlasitěji")
    val _basicVolumeUpCommand = Command(_basicId++,  "volume", "up")
    val _basicVolumeUpResponse = Response(_basicId++, false, { "zvyšuji hlasitost" })

    val _basicVolumeDownGlobalIntent = GlobalIntent(_basicId++, "basicVolumeDownGlobalIntent", 0.99F, "snížit hlasitost", "mluv tišeji")
    val _basicVolumeDownCommand = Command(_basicId++,  "volume", "down")
    val _basicVolumeDownResponse = Response(_basicId++, false, { "snižuji hlasitost" })

    val _basicLogApplicationErrorGlobalIntent = GlobalIntent(_basicId++, "basicLogApplicationErrorGlobalIntent", 0.99F, "chyba aplikace", "problém aplikace")
    val _basicLogApplicationErrorResponse1 = Response(_basicId++, { "O co jde?" })
    val _basicLogApplicationErrorResponse2 = Response(_basicId++, false, { "Díky, pojďme zpátky." })
    val _basicLogApplicationErrorUserInput = UserInput(_basicId++, arrayOf(), arrayOf()) {
        val transition = Transition(_basicLogApplicationErrorResponse2)
        dialogueEvent = DialogueEvent(this, this@BasicCzechDialogue, DialogueEvent.Type.UserError, input.alternatives[0].text)
        transition
    }

    val _basicLogApplicationCommentGlobalIntent = GlobalIntent(_basicId++, "basicLogApplicationCommentGlobalIntent", 0.99F, "komentář aplikace")
    val _basicLogApplicationCommentResponse1 = Response(_basicId++, { "O co jde?" })
    val _basicLogApplicationCommentResponse2 = Response(_basicId++, false, { "Díky, pojďme zpátky." })
    val _basicLogApplicationCommentUserInput = UserInput(_basicId++, arrayOf(), arrayOf()) {
        val transition = Transition(_basicLogApplicationCommentResponse2)
        dialogueEvent = DialogueEvent(this, this@BasicCzechDialogue, DialogueEvent.Type.UserComment, input.alternatives[0].text)
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