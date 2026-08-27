package com.tianji.aigc.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import redis.clients.jedis.JedisPooled;

@Configuration
public class VectorStoreConfig {
    /**
     * 知识库专用 Redis Stack 客户端。
     * 不复用业务 Redis，避免影响原来的 StringRedisTemplate。
     */
    @Bean(destroyMethod = "close")
    public JedisPooled vectorStoreJedis(
            @Value("${tj.ai.vectorstore.redis.host:localhost}") String host,
            @Value("${tj.ai.vectorstore.redis.port:6380}") int port) {
        return new JedisPooled(host, port);
    }

    /**
     * Redis 向量库。
     * @Primary 表示注入 VectorStore 接口时优先使用该实现。
     */
    @Bean("redisVectorStore")
    @Primary
    public RedisVectorStore redisVectorStore(
            @Qualifier("vectorStoreJedis") JedisPooled jedis,
            EmbeddingModel embeddingModel,
            @Value("${spring.ai.vectorstore.redis.index-name:rag_index}")
            String indexName,
            @Value("${spring.ai.vectorstore.redis.prefix:rag_prefix}")
            String prefix,
            @Value("${spring.ai.vectorstore.redis.initialize-schema:true}")
            boolean initializeSchema) {

        return RedisVectorStore.builder(jedis, embeddingModel)
                .indexName(indexName)
                .prefix(prefix)
                .initializeSchema(initializeSchema)
                .build();
    }
}
