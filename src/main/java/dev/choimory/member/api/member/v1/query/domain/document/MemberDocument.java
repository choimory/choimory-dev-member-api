package dev.choimory.member.api.member.v1.query.domain.document;

import dev.choimory.member.api.common.domain.document.CommonDateDocument;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * 회원 Elasticsearch Document입니다.
 */
@Getter
@Document(indexName = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MemberDocument {

    @Id
    private String id; // 회원 ID

    @Field(type = FieldType.Keyword)
    private String email; // 이메일

    @Field(type = FieldType.Keyword)
    private String nickname; // 닉네임

    @Field(type = FieldType.Keyword)
    private String password; // 암호화된 비밀번호

    @Field(type = FieldType.Keyword)
    private String introduce; // 소개글

    private CommonDateDocument commonDate; // 공통 일시 정보
}
