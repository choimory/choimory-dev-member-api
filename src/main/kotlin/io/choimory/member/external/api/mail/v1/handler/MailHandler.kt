package io.choimory.member.external.api.mail.v1.handler

import io.choimory.member.external.api.mail.v1.domain.dto.MailDto
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class MailHandler(
    private val javaMailSender: JavaMailSender
) {
    fun sendMail(to:List<String>, from:String?, subject:String, text:String):MailDto{
        val message:SimpleMailMessage = MailDto.toSimpleMailMessage(to, from?:"", subject, text)

        javaMailSender.send(message)
        TODO()
    }
}