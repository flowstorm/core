package org.promethist.messaging

interface MessageSender {

    fun sendMessage(recipient: Recipient, subject: String, templateName: String, templateVariables: Map<String, String>)

    fun sendMessage(recipient: Recipient, subject: String, text: String)

    data class Recipient(val address: String, val name: String)
}