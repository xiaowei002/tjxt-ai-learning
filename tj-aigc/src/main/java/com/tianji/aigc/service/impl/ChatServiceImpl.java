package com.tianji.aigc.service.impl;

import cn.hutool.core.date.DateUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
    private final StringRedisTemplate redisTemplate;
    private final String SESSION_STATUS_KEY_PREFIX = "session_status_key";

    @Override
    public void stop(String sessionId) {
        redisTemplate.delete(SESSION_STATUS_KEY_PREFIX + sessionId);
    }

    @Override
    public Flux<ChatEventVO> chat(String sessionId, String question) {
        //每次读取最新的系统提示词
        String systemPrompt = systemPromptConfig.getChatSystemMessage();

        return chatClient.prompt()
                .system(promptSystemSpec -> {
                    promptSystemSpec
                            .text(systemPrompt) //设置系统提示词
                            .params(Map.of("now", DateUtil.now())); //设置系统提示词参数 ——> 当前时间
                })
                .user(question)
                .stream()
                .chatResponse()
                .doOnError(throwable -> redisTemplate.delete(SESSION_STATUS_KEY_PREFIX + sessionId)) //遇到错误时，清空状态
                .doFirst(() -> redisTemplate.opsForValue().set(SESSION_STATUS_KEY_PREFIX + sessionId, Boolean.TRUE, Duration.ofMinutes(30))) //第一次输出内容时执行,过期时间设置30min
                .doOnComplete(() -> redisTemplate.delete(SESSION_STATUS_KEY_PREFIX + sessionId)) //完成输出时执行
                .takeWhile(response -> {
                    Boolean flag = redisTemplate.opsForValue().get(SESSION_STATUS_KEY_PREFIX + sessionId);
                    return flag == null ? Boolean.FALSE : flag;
                }) //通过返回值来控制是否输出
                .map(chatResponse -> {
                    String text = chatResponse.getResult().getOutput().getText();
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())//数据
                            .build();
                })
                .concatWith(Flux.just(ChatEventVO.builder().eventType(ChatEventTypeEnum.STOP.getValue()).build()));
    }
}
