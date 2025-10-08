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

/**
 * 搜索服务Elasticsearch优化配置类
 * 针对搜索查询场景进行性能优化
 *
 * @author what's up
 * @since 1.0.0
 */
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

    /**
     * 创建优化的Elasticsearch客户端
     * 针对搜索查询场景进行性能调优
     *
     * @return ElasticsearchClient实例
     */
    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient() {
        log.info("初始化搜索服务Elasticsearch客户端");
        log.info("ES URIs: {}", elasticsearchUris);
        log.info("连接超时: {}, Socket超时: {}", connectionTimeout, socketTimeout);
        log.info("最大连接数: {}, 每路由最大连接数: {}", maxConnections, maxConnectionsPerRoute);

        // 解析多个URI
        String[] uris = elasticsearchUris.split(",");
        HttpHost[] hosts = new HttpHost[uris.length];

        for (int i = 0; i < uris.length; i++) {
            String uri = uris[i].trim();
            String[] parts = uri.replace("http://", "").replace("https://", "").split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
            boolean isHttps = uri.startsWith("https://");
            hosts[i] = new HttpHost(host, port, isHttps ? "https" : "http");
        }

        // 创建RestClient构建器
        RestClientBuilder builder = RestClient.builder(hosts);

        // 配置认证（如果提供了用户名和密码）
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));

            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                            .setMaxConnTotal(maxConnections)
                            .setMaxConnPerRoute(maxConnectionsPerRoute)
                            // 搜索服务优化：启用连接保持活跃
                            .setKeepAliveStrategy((response, context) -> Duration.ofMinutes(5).toMillis())
            );
        } else {
            // 仅配置连接池
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder
                            .setMaxConnTotal(maxConnections)
                            .setMaxConnPerRoute(maxConnectionsPerRoute)
                            // 搜索服务优化：启用连接保持活跃
                            .setKeepAliveStrategy((response, context) -> Duration.ofMinutes(5).toMillis())
            );
        }

        // 配置请求超时 - 搜索服务需要更长的超时时间
        builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                        .setConnectTimeout((int) connectionTimeout.toMillis())
                        .setSocketTimeout((int) socketTimeout.toMillis())
                        .setConnectionRequestTimeout(5000) // 从连接池获取连接的超时时间
        );

        // 配置节点选择器 - 搜索服务优化
        builder.setNodeSelector(nodes -> nodes.iterator());

        // 创建低级客户端
        RestClient restClient = builder.build();

        // 创建传输层对象
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // 创建API客户端
        ElasticsearchClient client = new ElasticsearchClient(transport);

        log.info("搜索服务Elasticsearch客户端初始化完成");
        return client;
    }


}
