package com.promethist.core.builder

import com.promethist.core.dialogue.Dialogue
import com.promethist.core.resources.FileResource
import com.promethist.core.runtime.DialogueClassLoader
import com.promethist.core.runtime.Kotlin
import com.promethist.util.LoggerDelegate
import org.jetbrains.kotlin.daemon.common.toHexString
import java.io.*
import java.net.URLClassLoader
import java.security.MessageDigest
import java.util.*
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

    private val version = "undefined"

    fun create(name: String): Builder = Builder(name)

    interface Resource {
        val filename: String
        val stream: InputStream
    }

    inner class Builder(val name: String) {

        val buildId = "id" + md5(random.nextLong().toString())
        val source = DialogueSourceCodeBuilder(name, buildId)
        val classFiles = mutableListOf<File>()
        val resources: MutableList<Resource> = mutableListOf()
        val basePath = "dialogue/${name}/"
        val propertiesAsString get() = """
                buildId=${buildId}
                classes=${classFiles.joinToString(",") { it.nameWithoutExtension }}
            """.trimIndent()

        fun addResource(resource: Resource) = resources.add(resource)

        fun build(): Dialogue {
            logger.info("start building dialogue model $name")
            source.build()
            val dialogue = if (sourceOnly) {
                //todo remove args?
                Kotlin.newObjectWithArgs(Kotlin.loadClass(StringReader(source.scriptCode)), source.parameters)
            } else {
                compileByteCode()
                val time1 = System.currentTimeMillis()
                classFiles.forEach {
                    val className = "model.$buildId.${it.nameWithoutExtension}"
                    logger.info("loading dialogue model class $className from $it")
                    byteCodeClassLoader.loadClass(className, it.readBytes())
                }
                val time2 = System.currentTimeMillis()
                val modelJavaClass = byteCodeClassLoader.loadClass("model.$buildId.Model")
                //println(modelClass.isKotlinClass())
                val modelKotlinClass = Reflection.createKotlinClass(modelJavaClass)
                //val dialogue = modelClass.getDeclaredConstructor().newInstance() as Dialogue
                val dialogue = Kotlin.newObjectWithArgs(modelKotlinClass, source.parameters) as Dialogue
                logger.info("dialogue model classes loaded in ${time2 - time1} ms, instantiated in ${System.currentTimeMillis() - time2} ms")
                dialogue
            }
            validate(dialogue)
            return dialogue
        }
        /**
         * Builds and stores dialogue model with intent model and included files using file resource.
         */
        fun buildAndSave() {
            val dialogue = build()
            logger.info("start saving dialogue model $name")
            saveSourceCode()
            saveResources()
            saveClassesWithProperties()
            buildIntentModels(dialogue)
            logger.info("finished saving dialogue model $name")
        }

        fun validate(dialogue: Dialogue) {
            dialogue.validate()
        }

        fun compileByteCode() {
            val classLoader = javaClass.classLoader
            val classPath = if (classLoader is URLClassLoader) {
                classLoader.urLs.map { it.toString() }.joinToString(":")
            } else {
                System.getProperty("java.class.path")
            }
            val sourceFile = File(workDir, "${source.buildId}.kt")
            sourceFile.writeText(source.code)
            logger.info("compiling source file ${sourceFile.absolutePath} to $workDir")
            logger.info("classPath = $classPath")
            ProcessBuilder(
                    "/usr/local/bin/kotlinc",
                    "-cp", classPath,
                    "-d", workDir.absolutePath,
                    sourceFile.absolutePath
            ).apply {
                redirectErrorStream(true)
                val buf = StringBuilder()
                val proc = start()
                val input = BufferedReader(InputStreamReader(proc.inputStream))
                while (true) {
                    val line = input.readLine()
                    if (line == null)
                        break
                    else
                        buf.appendln(line)
                }
                if (proc.waitFor() != 0)
                    error(buf)
                File(workDir, "model/$buildId").apply {
                    list { _, name -> name.endsWith(".class") }.forEach {
                        classFiles.add(File(this, it))
                    }
                }
            }
        }

        fun saveSourceCode() {
            val path = basePath + "model.kts"
            logger.info("saving dialogue model script $name to file resource $path")
            val stream = ByteArrayInputStream(source.scriptCode.toByteArray())
            fileResource.writeFile(path, "text/kotlin", listOf("version:$version", "buildId:$buildId"), stream)
        }

        fun saveClassesWithProperties() {
            classFiles.forEach { classFile ->
                val path = basePath + "model/${classFile.name}"
                logger.info("saving dialogue model class $classFile to file resource $path")
                classFile.inputStream().use { input ->
                    fileResource.writeFile(path, "application/octet-stream", listOf("version:$version"), input)
                }
            }
            fileResource.writeFile(basePath + "model.properties", "text/plain", listOf("version:$version"), propertiesAsString.byteInputStream())
        }

        fun saveResources() {
            resources.forEach {
                val path = "${basePath}resources/${it.filename}"
                logger.info("saving resource file ${it.filename}")
                fileResource.writeFile(path, "text/json", listOf(), it.stream)
            }
        }

        fun buildIntentModels(dialogue: Dialogue) {
            logger.info("building intent models for dialogue model $name")
            val irModels = mutableListOf<IrModel>()
            val language = Locale(dialogue.language)

            dialogue.globalIntents.apply/*ifNotEmpty*/ {
                val irModel = IrModel(buildId, name, null)
                irModels.add(irModel)
                intentModelBuilder.build(irModel, language, this)
            }

            dialogue.userInputs.forEach {
                val irModel = IrModel(buildId, name, it.id)
                irModels.add(irModel)
                intentModelBuilder.build(irModel, language, it.intents.asList())
            }
            logger.info("built intent models: $irModels")
        }
    }

    companion object {
        private val random = Random()
        private val md = MessageDigest.getInstance("MD5")

        fun md5(str: String): String = md.digest(str.toByteArray()).toHexString()
    }
}