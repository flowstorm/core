package com.promethist.core.builder

import com.promethist.common.AppConfig
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

    private val version = "undefined"

    fun create(name: String): Builder = Builder(name)

    interface Resource {
        val filename: String
        val stream: InputStream
    }

    inner class Builder(val name: String) {

        val buildId = "id" + md5(random.nextLong().toString())
        val source = SourceCodeBuilder(name, buildId)
        val classFiles = mutableListOf<File>()
        val resources: MutableList<Resource> = mutableListOf()
        val basePath = "dialogue/$name/"
        val buildPath = "dialogue/$name/builds/$buildId/"
        val manifest
            get() = """
            Manifest-Version: 1.0
            Package: model.$buildId
            Created-By: promethist
            Name: model/$buildId/
            Sealed: true
            Specification-Title: "Promethist Dialogue Model" 
            Specification-Version: "${AppConfig.instance.get("git.ref", "unknown")}"
            Specification-Vendor: "PromethistAI a.s.".
            Implementation-Title: "Dialogue Model $name" 
            Implementation-Version: "$version"
            Implementation-Vendor: "(vendor)"
            """.trimIndent()

        fun addResource(resource: Resource) = resources.add(resource)

        /**
         * Builds and stores dialogue model with intent model and included files using file resource.
         */
        fun build() {
            try {
                logger.info("start building dialogue model $name")
                source.build()
                saveSourceCode(buildPath)
                val dialogue = createInstance()
                saveResources(buildPath)
                saveJavaArchive(buildPath)
                buildIntentModels(dialogue)
                logger.info("finished building dialogue model $name")
            } finally {
                saveBuildLog(buildPath)
            }
        }

        fun deploy() {
            saveSourceCode(basePath)
            saveResources(basePath)
            saveJavaArchive(basePath)
        }

        private fun saveBuildLog(dir: String) {
            val threadName = Thread.currentThread().name
            val logs = BuildLogAppender.getEvents(threadName)
            BuildLogAppender.clearEvents(threadName)
            val log = StringBuilder()

            logs.forEach {
                log.appendln(it)
            }

            val path = dir + "build.log"
            fileResource.writeFile(path, "text/plain",
                    listOf("version:$version", "buildId:$buildId"), log.toString().byteInputStream())
        }

        private fun createInstance(): Dialogue {
            logger.info("start building dialogue model $name")
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
                val javaClass = byteCodeClassLoader.loadClass("model.$buildId.Model")
                //println(modelClass.isKotlinClass())
                val kotlinClass = Reflection.createKotlinClass(javaClass)
                //val dialogue = modelClass.getDeclaredConstructor().newInstance() as Dialogue
                val dialogue = Kotlin.newObjectWithArgs(kotlinClass, source.parameters) as Dialogue
                logger.info("dialogue model classes loaded in ${time2 - time1} ms, instantiated in ${System.currentTimeMillis() - time2} ms")
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
            logger.info("compiling source file ${sourceFile.absolutePath} to $workDir")
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
                logger.debug("Kotlin compiler output:\n$buf")
                File(workDir, "model/$buildId").apply {
                    list { _, name -> name.endsWith(".class") }.forEach {
                        classFiles.add(File(this, it))
                    }
                }
            }
        }

        private fun saveSourceCode(dir: String) {
            val path = dir + "model.kts"
            logger.info("saving dialogue model file $name to resource $path")
            ByteArrayInputStream(source.scriptCode.toByteArray()).let {
                fileResource.writeFile(path, "text/kotlin",
                        listOf("version:$version", "buildId:$buildId"), it)
            }
        }

        private fun saveJavaArchive(dir: String, jar: OutputStream? = null) {
            val buf = ByteArrayOutputStream()
            val zip = ZipOutputStream(buf)
            classFiles.forEach { classFile ->
                val path = "model/$buildId/${classFile.name}"
                logger.info("saving dialogue model class $classFile to JAR resource $path")
                zip.putNextEntry(ZipEntry(path))
                classFile.inputStream().use { it.copyTo(zip) }
            }
            zip.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
            manifest.byteInputStream().copyTo(zip)
            zip.closeEntry()
            zip.close()
            val path = dir + "model.jar"
            logger.info("saving dialogue model file $name to resource $path")
            ByteArrayInputStream(buf.toByteArray()).let {
                jar?.use { out -> it.copyTo(out) } ?:
                    fileResource.writeFile(path, "application/java-archive",
                            listOf("version:$version", "buildId:$buildId"), it)
            }
        }

        private fun saveResources(dir: String) {
            resources.forEach {
                it.stream.reset()
                val path = "${dir}resources/${it.filename}"
                logger.info("saving resource file ${it.filename}")
                fileResource.writeFile(path, "text/json", listOf(), it.stream)
            }
        }

        private fun buildIntentModels(dialogue: Dialogue) {
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

        @JvmStatic
        fun main(args: Array<String>) {
            val classLoader = URLClassLoader(arrayOf(File("/Users/tomas.zajicek/Downloads/model.jar").toURI().toURL()), this::class.java.classLoader)
            val className = "model.id5a5282249eb7f3297764357710a94559.Model"
            //val javaClass = Class.forName(className, true, classLoader)
            val javaClass = classLoader.loadClass(className)
            val kotlinClass = Reflection.createKotlinClass(javaClass)
            //val dialogue = modelClass.getDeclaredConstructor().newInstance() as Dialogue
            val dialogue = Kotlin.newObjectWithArgs(kotlinClass, mapOf("str" to "bla", "num" to 123, "chk" to true)) as Dialogue
            println(dialogue.nodes)
        }
    }
}