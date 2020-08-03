package com.promethist.client.standalone.cli

import com.promethist.client.standalone.Application
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import cz.alry.jcommander.CommandRunner
import javax.sound.sampled.*

class DiagCommand: CommandRunner<Application.Params, DiagCommand.Params> {

    @Parameters(commandNames = ["diag"], commandDescription = "Show diagnostics information")
    class Params {

        @Parameter(names = ["-i"], order = 1, description = "Test index")
        var index = 0
    }

    override fun run(globalParams: Application.Params, params: Params) {
        val out = System.out
        val mis = AudioSystem.getMixerInfo()
        out.println("Audio System Mixers:")
        for (i in mis.indices) {
            out.println("$i: ${mis[i]}")
            val mixer = AudioSystem.getMixer(mis[i])
            out.println("  Source lines:")
            for (info in mixer.sourceLineInfo) {
                out.println("   INFO $info")
                val line = AudioSystem.getLine(info)
                out.println("   LINE $line")
                if (info is DataLine.Info) {
                    for (format in info.formats) {
                        out.println("    $format")
                    }
                }
                if (info is Port.Info) {
                    out.println("    name = ${info.name}")
                }
            }
            out.println("  Target lines:")
            for (info in mixer.targetLineInfo) {
                out.println("   INFO $info")
                val line = AudioSystem.getLine(info)
                out.println("   LINE $line")
                if (info is DataLine.Info) {
                    for (format in info.formats) {
                        out.println("    $format")
                    }
                }
                if (info is Port.Info) {
                    out.println("    name = ${info.name}")
                }
            }
        }

        if (AudioSystem.isLineSupported(Port.Info.MICROPHONE)) {
            val line = AudioSystem.getLine(Port.Info.MICROPHONE)
            out.println(line)
        } else {
            out.println("NO MICROPHONE")
        }

        /*
        val audioFormat = AudioFormat(16000.0f, 16, 2, true, false)
        val info = AudioSystem.getMixerInfo()[params.index]
        val line = AudioSystem.getTargetDataLine(audioFormat, info) as TargetDataLine

        out.println("${params.index} - $line")

         */
    }
}