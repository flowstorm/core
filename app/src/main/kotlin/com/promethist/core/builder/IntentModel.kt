package com.promethist.core.builder

import com.promethist.core.model.Model
import org.jetbrains.kotlin.daemon.common.toHexString
import java.security.MessageDigest

data class IntentModel(val buildId: String, val dialogueId: String, val nodeId: Int?) : Model {
    override val name: String = dialogueId + if (nodeId != null) "#${nodeId}" else ""
    override val id: String = md5(buildId + dialogueId + nodeId)

    constructor(buildId: String, dialogueId: String) : this(buildId, dialogueId, null)

    override fun toString(): String = "IntentModel(name='$name', id='$id')"

    companion object {
        private val md = MessageDigest.getInstance("MD5")
        private fun md5(str: String): String = md.digest(str.toByteArray()).toHexString()
    }
}