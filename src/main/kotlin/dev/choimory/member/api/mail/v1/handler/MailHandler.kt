package dev.choimory.member.api.mail.v1.handler

import org.springframework.stereotype.Component

@Component
class MailHandler(
    // private val javaMailSender: JavaMailSender,
) {
    /*fun sendMail(
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
    }*/
}
