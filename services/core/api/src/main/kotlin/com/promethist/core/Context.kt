package com.promethist.core

import com.promethist.core.model.*
import com.promethist.core.resources.CommunityResource
import org.slf4j.Logger
import java.util.*

data class Context(
        var pipeline: Pipeline,
        val userProfile: Profile,
        val session: Session,
        val turn: Turn,
        val logger: Logger,
        var locale: Locale? = null,
        val communityResource: CommunityResource,
        val communities: MutableMap<String, Community> = mutableMapOf(),
        var irModels: List<IrModel> = listOf(),
        var dialogueEvent: DialogueEvent? = null
) {
    // aliases
    val input get() = turn.input
    val user get() = session.user
    val attributes get() = turn.attributes
    val application get() = session.application
    val previousTurns get() = session.turns.reversed()
    val sessionEnded get() = session.dialogueStack.isEmpty()
    fun processPipeline() = pipeline.process(this)

    // other properties
    val expectedPhrases: MutableList<ExpectedPhrase> = mutableListOf()
}