package com.tianji.aigc.agent;

import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.enums.AgentTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 知识讲解智能体
 */
@Component
@RequiredArgsConstructor
public class KnowledgeAgent extends AbstractAgent{

    private final SystemPromptConfig systemPromptConfig;

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.KNOWLEDGE;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getKnowledgeAgentSystemMessage();
    }
}
