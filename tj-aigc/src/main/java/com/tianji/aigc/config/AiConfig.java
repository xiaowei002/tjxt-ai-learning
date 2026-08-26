package com.tianji.aigc.config;

import com.tianji.aigc.memory.RedisChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${tj.ai.memory.max:1024}")
    private Integer maxMessages;

    @Bean
    public ChatClient getChatClient(ChatClient.Builder builder,
                                    @Qualifier("loggerAdvisor") Advisor loggerAdvisor, //日志记录器
                                    @Qualifier("messageChatMemoryAdvisor") Advisor messageChatMemoryAdvisor) {
        return builder
                .defaultAdvisors(loggerAdvisor, messageChatMemoryAdvisor)
                .build();
    }

    @Bean("loggerAdvisor")
    public Advisor getAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new RedisChatMemoryRepository();
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
