package com.tianji.aigc.agent;

import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.tools.TradeTools;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 课程购买智能体
 * 不需要advisor增强
 */
@Component
@RequiredArgsConstructor
public class BuyAgent extends AbstractAgent {

    public final SystemPromptConfig systemPromptConfig;
    public final TradeTools tradeTools;

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.BUY;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getBuyAgentSystemMessage();
    }

    @Override
    public Object[] tools() {
        return new Object[]{this.tradeTools};
    }

    @Override
    public Map<String, Object> toolContext(String sessionId, String requestId) {
        var userId = UserContext.getUser();
        return Map.of(
                Constant.USER_ID, userId, // 设置用户id参数
                Constant.REQUEST_ID, requestId  // 设置请求id参数
        );
    }
}
