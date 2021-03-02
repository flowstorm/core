package ai.flowstorm.core.runtime

import ai.flowstorm.core.dialogue.AbstractDialogue
import ai.flowstorm.core.type.Throwables.root

class DialogueException(node: AbstractDialogue.Node, cause: Throwable) :
    Throwable("Dialogue failed at ${node.dialogue.dialogueName}:${node.dialogue.version}#${node.id} because of ${cause.root.message}", cause)
