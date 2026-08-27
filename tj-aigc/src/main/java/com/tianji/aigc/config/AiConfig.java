package com.tianji.aigc.config;

import com.tianji.aigc.memory.mongodb.MongoDBChatMemoryRepository;
import com.tianji.aigc.memory.MysqlChatMemoryRepository;
import com.tianji.aigc.memory.RedisChatMemoryRepository;
import com.tianji.aigc.tools.CourseTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AiConfig {

    @Value("${tj.ai.memory.max:1024}")
    private Integer maxMessages;

    @Value("${tj.ai.memory.type: Redis}")
    private String memoryType;

    @Bean
    public ChatClient getChatClient(ChatClient.Builder builder,
                                    @Qualifier("loggerAdvisor") Advisor loggerAdvisor, //日志记录器
                                    @Qualifier("messageChatMemoryAdvisor") Advisor messageChatMemoryAdvisor,
                                    CourseTools courseTools) {
        return builder
                .defaultAdvisors(loggerAdvisor, messageChatMemoryAdvisor)
                .defaultTools(courseTools)
                .build();
    }

    @Bean("loggerAdvisor")
    public Advisor getAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    @Bean
    @ConditionalOnProperty(prefix = "tj.ai.memory", value = "type", havingValue = "Redis")
    public ChatMemoryRepository redisChatMemoryRepository() {
        return new RedisChatMemoryRepository();
    }


    @Bean
    @ConditionalOnProperty(prefix = "tj.ai.memory", value = "type", havingValue = "MySQL")
    public ChatMemoryRepository mysqlChatMemoryRepository() {
        return new MysqlChatMemoryRepository();
    }

    @Bean
    @ConditionalOnProperty(prefix = "tj.ai.memory", value = "type", havingValue = "MongoDB")
    public ChatMemoryRepository mongoDBChatMemoryRepository() {
        return new MongoDBChatMemoryRepository();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        // 基于 chatMemoryRepository 对象构建 chatMemory 对象
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
    }

    @Bean("messageChatMemoryAdvisor")
    public Advisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
    }
}
