package com.promethist.core.model

import com.promethist.core.nlp.Input
import com.promethist.core.type.Dynamic
import java.util.*

data class Turn(
        var input: Input,
        var attributes: Dynamic = Dynamic(),
        val dialogueStack: LinkedList<DialogueStackFrame> = LinkedList(),
        val responseItems: MutableList<MessageItem> = mutableListOf()
) {
    data class DialogueStackFrame(val name: String, var nodeId: Int = 0)
}
