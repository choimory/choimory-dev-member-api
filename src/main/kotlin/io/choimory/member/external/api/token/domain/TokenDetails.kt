package io.choimory.member.external.api.token.domain

import io.choimory.member.external.api.member.v1.query.domain.dto.MemberQueryDto

data class TokenDetails(val email: String) {
    companion object {
        fun of(member: MemberQueryDto): TokenDetails {
            return TokenDetails(member.email)
        }
    }
}
