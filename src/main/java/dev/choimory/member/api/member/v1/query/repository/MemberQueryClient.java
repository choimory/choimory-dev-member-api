package dev.choimory.member.api.member.v1.query.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch Java API Client 기반 회원 query Repository입니다.
 */
@Repository
@RequiredArgsConstructor
public class MemberQueryClient {

    private final ElasticsearchClient client; // Elasticsearch Java API Client
}
