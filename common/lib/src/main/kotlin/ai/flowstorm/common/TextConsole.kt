package ai.flowstorm.common

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream

abstract class TextConsole(
        val input: BufferedReader = BufferedReader(InputStreamReader(System.`in`)),
        val output: PrintStream = System.out
) : Runnable {

    var stop = false

    open fun beforeInput() {}

    abstract fun afterInput(text: String)

    open fun done() {}

    override fun run() {
        while (!stop) {
            beforeInput()
            if (!stop) {
                val text = input.readLine()!!.trim()
                if (text == "exit")
                    break
                afterInput(text)
            }
        }
        done()
    }
}