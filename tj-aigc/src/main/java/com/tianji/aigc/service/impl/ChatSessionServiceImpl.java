package com.tianji.aigc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.aigc.config.SessionProperties;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.enums.MessageTypeEnum;
import com.tianji.aigc.mapper.ChatSessionMapper;
import com.tianji.aigc.memory.MyAssistantMessage;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.ChatSessionVO;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
     * 更新历史会话标题
     *
     * @param sessionId 会话信息
     * @param title     标题
     */
    @Override
    public void updateTitle(String sessionId, String title) {
        //更新数据
        super.lambdaUpdate()
                // 设置更新条件, 更新字段为title(最多设置前100个字符)，更新条件为sessionId和userId
                .set(ChatSession::getTitle, StrUtil.sub(title, 0, 100))
                .eq(ChatSession::getUserId, UserContext.getUser())
                .eq(ChatSession::getSessionId, sessionId)
                .update();
    }

    /**
     * 删除历史会话
     *
     * @param sessionId 会话id
     */
    @Override
    public void deleteHistorySession(String sessionId) {
        //1. 删除chatSession
        LambdaQueryWrapper<ChatSession> queryWrapper = Wrappers.<ChatSession>lambdaQuery().eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, UserContext.getUser());
        super.remove(queryWrapper);
        //2. 删除会话记忆（chatMemory）
        String conversationId = ChatService.getConversationId(sessionId);
        chatMemory.clear(conversationId);
    }

    /**
     * 查询历史对话
     *
     * @return 分组展示
     */
    @Override
    public Map<String, List<ChatSessionVO>> queryHistorySession() {
        //获取当前用户信息
        Long userId = UserContext.getUser();
        List<ChatSession> chatSessionList = super.lambdaQuery()
                .eq(ChatSession::getUserId, userId)
                .isNotNull(ChatSession::getTitle) //标题不为空
                .orderByDesc(ChatSession::getUpdateTime) //排序
                .last("Limit 30")
                .list();
        if (CollUtils.isEmpty(chatSessionList)) {
            log.info("没有发现该用户的对话信息: {}", userId);
            return Map.of();
        }
        //转换为VO
        List<ChatSessionVO> chatSessionVOList = chatSessionList.stream().map(chatSession -> ChatSessionVO.builder()
                .sessionId(chatSession.getSessionId())
                .title(chatSession.getTitle())
                .updateTime(chatSession.getUpdateTime())
                .build()
        ).toList();

        //按照时间分组，当天，30天 ，一年，一年以上
        final var TODAY = "当天";
        final var LAST_30_DAYS = "最近30天";
        final var LAST_YEAR = "最近1年";
        final var MORE_THAN_YEAR = "1年以上";
        //分组数据
        LocalDate now = LocalDateTime.now().toLocalDate();
        return chatSessionVOList.stream().collect(Collectors.groupingBy(
                chatSessionVO -> {
                    LocalDate localDate = chatSessionVO.getUpdateTime().toLocalDate();
                    //计算时间差
                    long between = Math.abs(ChronoUnit.DAYS.between(localDate, now));
                    if (between == 0) {
                        return TODAY;
                    } else if (between <= 30) {
                        return LAST_30_DAYS;
                    } else if (between <= 365) {
                        return LAST_YEAR;
                    } else {
                        return MORE_THAN_YEAR;
                    }
                }
        ));
    }

    /**
     * 更新会话标题 (异步方法)
     *
     * @param sessionId 会话id
     * @param title     会话标题
     * @param userId    用户id
     */
    @Override
    @Async
    public void update(String sessionId, String title, Long userId) {
        //先查询会话信息,一个sessionId
        ChatSession chatSession = super.lambdaQuery()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .one();
        if (Objects.isNull(chatSession)) {
            return;
        }
        //只有当第一次的时候更新标题
        if (Strings.isBlank(chatSession.getTitle()) && Strings.isNotBlank(title)) {
            //截取100个数据
            chatSession.setTitle(StrUtil.sub(title, 0, 100));
        }
        chatSession.setUpdateTime(LocalDateTime.now());
        super.updateById(chatSession);
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
                .map(message -> {
                    //大模型返回消息
                    if (message instanceof MyAssistantMessage myAssistantMessage) {
                        return MessageVO.builder()
                                .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                                .params(myAssistantMessage.getParams())
                                .content(message.getText())
                                .build();
                    }
                    //普通消息
                    return MessageVO.builder()
                            .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                            .content(message.getText())
                            .build();
                }).toList();
    }


    @Override
    public List<SessionVO.Example> hotExamples(Integer num) {
        return RandomUtil.randomEleList(sessionProperties.getExamples(), num);
    }
}
