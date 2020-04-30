package com.promethist.core.builder

import com.promethist.core.model.IrModel
import org.jetbrains.kotlin.daemon.common.toHexString
import java.security.MessageDigest

data class IrModel(val buildId: String, val dialogueName: String, val nodeId: Int?) : IrModel {
    override val name: String = dialogueName + if (nodeId != null) "#${nodeId}" else ""
    override val id: String = md5(buildId + dialogueName + nodeId)

    constructor(buildId: String, dialogueName: String) : this(buildId, dialogueName, null)

    companion object {
        private val md = MessageDigest.getInstance("MD5")
        private fun md5(str: String): String = md.digest(str.toByteArray()).toHexString()
    }
}