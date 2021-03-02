package ai.flowstorm.core.runtime

import ai.flowstorm.core.Context
import ai.flowstorm.core.dialogue.AbstractDialogue.Node
import ai.flowstorm.core.dialogue.AbstractDialogue.Run
import ai.flowstorm.util.LoggerDelegate

object DialogueRuntime {

    private val logger by LoggerDelegate()
    private val threadRun = ThreadLocal<Run>()

    val isRunning get() = (threadRun.get() != null)

    val run get() = threadRun.get() ?: error("Dialogue is not running")

    fun ifRunning(block: Run.() -> Unit) {
        if (isRunning)
            block(run)
    }

    fun run(context: Context, node: Node, block: () -> Any?): Any? {
        logger.info("Running ${node.javaClass.simpleName} ${node.dialogue.dialogueName}#${node.id}")
        val securityManager = System.getSecurityManager() as DialogueSecurityManager
        try {
            securityManager.enable()
            threadRun.set(Run(node, context))
            return block()
        } catch (e: Throwable) {
            throw DialogueException(node, e)
        } finally {
            securityManager.disable()
            threadRun.remove()
        }
    }
}