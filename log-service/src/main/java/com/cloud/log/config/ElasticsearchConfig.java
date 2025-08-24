package com.cloud.log.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch配置类
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.url}")
    private String elasticsearchUrl;

    /**
     * 创建Elasticsearch客户端
     *
     * @return ElasticsearchClient实例
     */
    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 解析URL
        String[] parts = elasticsearchUrl.replace("http://", "").split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        // 创建低级客户端
        RestClient restClient = RestClient.builder(
                new HttpHost(host, port)
        ).build();

        // 创建传输层对象
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // 创建API客户端
        return new ElasticsearchClient(transport);
    }
}