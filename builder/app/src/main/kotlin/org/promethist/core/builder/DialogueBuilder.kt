package org.promethist.core.builder

import org.promethist.common.AppConfig
import org.promethist.core.dialogue.AbstractDialogue
import org.promethist.core.model.DialogueSourceCode
import org.promethist.core.model.DialogueBuild
import org.promethist.core.resources.FileResource
import org.promethist.core.runtime.DialogueClassLoader
import org.promethist.core.runtime.Kotlin
import org.promethist.util.LoggerDelegate
import java.io.*
import java.net.URLClassLoader
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlin.jvm.internal.Reflection

class DialogueBuilder(
    val sourceOnly: Boolean = false,
    val workDir: File = File(System.getProperty("java.io.tmpdir"))) {

    @Inject
    lateinit var fileResource: FileResource

    @Inject
    lateinit var intentModelBuilder: IntentModelBuilder

    private val byteCodeClassLoader = DialogueClassLoader(this::class.java.classLoader)

    private val logger by LoggerDelegate()

    fun create(source: DialogueSourceCode): Builder = Builder(source)

    fun kotlinc(vararg args: String, lineCallback: ((String) -> Unit)): Int =
        ProcessBuilder("/usr/local/bin/kotlinc", *args).run {
            redirectErrorStream(true)
            val proc = start()
            val input = BufferedReader(InputStreamReader(proc.inputStream))
            while (true) {
                val line = input.readLine() ?: break
                lineCallback(line)
            }
            proc.waitFor()
        }

    interface Resource {
        val filename: String
        val stream: InputStream
    }

    inner class Builder(val source: DialogueSourceCode) {
        private val classFiles = mutableListOf<File>()
        private val resources: MutableList<Resource> = mutableListOf()
        private val basePath = "dialogue/${source.dialogueId}/"
        private val buildPath = "dialogue/${source.dialogueId}/builds/${source.buildId}/"
        private val manifest
            get() = """
            Manifest-Version: 1.0
            Package: model.${source.buildId}
            Created-By: promethist
            Name: model/${source.buildId}/
            Sealed: true
            Specification-Title: "Promethist Dialogue Model" 
            Specification-Version: "${AppConfig.instance.get("git.ref", "unknown")}"
            Specification-Vendor: "PromethistAI a.s.".
            Implementation-Title: "Dialogue Model ${source.dialogueId}" 
            Implementation-Version: "${source.version}"
            Implementation-Vendor: "(vendor)"
            """.trimIndent()
        private val scriptCode get() = "${source.code}\n//--export-class\n${source.className}::class\n"

        fun addResource(resource: Resource) = resources.add(resource)

        /**
         * Builds and stores dialogue model with intent model and included files using file resource.
         */
        fun build(oodExamples: List<DialogueSourceCode.GlobalIntent> = listOf()): DialogueBuild {
            val time = System.currentTimeMillis()
            try {
                logger.info("Start building dialogue model ${source.dialogueId}")
                saveSourceCode(buildPath)
                val dialogue = createInstance()
                saveResources(buildPath)
                saveJavaArchive(buildPath)
                buildIntentModels(dialogue, oodExamples)
                val duration = System.currentTimeMillis() - time
                logger.info("Finished build of dialogue model ${source.dialogueId} in $duration ms")
                return DialogueBuild(source.buildId, true, getLogs(), duration = duration)
            } catch (t: Throwable) {
                val fullError = getExceptionChain("", t)
                logger.error("Dialogue model build Exception:\n$fullError", t)
                return DialogueBuild(source.buildId, false, getLogs(), fullError)
            } finally {
                saveBuildLog(buildPath)
            }
        }

        private fun getExceptionChain(message: String, t: Throwable?): String {
            if (t == null) {
                return message
            }
            return getExceptionChain("$t\n$message", t.cause)
        }

        fun deploy() {
            saveSourceCode(basePath)
            saveResources(basePath)
            saveJavaArchive(basePath)
        }

        private fun getLogs(): List<String> {
            return BuildLogAppender.getEvents(Thread.currentThread().name)
        }

        private fun saveBuildLog(dir: String) {
            val logs = getLogs()
            BuildLogAppender.clearEvents(Thread.currentThread().name)
            val log = StringBuilder()

            logs.forEach {
                log.appendLine(it)
            }

            val path = dir + "build.log"
            fileResource.writeFile(path, "text/plain",
                listOf("version:${source.version}", "buildId:${source.buildId}"), log.toString().byteInputStream())
        }

        private fun createInstance(): AbstractDialogue {
            logger.info("Starting build of dialogue model ${source.dialogueId}")
            val dialogue = if (sourceOnly) {
                //todo remove args?
                Kotlin.newObjectWithArgs(Kotlin.loadClass(StringReader(scriptCode)), source.parameters)
            } else {
                compileByteCode()
                val time1 = System.currentTimeMillis()
                classFiles.forEach {
                    val className = "model.${source.buildId}.${it.nameWithoutExtension}"
//                    logger.info("loading dialogue model class $className from $it")
                    byteCodeClassLoader.loadClass(className, it.readBytes())
                }
                val time2 = System.currentTimeMillis()
                val javaClass = byteCodeClassLoader.loadClass("model.${source.buildId}.Model")
                //println(modelClass.isKotlinClass())
                val kotlinClass = Reflection.createKotlinClass(javaClass)
                //val dialogue = modelClass.getDeclaredConstructor().newInstance() as Dialogue
                val dialogue = Kotlin.newObjectWithArgs(kotlinClass, source.parameters) as AbstractDialogue
                logger.info("Dialogue model classes loaded in ${time2 - time1} ms, instantiated in ${System.currentTimeMillis() - time2} ms")
                dialogue
            }
            dialogue.validate()
            return dialogue
        }

        private fun compileByteCode() {
            val classLoader = javaClass.classLoader
            val classPath = if (classLoader is URLClassLoader) {
                classLoader.urLs.map { it.toString() }.joinToString(":")
            } else {
                System.getProperty("java.class.path")
            }
            val sourceFile = File(workDir, "${source.buildId}.kt")
            sourceFile.writeText(source.code)
            logger.info("Compiling dialogue model source file ${sourceFile.absolutePath} to $workDir")
            val buf = StringBuilder()
            val res = kotlinc(
                "-cp",
                classPath,
                "-d",
                workDir.absolutePath,
                "-jvm-target",
                "1.8",
                sourceFile.absolutePath
            ) { line ->
                if (!line.contains("warning: classpath entry points to a non-existent location")) // ignore this warning
                    buf.appendLine(line)
            }
            if (res != 0)
                error(buf)
            logger.debug("Kotlin compiler output:\n$buf")

            File(workDir, "model/${source.buildId}").apply {
                list { _, name -> name.endsWith(".class") }.forEach {
                    classFiles.add(File(this, it))
                }
            }
        }

        private fun saveSourceCode(dir: String) {
            val path = dir + "model.kts"
            logger.info("Saving dialogue model ${source.dialogueId} resource file $path")
            ByteArrayInputStream(scriptCode.toByteArray()).let {
                fileResource.writeFile(path, "text/kotlin",
                    listOf("version:${source.version}", "buildId:${source.buildId}"), it)
            }
        }

        private fun saveJavaArchive(dir: String, jar: OutputStream? = null) {
            val buf = ByteArrayOutputStream()
            val zip = ZipOutputStream(buf)
            classFiles.forEach { classFile ->
                val path = "model/${source.buildId}/${classFile.name}"
                zip.putNextEntry(ZipEntry(path))
                classFile.inputStream().use { it.copyTo(zip) }
            }
            zip.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
            manifest.byteInputStream().copyTo(zip)
            zip.closeEntry()
            zip.close()
            val path = dir + "model.jar"
            logger.info("Saving dialogue model ${source.dialogueName} resource file $path")
            ByteArrayInputStream(buf.toByteArray()).let {
                jar?.use { out -> it.copyTo(out) } ?:
                fileResource.writeFile(path, "application/java-archive",
                    listOf("version:${source.version}", "buildId:${source.buildId}"), it)
            }
        }

        private fun saveResources(dir: String) {
            resources.forEach {
                it.stream.reset()
                val path = "${dir}resources/${it.filename}"
                logger.info("Saving dialogue model ${source.dialogueName} resource file ${it.filename}")
                fileResource.writeFile(path, "text/json", listOf(), it.stream)
            }
        }

        private fun buildIntentModels(dialogue: AbstractDialogue, oodExamples: List<DialogueSourceCode.GlobalIntent>) {
            logger.info("Building intent models for dialogue model ${source.dialogueId}")
            val irModels = mutableListOf<IntentModel>()
            val language = Locale(dialogue.language)

            dialogue.globalIntents.apply/*ifNotEmpty*/ {
                val irModel = IntentModel(source.buildId, source.dialogueId, null)
                irModels.add(irModel)
                intentModelBuilder.build(irModel, language, this, oodExamples)
            }

            dialogue.userInputs.forEach {
                val irModel = IntentModel(source.buildId, source.dialogueId, it.id)
                irModels.add(irModel)
                intentModelBuilder.build(irModel, language, it.intents.asList(), (oodExamples + dialogue.globalIntents.map {
                    DialogueSourceCode.GlobalIntent(it.id, it.name, it.threshold, it.utterances.toList(), it.entities)
                } ))
            }
            logger.info("Built ${irModels.size} intent models")
        }
    }
}