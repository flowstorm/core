package ai.flowstorm.common

import java.io.BufferedReader
import java.io.InputStreamReader

object SystemUtil {

    fun exec(cmd: String) = ProcessBuilder(*cmd.split(' ').toTypedArray()).run {
        redirectErrorStream(true)
        val buf = StringBuilder()
        val proc = start()
        val input = BufferedReader(InputStreamReader(proc.inputStream))
        while (true)
            buf.appendLine(input.readLine() ?: break)
        if (proc.waitFor() != 0)
            error(buf)
        buf
    }
}