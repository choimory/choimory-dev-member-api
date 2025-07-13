package io.choimory.member.external.api.member.v1.query.repository

import io.choimory.member.external.api.member.v1.domain.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberQueryRepository : JpaRepository<Member, String>
