package dev.choimory.member.api.token.v1.domain

import dev.choimory.member.api.member.v1.query.domain.dto.MemberDocumentDto

data class TokenDetails(val email: String) {
    companion object {
        fun of(member: MemberDocumentDto): TokenDetails {
            return TokenDetails(member.email)
        }
    }
}
