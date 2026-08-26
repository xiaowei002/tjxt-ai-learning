package com.tianji.aigc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.aigc.config.SessionProperties;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.enums.MessageTypeEnum;
import com.tianji.aigc.mapper.ChatSessionMapper;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {


    private final SessionProperties sessionProperties;
    private final ChatMemory chatMemory;


    @Override
    public SessionVO createSession(Integer num) {
        var sessionVO = BeanUtil.toBean(sessionProperties, SessionVO.class);
        // 随机获取examples
        sessionVO.setExamples(RandomUtil.randomEleList(sessionProperties.getExamples(), num));

        // 随机生成sessionId
        sessionVO.setSessionId(IdUtil.fastSimpleUUID());

        // 构建持久化对象，并持久化
        var chatSession = ChatSession.builder()
                .sessionId(sessionVO.getSessionId())
                .userId(UserContext.getUser())
                .build();
        super.save(chatSession);

        return sessionVO;
    }

    /**
     * 获取对话详情
     *
     * @param sessionId
     * @return
     */
    @Override
    public List<MessageVO> queryBySessionId(String sessionId) {
        //获取ConversationId
        String conversationId = ChatService.getConversationId(sessionId);
        //
        List<Message> messages = chatMemory.get(conversationId);
        if (CollUtils.isEmpty(messages)) {
            return List.of();
        }
        return messages.stream().filter(
                        message ->
                                message.getMessageType().equals(MessageType.ASSISTANT) || message.getMessageType().equals(MessageType.USER))
                .map(message -> MessageVO.builder().type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                        .content(message.getText()).build())
                .toList();
    }


    @Override
    public List<SessionVO.Example> hotExamples(Integer num) {
        return RandomUtil.randomEleList(sessionProperties.getExamples(), num);
    }
}
