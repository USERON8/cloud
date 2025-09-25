package com.cloud.log.config;

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

/**
 * 日志服务Elasticsearch优化配置类
 * 针对日志写入场景进行性能优化
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.cloud.log.repository")
public class ElasticsearchConfig {

    @Value("${elasticsearch.url:http://localhost:9200}")
    private String elasticsearchUrl;

    @Value("${elasticsearch.username:}")
    private String username;

    @Value("${elasticsearch.password:}")
    private String password;

    @Value("${elasticsearch.connection-timeout:10s}")
    private Duration connectionTimeout;

    @Value("${elasticsearch.socket-timeout:30s}")
    private Duration socketTimeout;

    @Value("${elasticsearch.max-connections:100}")
    private int maxConnections;

    @Value("${elasticsearch.max-connections-per-route:10}")
    private int maxConnectionsPerRoute;

    /**
     * 创建优化的Elasticsearch客户端
     * 针对日志写入场景进行性能调优
     *
     * @return ElasticsearchClient实例
     */
    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient() {
        log.info("初始化日志服务Elasticsearch客户端");
        log.info("ES URL: {}", elasticsearchUrl);
        log.info("连接超时: {}, Socket超时: {}", connectionTimeout, socketTimeout);
        log.info("最大连接数: {}, 每路由最大连接数: {}", maxConnections, maxConnectionsPerRoute);

        // 解析URL
        String[] parts = elasticsearchUrl.replace("http://", "").replace("https://", "").split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
        boolean isHttps = elasticsearchUrl.startsWith("https://");

        // 创建HTTP主机
        HttpHost httpHost = new HttpHost(host, port, isHttps ? "https" : "http");

        // 创建RestClient构建器
        RestClientBuilder builder = RestClient.builder(httpHost);

        // 配置认证（如果提供了用户名和密码）
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

            builder.setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                    .setMaxConnTotal(maxConnections)
                    .setMaxConnPerRoute(maxConnectionsPerRoute)
            );
        } else {
            // 仅配置连接池
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder
                    .setMaxConnTotal(maxConnections)
                    .setMaxConnPerRoute(maxConnectionsPerRoute)
            );
        }

        // 配置请求超时
        builder.setRequestConfigCallback(requestConfigBuilder ->
            requestConfigBuilder
                .setConnectTimeout((int) connectionTimeout.toMillis())
                .setSocketTimeout((int) socketTimeout.toMillis())
        );

        // 创建低级客户端
        RestClient restClient = builder.build();

        // 创建传输层对象
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // 创建API客户端
        ElasticsearchClient client = new ElasticsearchClient(transport);

        log.info("日志服务Elasticsearch客户端初始化完成");
        return client;
    }


}