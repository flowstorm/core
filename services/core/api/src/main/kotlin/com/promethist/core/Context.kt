package com.promethist.core

import com.promethist.core.model.IrModel
import com.promethist.core.model.Profile
import com.promethist.core.model.Turn
import com.promethist.core.model.Session
import com.promethist.core.model.metrics.Metrics
import com.promethist.core.resources.CommunityResource
import org.slf4j.Logger

data class Context(
        var pipeline: Pipeline,
        val profile: Profile,
        val session: Session,
        val turn: Turn,
        val metrics: Metrics,
        val logger: Logger,
        val communityResource: CommunityResource,
        var irModels: List<IrModel> = listOf()
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