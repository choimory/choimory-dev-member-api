package dev.choimory.member.api.member.v1.command.domain.dto

import dev.choimory.member.api.member.v1.command.domain.entity.MemberEntity

data class MemberEntityDto(
    val id: String? = null,
    val email: String? = null,
    val nickname: String? = null,
    val password: String? = null,
    val introduce: String? = null,
) {
    companion object {
        fun toEntity(dto: MemberEntityDto): MemberEntity {
            return MemberEntity(id = dto.id, email = dto.email, nickname = dto.nickname, password = dto.password, introduce = dto.introduce)
        }

        fun toDto(entity: MemberEntity): MemberEntityDto {
            return MemberEntityDto(
                id = entity.id,
                email = entity.email,
                nickname = entity.nickname,
                password = entity.password,
                introduce = entity.introduce,
            )
        }
    }
}
