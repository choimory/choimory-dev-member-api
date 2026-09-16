package dev.choimory.member.api.member.v1.query.repository;

import dev.choimory.member.api.member.v1.query.domain.document.MemberDocument;
import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 회원 query Elasticsearch Repository입니다.
 */
@Repository
public interface MemberQueryRepository extends ElasticsearchRepository<MemberDocument, String> {

    /**
     * 이메일로 회원 Document를 조회합니다.
     *
     * @param email 이메일
     * @return 회원 Document Optional
     */
    Optional<MemberDocument> findByEmail(String email);
}
