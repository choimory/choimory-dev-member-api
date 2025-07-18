package io.choimory.member.external.api.token.v1.domain

import io.choimory.member.external.api.member.v1.query.domain.dto.MemberDocumentDto

data class TokenDetails(val email: String) {
    companion object {
        fun of(member: MemberDocumentDto): TokenDetails {
            return TokenDetails(member.email)
        }
    }
}
