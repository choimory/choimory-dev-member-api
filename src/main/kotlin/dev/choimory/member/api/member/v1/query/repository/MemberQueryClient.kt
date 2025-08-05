package dev.choimory.member.api.member.v1.query.repository

import co.elastic.clients.elasticsearch.ElasticsearchClient
import org.springframework.stereotype.Repository

@Repository
class MemberQueryClient(
    private val client: ElasticsearchClient,
)
