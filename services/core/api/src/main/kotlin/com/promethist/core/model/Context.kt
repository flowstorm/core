package com.promethist.core.model

import com.promethist.core.type.Dynamic
import java.util.*

data class Context(
        var input: String,
        var attributes: Dynamic = Dynamic(),
        val dialogueStack: LinkedList<DialogueStackFrame> = LinkedList(),
        val responseItems: MutableList<MessageItem> = mutableListOf()
) {
    //lateinit var session: Session

    data class DialogueStackFrame(val name: String, var nodeId: Int = 0)
}
