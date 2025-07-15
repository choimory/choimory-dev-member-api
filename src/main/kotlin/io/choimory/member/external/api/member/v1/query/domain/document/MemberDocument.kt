package io.choimory.member.external.api.member.v1.query.domain.document

import io.choimory.member.external.api.common.domain.document.CommonDateDocument
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document

@Document(indexName = "member")
class MemberDocument(
    @Id
    val id: String,
    val email: String,
    val nickname: String,
    val password: String,
    val introduce: String,
    val commonDate: CommonDateDocument,
)
