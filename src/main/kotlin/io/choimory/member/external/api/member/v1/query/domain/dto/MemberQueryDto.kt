package io.choimory.member.external.api.member.v1.query.domain.dto

import io.choimory.member.external.api.member.v1.query.domain.document.MemberDocument

data class MemberQueryDto(val email: String){
    companion object {
        fun from(memberDocument: MemberDocument): MemberQueryDto {
            TODO()
        }
    }
}
