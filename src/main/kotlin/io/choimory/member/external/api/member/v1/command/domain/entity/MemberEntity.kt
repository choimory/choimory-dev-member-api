package io.choimory.member.external.api.member.v1.command.domain.entity

import io.choimory.member.external.api.common.domain.CommonDateEntity
import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class MemberEntity(id: String?, email: String?, nickname: String?, password: String?, introduce: String?) :
    CommonDateEntity() {
    @Id
    var id: String? = id
        protected set
    var email: String? = email
        protected set
    var nickname: String? = nickname
        protected set
    var password: String? = password
        protected set
    var introduce: String? = introduce
        protected set
}
