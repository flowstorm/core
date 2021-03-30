package ai.flowstorm.core.builder

import ai.flowstorm.core.model.Model
import ai.flowstorm.security.Digest

data class IntentModel(val buildId: String, val dialogueId: String, val nodeId: Int?) : Model {
    override val name: String = dialogueId + if (nodeId != null) "#${nodeId}" else ""
    override val id: String = Digest.md5(buildId + dialogueId + nodeId)

    constructor(buildId: String, dialogueId: String) : this(buildId, dialogueId, null)

    override fun toString(): String = "IntentModel(name='$name', id='$id')"

}