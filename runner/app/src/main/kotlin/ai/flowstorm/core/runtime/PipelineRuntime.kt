package ai.flowstorm.core.runtime

import ai.flowstorm.common.monitoring.Monitor
import ai.flowstorm.core.*
import ai.flowstorm.core.dialogue.AbstractDialogue.Companion.defaultNamespace
import ai.flowstorm.core.model.Session
import ai.flowstorm.core.model.Voice
import ai.flowstorm.core.model.metrics.Metric
import ai.flowstorm.core.type.Memory
import javax.inject.Inject

class PipelineRuntime {

    @Inject
    lateinit var pipelineFactory: PipelineFactory

    @Inject
    lateinit var contextFactory: ContextFactory

    @Inject
    lateinit var contextPersister: ContextPersister

    @Inject
    lateinit var monitor: Monitor

    fun process(session: Session, request: Request, contextLog: ContextLog): Response {

        updateAutomaticMetrics(session)

        val pipeline = pipelineFactory.createPipeline()
        var context = contextFactory.createContext(pipeline, session, request, contextLog)

        try {
            context = call { pipeline.process(context) }
        } catch (e: Throwable) {
            context.createDialogueEvent(e)
            monitor.capture(e)
            throw e
        } finally {
            contextPersister.persist(context)
        }

        return with(context) {
            // client attributes
            listOf("speakingRate", "speakingPitch", "speakingVolumeGain").forEach {
                if (!turn.attributes[defaultNamespace].containsKey(it)) {
                    val value = session.attributes[defaultNamespace][it]
                        ?: userProfile.attributes[defaultNamespace][it]
                    if (value != null)
                        turn.attributes[defaultNamespace][it] = value
                }
            }
            turn.responseItems.forEach {
                it.ttsConfig = it.ttsConfig ?: Voice.forLanguage(locale?.language ?: "en").config
            }
            Response(locale, turn.responseItems, contextLog.log,
                turn.attributes[defaultNamespace].map { it.key to (it.value as Memory<*>).value }.toMap().toMutableMap(),
                turn.sttMode, turn.expectedPhrases, sessionEnded, sleepTimeout)
        }
    }

    private fun <T> call(timeout: Int = 5000, block: (() -> T)): T {
        var error: Throwable? = null
        var result: T? = null
        val thread = Thread {
            try {
                result = block()
            } catch (e: Throwable) {
                error = e
            }
        }
        thread.start()
        var i = timeout / 100
        while (i > -10 && result == null && error == null) {
            Thread.sleep(100)
            i--
            if (i == 0)
                thread.interrupt() // try to interrupt first, if thread is stuck, we will stop it hard after one more second
        }
        return when {
            error is InterruptedException -> error("Thread interrupted after timeout $timeout milliseconds")
            error != null -> throw error!!
            result == null -> {
                thread.stop()
                error("Thread stopped after timeout $timeout milliseconds")
            }
            else -> result!!
        }
    }

    private fun updateAutomaticMetrics(session: Session) {
        with(session.metrics) {
            find { it.name == "count" && it.namespace == "session" }
                ?: add(Metric("session", "count", 1))
            find { it.name == "turns" && it.namespace == "session" }?.increment()
                ?: add(Metric("session", "turns", 1))
        }
    }
}