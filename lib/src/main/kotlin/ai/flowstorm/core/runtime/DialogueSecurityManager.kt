package ai.flowstorm.core.runtime

import ai.flowstorm.util.LoggerDelegate
import java.io.FileDescriptor
import java.net.InetAddress
import java.security.Permission

class DialogueSecurityManager : SecurityManager() {

    companion object {

        private val allowedPackages = listOf(
            "model\\..*",
            "kotlin",
            "kotlin\\..*",
            "java.lang",
            "java.util",
            "java.time",                        // required by date time operations
            "ai.flowstorm.core\\..*",
            "org.slf4j",                        // required by contextual logging
            "javax.ws.rs.client",               // required by API calls
            "org.glassfish.jersey.client",      // required by API calls
            "com.fasterxml.jackson.core.type"   // required by TypeReference in inlined code
        ).joinToString("|")

        private val logger by LoggerDelegate()
    }

    private val active = ThreadLocal.withInitial { false }
    private val checkingStackTrace = ThreadLocal.withInitial { false }

    internal fun enable() {
        active.set(true)
    }

    internal fun disable() {
        active.set(false)
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
                        throw SecurityException("denied package $pkg access for ${m.className}.${m.methodName}:${m.lineNumber}")
                } else if (!p.className.matches(Regex("($allowedPackages).*"))) {
                    throw SecurityException("denied class ${p.className} access for ${m.className}.${m.methodName}:${m.lineNumber}")
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
                if (perm.name == "getClassLoader" || perm.name == "setSecurityManager")
                    SecurityException("denied permission ${perm.name}")
            }
        }
    }

    override fun checkPermission(perm: Permission) = checkPermission(perm, null)

    override fun checkCreateClassLoader() {
        if (active.get())
            logger.info("Checking createClassLoader")
    }

    override fun checkAccess(t: Thread?) {
        if (active.get())
            logger.info("Checking access $t")
    }

    override fun checkAccess(g: ThreadGroup?) {
        if (active.get())
            logger.info("Checking access $g")
    }

    override fun checkLink(lib: String?) {
        if (active.get())
            throw SecurityException("denied link $lib")
    }

    override fun checkSecurityAccess(target: String?) {
        if (active.get())
            throw SecurityException("denied securityAccess $target")
    }

    override fun checkPackageDefinition(pkg: String?) {
        if (active.get())
            throw SecurityException("packageDefinition $pkg")
    }

    override fun checkListen(port: Int) {
        if (active.get())
            throw SecurityException("denied listen $port")
    }

    override fun checkAccept(host: String?, port: Int) {
        if (active.get())
            logger.info("Checking accept $host:$port")
    }

    override fun checkConnect(host: String?, port: Int, context: Any?) {
        if (active.get())
        logger.info("Checking connect $host:$port" + (context?.let { " with context $context" } ?: ""))
    }

    override fun checkConnect(host: String?, port: Int) = checkConnect(host, port, null)

    override fun checkMulticast(maddr: InetAddress?) {
        if (active.get())
            throw SecurityException("multicast $maddr")
    }

    override fun checkSetFactory() {
        if (active.get())
            logger.info("Checking setFactory")
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
            throw SecurityException("defined write $file")
    }

    override fun checkDelete(file: String?) {
        if (active.get())
            throw SecurityException("denied delete $file")
    }

    override fun checkPrintJobAccess() {
        if (active.get())
            throw SecurityException("denied printJobAccess")
    }

    override fun checkPropertiesAccess() {
        if (active.get())
            throw SecurityException("denied propertiesAccess")
    }

    override fun checkPropertyAccess(key: String?) {
        if (active.get())
            logger.debug("Checking propertyAccess $key")
    }

    override fun checkExec(cmd: String?) {
        if (active.get())
            throw SecurityException("denied exec $cmd")
    }

    override fun checkExit(status: Int) {
        if (active.get())
            throw SecurityException("denied exit with $status")
    }
}