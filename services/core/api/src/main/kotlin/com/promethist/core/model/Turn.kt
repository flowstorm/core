package com.promethist.core.model

import com.promethist.core.Input
import com.promethist.core.Response
import com.promethist.core.type.Dynamic
import java.util.*

data class Turn(
        var input: Input,
        val datetime: Date = Date(),
        var attributes: Dynamic = Dynamic(),
        val dialogueStack: LinkedList<DialogueStackFrame> = LinkedList(),
        val responseItems: MutableList<Response.Item> = mutableListOf()
) {
    data class DialogueStackFrame(val name: String, var nodeId: Int = 0, var skipGlobalIntents: Boolean = false)
}
