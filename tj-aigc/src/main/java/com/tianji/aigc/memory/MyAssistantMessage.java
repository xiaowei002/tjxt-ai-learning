package com.tianji.aigc.memory;

import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.units.qual.A;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.content.Media;

import java.util.List;
import java.util.Map;

/**
 * 自定义大模型返回数据
 */
@Getter
@Setter
public class MyAssistantMessage extends AssistantMessage {

    private Map<String, Object> params;

    public MyAssistantMessage(String content, Map<String, Object> properties, List<ToolCall> toolCalls, List<Media> media, Map<String, Object> params) {
        super(content, properties, toolCalls, media);
        this.params = params;
    }
}
