package io.choimory.member.external.api.member.v1.command.domain.entity

import io.choimory.member.external.api.common.domain.CommonDateEntity
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
