package dev.choimory.member.api.config

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ElasticSearchConfig(
    @Value("\${es-host}")
    val url: String,
    @Value("\${es-port}")
    val port: Int,
    @Value("\${es-user}")
    val userName: String,
    @Value("\${es-password}")
    val password: String,
) {
    @Bean
    fun elasticsearchClient(): ElasticsearchClient {
        val restClient = RestClient.builder(HttpHost(url, port)).build()

        val transport =
            RestClientTransport(
                restClient,
                JacksonJsonpMapper(),
            )

        return ElasticsearchClient(transport)
    }
}
