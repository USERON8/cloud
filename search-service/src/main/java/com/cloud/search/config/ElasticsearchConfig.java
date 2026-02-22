package com.cloud.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.cloud.search.repository")
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.elasticsearch.connection-timeout:5s}")
    private Duration connectionTimeout;

    @Value("${spring.elasticsearch.socket-timeout:60s}")
    private Duration socketTimeout;

    @Value("${elasticsearch.search.max-connections:200}")
    private int maxConnections;

    @Value("${elasticsearch.search.max-connections-per-route:20}")
    private int maxConnectionsPerRoute;

    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient() {
        String[] uriArray = elasticsearchUris.split(",");
        HttpHost[] hosts = new HttpHost[uriArray.length];
        for (int i = 0; i < uriArray.length; i++) {
            hosts[i] = HttpHost.create(uriArray[i].trim());
        }

        RestClientBuilder builder = RestClient.builder(hosts);

        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setMaxConnTotal(maxConnections);
            httpClientBuilder.setMaxConnPerRoute(maxConnectionsPerRoute);
            httpClientBuilder.setKeepAliveStrategy((response, context) -> Duration.ofMinutes(5).toMillis());

            if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
            return httpClientBuilder;
        });

        builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                .setConnectTimeout((int) connectionTimeout.toMillis())
                .setSocketTimeout((int) socketTimeout.toMillis())
                .setConnectionRequestTimeout(5000)
        );

        RestClient restClient = builder.build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
