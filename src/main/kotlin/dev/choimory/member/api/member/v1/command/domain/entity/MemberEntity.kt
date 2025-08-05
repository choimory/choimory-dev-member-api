package dev.choimory.member.api.member.v1.command.domain.entity

import dev.choimory.member.api.common.domain.entity.CommonDateEntity
import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class MemberEntity(
    @Id
    var id: String? = null,
    var email: String? = null,
    var nickname: String? = null,
    var password: String? = null,
    var introduce: String? = null,
) : CommonDateEntity()
