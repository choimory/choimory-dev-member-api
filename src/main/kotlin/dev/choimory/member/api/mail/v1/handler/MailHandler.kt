package dev.choimory.member.api.mail.v1.handler

import dev.choimory.member.api.common.exception.CommonException
import dev.choimory.member.api.mail.v1.domain.dto.MailDto
import org.springframework.http.HttpStatus
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class MailHandler(
    private val javaMailSender: JavaMailSender,
) {
    fun sendMail(
        to: List<String>,
        from: String?,
        subject: String,
        text: String,
    ): SimpleMailMessage {
        val message: SimpleMailMessage = MailDto.toSimpleMailMessage(to, from ?: "", subject, text)

        try {
            javaMailSender.send(message)
        } catch (exception: Exception) {
            throw CommonException(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.message)
        }

        return message
    }
}
