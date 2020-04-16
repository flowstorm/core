package com.promethist.core.builder

import com.promethist.core.model.IrModel
import org.jetbrains.kotlin.daemon.common.toHexString
import java.security.MessageDigest

data class IrModel(override val name: String, override val id: String) : IrModel {
    constructor(buildId: String, dialogueName: String, nodeId: Int?) : this(
            dialogueName + if (nodeId != null) "#${nodeId}" else "",
            md5(buildId + dialogueName + nodeId)
    )

    companion object {
        private val md = MessageDigest.getInstance("MD5")
        private fun md5(str: String): String = md.digest(str.toByteArray()).toHexString()
    }
}