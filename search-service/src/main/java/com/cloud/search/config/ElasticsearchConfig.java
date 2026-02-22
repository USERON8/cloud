package com.cloud.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
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

import java.time.Duration;








@Slf4j
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
        
        
        

        
        String[] uris = elasticsearchUris.split(",");
        HttpHost[] hosts = new HttpHost[uris.length];

        for (int i = 0; i < uris.length; i++) {
            String uri = uris[i].trim();
            String[] parts = uri.replace("http:
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
            boolean isHttps = uri.startsWith("https://");
            hosts[i] = new HttpHost(host, port, isHttps ? "https" : "http");
        }

        
        RestClientBuilder builder = RestClient.builder(hosts);

        
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));

            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                            .setMaxConnTotal(maxConnections)
                            .setMaxConnPerRoute(maxConnectionsPerRoute)
                            
                            .setKeepAliveStrategy((response, context) -> Duration.ofMinutes(5).toMillis())
            );
        } else {
            
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder
                            .setMaxConnTotal(maxConnections)
                            .setMaxConnPerRoute(maxConnectionsPerRoute)
                            
                            .setKeepAliveStrategy((response, context) -> Duration.ofMinutes(5).toMillis())
            );
        }

        
        builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                        .setConnectTimeout((int) connectionTimeout.toMillis())
                        .setSocketTimeout((int) socketTimeout.toMillis())
                        .setConnectionRequestTimeout(5000) 
        );

        
        builder.setNodeSelector(nodes -> nodes.iterator());

        
        RestClient restClient = builder.build();

        
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        
        ElasticsearchClient client = new ElasticsearchClient(transport);

        
        return client;
    }


}
