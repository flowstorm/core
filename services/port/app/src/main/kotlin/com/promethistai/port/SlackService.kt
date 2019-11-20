package com.promethistai.port

import com.promethistai.common.AppConfig
import com.promethistai.common.ObjectUtil
import com.promethistai.port.model.Message
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import java.io.InputStreamReader
import java.io.BufferedReader

class SlackService {

    data class Post(val text: String)

    @Inject
    lateinit var config: AppConfig

    fun sendMessage(message: Message) {
        val str = StringBuilder("[${config["namespace"]}] <${message.sender}>:")
        message.items.forEach {
            str.append(' ').append(it.text)
        }
        val post = Post(str.toString())

        val url = URL(config["slack.url"])
        try {
            val con = url.openConnection() as HttpURLConnection
            con.setRequestMethod("POST")
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            con.setDoOutput(true)
            val json = ObjectUtil.defaultMapper.writeValueAsString(post)
            con.outputStream.use {
                it.write(json.toByteArray(Charsets.UTF_8))
                it.flush()
            }
            val buf = StringBuilder()

            BufferedReader(InputStreamReader(con.inputStream)).readLines().forEach {
                buf.append(it)
            }
            println(json)
            println(buf)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}