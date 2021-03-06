package ai.flowstorm.common.messaging

import com.commit451.mailgun.Contact
import com.commit451.mailgun.Mailgun
import com.commit451.mailgun.SendMessageRequest
import ai.flowstorm.util.LoggerDelegate

class MailgunSender(private val mailgun: Mailgun, val from: Contact) : MessageSender {

    private val logger by LoggerDelegate()

    override fun sendMessage(recipient: MessageSender.Recipient, subject: String, templateName: String, templateVariables: Map<String, String>) {

        val to = mutableListOf(Contact(recipient.address, recipient.name))

        val requestBuilder = SendMessageRequest.Builder(from)
                .to(to)
                .subject(subject)
                .template(templateName)
                .templateVariables(templateVariables)
                .text("") //we send empty text body when using a template

        logger.info("Sending mail to $recipient with subject: $subject")
        mailgun.sendMessage(requestBuilder.build()).blockingGet()
    }

    override fun sendMessage(recipient: MessageSender.Recipient, subject: String, text: String) {

        val to = mutableListOf(Contact(recipient.address, recipient.name))

        val requestBuilder = SendMessageRequest.Builder(from)
                .to(to)
                .subject(subject)
                .text(text)

        logger.info("Sending mail to $recipient with subject: $subject")
        mailgun.sendMessage(requestBuilder.build()).blockingGet()
    }
}