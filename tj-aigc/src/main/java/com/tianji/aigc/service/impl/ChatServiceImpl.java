package com.tianji.aigc.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.common.utils.UuidUtils;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
    private final ChatMemory chatMemory;
    private final ConcurrentHashMap<String, Boolean> SESSION_STATUS = new ConcurrentHashMap<>();
    private final VectorStore vectorStore;


    @Override
    public void stop(String sessionId) {
        SESSION_STATUS.remove(sessionId);
    }

    @Override
    public Flux<ChatEventVO> chat(String sessionId, String question) {
        //每次读取最新的系统提示词
        String systemPrompt = systemPromptConfig.getChatSystemMessage();
        //获取conversationId
        String conversationId = ChatService.getConversationId(sessionId);
        //定义大模型停止后的回复
        StringBuilder output = new StringBuilder();
        //定义requestId
        String requestId = UuidUtils.generateUuid();
        //获取用户id
        String userId = Convert.toStr(UserContext.getUser());
        //RAG增强生成
        QuestionAnswerAdvisor answerAdvisor = QuestionAnswerAdvisor
                .builder(vectorStore)
                .searchRequest(SearchRequest
                        .builder()
                        .similarityThreshold(0.6f) //相似度阈值
                        .topK(3)                   //获取条数
                        .build())
                .build();


        return chatClient.prompt()
                .system(promptSystemSpec -> {
                    promptSystemSpec
                            .text(systemPrompt) //设置系统提示词
                            .params(Map.of("now", DateUtil.now())); //设置系统提示词参数 ——> 当前时间
                })
                .advisors(advisorSpec -> {
                    advisorSpec.advisors(answerAdvisor) //使用RAG增强生成
                            .param(ChatMemory.CONVERSATION_ID, conversationId);
                })
                .user(question) //用户提示词
                .toolContext(MapUtil.<String, Object>builder() // 设置tool列表
                        .put(Constant.REQUEST_ID, requestId) // 设置请求id参数
                        .put(Constant.USER_ID, userId) // 设置用户id参数
                        .build()
                )//传递requestId过去
                .stream()
                .chatResponse() //流式响应
                .doOnError(throwable -> SESSION_STATUS.remove(sessionId)) //遇到错误时，清空状态
                .doFirst(() -> SESSION_STATUS.put(sessionId, Boolean.TRUE)) //第一次输出内容时执行
                .doOnComplete(() -> SESSION_STATUS.remove(sessionId)) //完成输出时执行
                .doOnCancel(() -> {
                    this.saveChatMessage(conversationId, output.toString());
                })
                .takeWhile(response -> SESSION_STATUS.getOrDefault(sessionId, Boolean.FALSE)) //通过返回值来控制是否输出
                .map(chatResponse -> {
                    //关联requestId 和 metadataId
                    String metadataId = chatResponse.getMetadata().getId();
                    //获取完成原因，如果是STOP的话，对话结束才需要保存，此时才需要设置params
                    String finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                    if (StrUtil.equalsIgnoreCase(finishReason, Constant.STOP)) {
                        //保存
                        ToolResultHolder.put(metadataId, Constant.REQUEST_ID, requestId);
                    }
                    //模型输出
                    String text = chatResponse.getResult().getOutput().getText();
                    //追加到缓冲中
                    output.append(text);
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())//数据
                            .build();
                })
                //添加自定义数据
                .concatWith(Flux.defer(() -> {
                    Map<String, Object> stringObjectMap = ToolResultHolder.get(requestId);
                    if (Objects.nonNull(stringObjectMap)) {
                        //移除数据，防止内存泄漏
                        ToolResultHolder.remove(requestId);
                        //响应给前端的数据
                        ChatEventVO chatEventVO = ChatEventVO.builder()
                                .eventType(ChatEventTypeEnum.PARAM.getValue())
                                .eventData(stringObjectMap)
                                .build();
                        return Flux.just(chatEventVO, ChatEventVO.builder().eventType(ChatEventTypeEnum.STOP.getValue()).build());
                    }
                    return Flux.just(ChatEventVO.builder().eventType(ChatEventTypeEnum.STOP.getValue()).build());
                }));
    }

    /**
     * 手动保存大模型回复
     *
     * @param conversationId
     * @param message
     */
    private void saveChatMessage(String conversationId, String message) {
        chatMemory.add(conversationId, new AssistantMessage(message));
    }
}
