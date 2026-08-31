package com.tianji.aigc.agent;

import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.enums.AgentTypeEnum;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 路由智能体
 */
@Component
public class RouteAgent extends AbstractAgent{

    private final SystemPromptConfig systemPromptConfig;
    private final ChatClient routeChatClient;

    public RouteAgent(SystemPromptConfig systemPromptConfig,
                      @Qualifier("routeChatClient") ChatClient routeChatClient) {
        this.systemPromptConfig = systemPromptConfig;
        this.routeChatClient = routeChatClient;
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.ROUTE;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getRouteAgentSystemMessage();
    }

    @Override
    protected ChatClient chatClient() {
        return this.routeChatClient;
    }
}
