package org.promethist.core.model

import org.promethist.security.Digest

data class IntentModel(val buildId: String, val dialogueId: String, val nodeId: Int?) : Model {
    override val name: String = dialogueId + if (nodeId != null) "#${nodeId}" else ""
    override val id: String = Digest.md5((buildId + dialogueId + nodeId).toByteArray())

    constructor(buildId: String, dialogueId: String) : this(buildId, dialogueId, null)

    override fun toString(): String = "IntentModel(name='$name', id='$id')"
}