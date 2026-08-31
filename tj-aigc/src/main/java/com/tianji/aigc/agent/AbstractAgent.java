package com.tianji.aigc.agent;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractAgent implements Agent{

    @Resource
    private ChatClient chatClient;

    @Resource
    private ChatMemory chatMemory;

    @Resource
    private ChatSessionService chatSessionService;


    private final ConcurrentHashMap<String, Boolean> SESSION_STATUS = new ConcurrentHashMap<>();



    @Override
    public Flux<ChatEventVO> processStream(String question, String sessionId) {
        //生成请求id
        String requestId = this.generateRequestId();
        //生成conversationId
        String conversationId = ChatService.getConversationId(sessionId);
        //定义大模型停止后的回复
        StringBuilder output = new StringBuilder();

        //异步设置会话标题，标题内容为用户问题
        this.chatSessionService.update(sessionId, question, UserContext.getUser());

        return getClientRequest(question, sessionId, requestId)
                .stream()
                .chatResponse()
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

    @Override
    public String process(String question, String sessionId) {
        String requestId = generateRequestId();
        //异步设置会话标题，标题内容为用户问题
        this.chatSessionService.update(sessionId, question, UserContext.getUser());

        return getClientRequest(question, sessionId, requestId)
                .call()
                .content();
    }


    @NotNull
    private ChatClient.ChatClientRequestSpec getClientRequest(String question, String sessionId, String requestId) {
        return this.chatClient()
                .prompt()
                .system(promptSystemSpec -> promptSystemSpec.text(this.systemMessage()).params(this.systemMessageParams()))
                .advisors(advisorSpec -> advisorSpec.advisors(this.advisors()).params(this.advisorParams(sessionId, requestId)))
                .tools(this.tools())
                .toolContext(this.toolContext(sessionId, requestId))
                .user(question);
    }

    /**
     * 获取当前智能体使用的 ChatClient，子类可覆盖以隔离不同的 Advisor。
     */
    protected ChatClient chatClient() {
        return this.chatClient;
    }

    @Override
    public void stop(String sessionId) {
        SESSION_STATUS.remove(sessionId);
    }

    @Override
    public Map<String, Object> advisorParams(String sessionId, String requestId) {
        return Map.of(ChatMemory.CONVERSATION_ID, ChatService.getConversationId(sessionId));
    }

    /**
     * 生成requestId
     * @return 请求id
     */
    private String generateRequestId() {
        return IdUtil.fastSimpleUUID();
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
