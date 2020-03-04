package com.promethist.common.services

import com.commit451.mailgun.Contact
import com.commit451.mailgun.Mailgun
import com.commit451.mailgun.SendMessageRequest

class MailgunSender(val mailgun: Mailgun, val from: Contact) : EmailSender {

    override fun sendEmail(recipient: EmailSender.Recipient, subject: String, templateName: String, templateVariables: Map<String, String>) {

        val to = mutableListOf(Contact(recipient.email, recipient.name))

        val requestBuilder = SendMessageRequest.Builder(from)
                .to(to)
                .subject(subject)
                .template(templateName)
                .templateVariables(templateVariables)
                .text("") //we send empty text body when using a template

        val response = mailgun.sendMessage(requestBuilder.build()).blockingGet()
    }
}