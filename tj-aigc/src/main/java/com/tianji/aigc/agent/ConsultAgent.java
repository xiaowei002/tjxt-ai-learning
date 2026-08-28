package com.tianji.aigc.agent;

import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.tools.CourseTools;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 课程咨询智能体
 */
@Component
@RequiredArgsConstructor
public class ConsultAgent extends AbstractAgent {

    private final SystemPromptConfig systemPromptConfig;
    private final CourseTools courseTools;
    private final VectorStore vectorStore;

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.CONSULT;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getConsultAgentSystemMessage();
    }

    @Override
    public Object[] tools() {
        return new Object[]{this.courseTools};
    }

    @Override
    public Map<String, Object> toolContext(String sessionId, String requestId) {
        return Map.of(
                Constant.REQUEST_ID, requestId,         //请求id
                Constant.USER_ID, UserContext.getUser() //用户id
        );
    }

    @Override
    public List<Advisor> advisors() {
        //RAG增强生成
        QuestionAnswerAdvisor answerAdvisor = QuestionAnswerAdvisor
                .builder(vectorStore)
                .searchRequest(SearchRequest
                        .builder()
                        .similarityThreshold(0.6f) //相似度阈值
                        .topK(3)                   //获取条数
                        .build())
                .build();
        return List.of(answerAdvisor);
    }
}
