package com.promethistai.common.services

interface EmailSender {
    fun sendEmail(recipient: Recipient, subject: String, templateName: String, templateVariables: Map<String, String>)

    data class Recipient(val email: String, val name: String)
}