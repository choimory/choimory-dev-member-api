package io.choimory.member.external.api.member.v1.command.domain.entity

import io.choimory.member.external.api.common.entity.CommonDate
import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class Member(
    @Id
    var id: String,
    var email: String,
    var password: String,
    var introduce: String,
) : CommonDate()
