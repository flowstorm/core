package org.promethist.common.services

import org.promethist.messaging.MessageSender

class DummySender : MessageSender {
    override fun sendMessage(recipient: MessageSender.Recipient, subject: String, templateName: String, templateVariables: Map<String, String>) {
        println("sendMessage($recipient, $subject, $templateName, $templateVariables)")
    }

    override fun sendMessage(recipient: MessageSender.Recipient, subject: String, text: String) {
        println("sendMessage($recipient, $subject, $text)")
    }
}