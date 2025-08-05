package dev.choimory.member.api.common.domain.document

import java.time.LocalDateTime

data class CommonDateDocument(
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime,
    val deletedAt: LocalDateTime,
)
