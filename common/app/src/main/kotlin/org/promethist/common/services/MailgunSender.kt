package org.promethist.common.services

import com.commit451.mailgun.Contact
import com.commit451.mailgun.Mailgun
import com.commit451.mailgun.SendMessageRequest
import org.promethist.messaging.MessageSender

class MailgunSender(val mailgun: Mailgun, val from: Contact) : MessageSender {

    override fun sendMessage(recipient: MessageSender.Recipient, subject: String, templateName: String, templateVariables: Map<String, String>) {

        val to = mutableListOf(Contact(recipient.address, recipient.name))

        val requestBuilder = SendMessageRequest.Builder(from)
                .to(to)
                .subject(subject)
                .template(templateName)
                .templateVariables(templateVariables)
                .text("") //we send empty text body when using a template

        mailgun.sendMessage(requestBuilder.build()).blockingGet()
    }

    override fun sendMessage(recipient: MessageSender.Recipient, subject: String, text: String) {

        val to = mutableListOf(Contact(recipient.address, recipient.name))

        val requestBuilder = SendMessageRequest.Builder(from)
                .to(to)
                .subject(subject)
                .text(text)

        mailgun.sendMessage(requestBuilder.build()).blockingGet()
    }
}