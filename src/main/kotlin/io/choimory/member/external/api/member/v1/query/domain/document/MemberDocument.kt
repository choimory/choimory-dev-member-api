package io.choimory.member.external.api.member.v1.query.domain.document

import io.choimory.member.external.api.common.domain.document.CommonDateDocument
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

@Document(indexName = "member")
class MemberDocument(
    @Id
    val id: String,
    @Field(type = FieldType.Keyword)
    val email: String,
    @Field(type = FieldType.Keyword)
    val nickname: String,
    @Field(type = FieldType.Keyword)
    val password: String,
    @Field(type = FieldType.Keyword)
    val introduce: String,
    val commonDate: CommonDateDocument,
)
