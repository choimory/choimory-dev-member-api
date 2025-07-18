package io.choimory.member.external.api.member.v1.query.repository

import io.choimory.member.external.api.member.v1.query.domain.document.MemberDocument
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MemberQueryRepository : ElasticsearchRepository<MemberDocument, String>{
    fun findByEmail(email:String): Optional<MemberDocument>
}
