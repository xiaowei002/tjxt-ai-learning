package com.tianji.aigc.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tianji.aigc.agent.Agent;
import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tj.ai", name = "chat-type", havingValue = "ROUTE")
public class AgentServiceImpl implements ChatService {

    @Override
    public Flux<ChatEventVO> chat(String sessionId, String question) {
        //获取对话输出
        String result = this.findAgentByType(AgentTypeEnum.ROUTE).process(question, sessionId);
        //是否能转换为agentType
        AgentTypeEnum agentTypeEnum = AgentTypeEnum.agentNameOf(result);
        //查找对应的agent实例
        Agent agent = findAgentByType(agentTypeEnum);
        //普通输入
        if (agent == null) {
            ChatEventVO chatEventVO = ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.DATA.getValue())
                    .eventData(result)
                    .build();
            return Flux.just(chatEventVO, ChatEventVO.builder().eventType(ChatEventTypeEnum.STOP.getValue()).build());
        }
        //agent调用
        return agent.processStream(question, sessionId);
    }

    @Override
    public void stop(String sessionId) {
        Agent routeAgent = findAgentByType(AgentTypeEnum.ROUTE);
        routeAgent.stop(sessionId);
    }

    /**
     * 根据代理类型查找对应的Agent实例
     *
     * @param agentTypeEnum 要查找的代理类型
     * @return 与给定类型匹配的Agent实例，如果未找到或类型为null则返回null
     */
    private Agent findAgentByType(AgentTypeEnum agentTypeEnum) {
        if (Objects.isNull(agentTypeEnum)) {
            return null;
        }
        //获取到所有的agent
        Map<String, Agent> beans = SpringUtil.getBeansOfType(Agent.class);
        for (Agent agent : beans.values()) {
            if (agent.getAgentType().equals(agentTypeEnum)) {
                return agent;
            }
        }
        return null;
    }

}
