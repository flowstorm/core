package ai.flowstorm.common.messaging

class StdOutSender : MessageSender {
    override fun sendMessage(recipient: MessageSender.Recipient, subject: String, templateName: String, templateVariables: Map<String, String>) {
        println("sendMessage($recipient, $subject, $templateName, $templateVariables)")
    }

    override fun sendMessage(recipient: MessageSender.Recipient, subject: String, text: String) {
        println("sendMessage($recipient, $subject, $text)")
    }
}