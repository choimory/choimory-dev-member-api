package io.choimory.member.external.api.member.v1.command.repository

import io.choimory.member.external.api.member.v1.command.domain.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberCommandRepository : JpaRepository<Member, String>
