package dev.choimory.member.api.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch Java API Client 설정입니다.
 */
@Configuration
public class ElasticSearchConfig {

    private final String url; // Elasticsearch host
    private final int port; // Elasticsearch port
    private final String userName; // Elasticsearch 사용자명
    private final String password; // Elasticsearch 비밀번호

    /**
     * Elasticsearch 설정값을 주입받습니다.
     *
     * @param url Elasticsearch host
     * @param port Elasticsearch port
     * @param userName Elasticsearch 사용자명
     * @param password Elasticsearch 비밀번호
     */
    public ElasticSearchConfig(
            @Value("${es-host}") String url,
            @Value("${es-port}") int port,
            @Value("${es-user}") String userName,
            @Value("${es-password}") String password) {
        this.url = url;
        this.port = port;
        this.userName = userName;
        this.password = password;
    }

    /**
     * ElasticsearchClient Bean을 생성합니다.
     *
     * @return ElasticsearchClient
     */
    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient =
                RestClient.builder(HttpHost.create(url + ":" + port)).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
