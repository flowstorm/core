package ai.flowstorm.core.runtime

import ai.flowstorm.util.LoggerDelegate
import java.io.FileDescriptor
import java.net.InetAddress
import java.security.Permission

class DialogueSecurityManager(private val raiseExceptions: Boolean) : SecurityManager() {

    companion object {

        val importedPackages = listOf(
            "ai.flowstorm.core",
            "ai.flowstorm.core.type",
            "ai.flowstorm.core.type.value",
            "ai.flowstorm.core.model",
            "ai.flowstorm.core.dialogue",
            "ai.flowstorm.core.runtime"
        )
        private val allowedPackages = (listOf(
            "model\\..*",
            "kotlin",
            "kotlin\\..*",
            "java.lang",
            "java.util",
            "java.time",                        // required by date time operations
            "org.slf4j",                        // required by contextual logging
            "com.fasterxml.jackson.core.type"   // required by TypeReference in inlined code
        ) + importedPackages)
            .joinToString("|")

        private val logger by LoggerDelegate()
    }

    init {
        logger.info("Creating security manager (raiseExceptions=$raiseExceptions)")
    }

    private val active = ThreadLocal.withInitial { false }
    private val checkingStackTrace = ThreadLocal.withInitial { false }

    internal fun enable() {
        active.set(true)
    }

    internal fun disable() {
        active.set(false)
    }
    
    private fun issue(text: String, warn: Boolean = true) {
        if (raiseExceptions)
            throw SecurityException(text)
        else if (warn) {
            val s = "Security $text"
            logger.warn(s)
            DialogueRuntime.run.context.logger.warn(s)
        }
    }

    /**
     * This method will check current thread stack whether model method is executed and if so, whether is is invoking
     * code OR loading class belonging to allowed packages
     */
    private fun checkStackTrace(pkg: String? = null) {
        checkingStackTrace.set(true)
        try {
            val stackTrace = Thread.currentThread().stackTrace
            val i = stackTrace.indices.firstOrNull { i: Int -> stackTrace[i].className.startsWith("model.") }
            if (i != null) {
                val m = stackTrace[i]
                val p = stackTrace[i - 1]
                if (p.className == "java.lang.ClassLoader") {
                    if (pkg != null && !pkg.matches(Regex("($allowedPackages)")))
                        issue("denied package $pkg access for ${m.className}.${m.methodName}:${m.lineNumber}")
                } else if (!p.className.matches(Regex("($allowedPackages).*"))) {
                    issue("denied class ${p.className} access for ${m.className}.${m.methodName}:${m.lineNumber}")
                }
            }
        } finally {
            checkingStackTrace.set(false)
        }
    }

    override fun checkPackageAccess(pkg: String) {
        if (active.get() && !checkingStackTrace.get())
            checkStackTrace(pkg)
    }

    override fun checkPermission(perm: Permission, context: Any?) {
        if (active.get()) {
            logger.debug("Checking permission $perm" + (context?.let { " with context $context" } ?: ""))
            if (perm is RuntimePermission) {
                if (perm.name == "setSecurityManager")
                    issue("denied permission ${perm.name}")
            }
        }
    }

    override fun checkPermission(perm: Permission) = checkPermission(perm, null)

    override fun checkCreateClassLoader() {
        if (active.get()) {
            logger.info("Checking createClassLoader")
            checkStackTrace()
        }
    }

    override fun checkAccess(t: Thread?) {
        if (active.get()) {
            logger.info("Checking access $t")
            checkStackTrace()
        }
    }

    override fun checkAccess(g: ThreadGroup?) {
        if (active.get()) {
            logger.info("Checking access $g")
            checkStackTrace()
        }
    }

    override fun checkLink(lib: String?) {
        if (active.get())
            issue("denied link $lib")
    }

    override fun checkSecurityAccess(target: String?) {
        if (active.get())
            issue("denied securityAccess $target")
    }

    override fun checkPackageDefinition(pkg: String?) {
        if (active.get())
            issue("packageDefinition $pkg")
    }

    override fun checkListen(port: Int) {
        if (active.get())
            issue("denied listen $port")
    }

    override fun checkAccept(host: String?, port: Int) {
        if (active.get())
            issue("accept $host:$port")
    }

    override fun checkConnect(host: String?, port: Int, context: Any?) {
        if (active.get()) {
            val text = "connect $host:$port" + (context?.let { " with context $context" } ?: "")
            if (!listOf(-1, 80, 443).contains(port))
                issue(text)
            else
                logger.info("Checking connect $host:$port" + (context?.let { " with context $context" } ?: ""))
        }
    }

    override fun checkConnect(host: String?, port: Int) = checkConnect(host, port, null)

    override fun checkMulticast(maddr: InetAddress?) {
        if (active.get())
            issue("multicast $maddr")
    }

    override fun checkSetFactory() {
        if (active.get()) {
            logger.info("Checking setFactory")
            checkStackTrace()
        }
    }

    override fun checkRead(fd: FileDescriptor?) {
        if (active.get())
            logger.debug("Checking read $fd")
    }

    override fun checkRead(file: String?, context: Any?) {
        if (active.get())
            logger.debug("Checking read $file" + (context?.let { " with context $context" } ?: ""))
    }

    override fun checkRead(file: String?) = checkRead(file, null)

    override fun checkWrite(fd: FileDescriptor?) {
        if (active.get())
            logger.info("Checking write $fd")
    }

    override fun checkWrite(file: String?) {
        if (active.get())
            issue("defined write $file")
    }

    override fun checkDelete(file: String?) {
        if (active.get())
            issue("denied delete $file")
    }

    override fun checkPrintJobAccess() {
        if (active.get())
            issue("denied printJobAccess")
    }

    override fun checkPropertiesAccess() {
        if (active.get())
            issue("denied propertiesAccess", false)
    }

    override fun checkPropertyAccess(key: String?) {
        if (active.get())
            logger.debug("Checking propertyAccess $key")
    }

    override fun checkExec(cmd: String?) {
        if (active.get())
            issue("denied exec $cmd")
    }

    override fun checkExit(status: Int) {
        if (active.get())
            throw SecurityException("denied exit with $status")
    }
}