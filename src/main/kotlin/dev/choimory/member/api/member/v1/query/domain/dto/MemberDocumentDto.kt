package dev.choimory.member.api.member.v1.query.domain.dto

import dev.choimory.member.api.member.v1.query.domain.document.MemberDocument

data class MemberDocumentDto(val email: String) {
    companion object {
        fun from(memberDocument: MemberDocument): MemberDocumentDto {
            TODO()
        }
    }
}
