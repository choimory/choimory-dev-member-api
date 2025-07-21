package io.choimory.member.external.api.mail.v1.domain.dto

import org.springframework.mail.SimpleMailMessage

data class MailDto(
    val to:List<String>,
    val from:String,
    val subject:String,
    val text:String,
) {
    companion object{
        fun toSimpleMailMessage(to: List<String>, from: String, subject: String, text: String):SimpleMailMessage{
            val simpleMailMessage:SimpleMailMessage = SimpleMailMessage()

            simpleMailMessage.setTo(*to.toTypedArray())
            simpleMailMessage.from = from
            simpleMailMessage.subject = subject
            simpleMailMessage.text = text

            return simpleMailMessage
        }
    }
}