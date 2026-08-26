package com.tianji.aigc.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.aigc.entity.ChatMemory;
import com.tianji.aigc.mapper.ChatMemoryMapper;
import com.tianji.aigc.util.MessageUtil;
import com.tianji.common.utils.CollUtils;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 使用mysql来实现 对话记忆
 */
public class MysqlChatMemoryRepository implements ChatMemoryRepository {

    @Resource
    private ChatMemoryMapper chatMemoryMapper;

    @Override
    public List<String> findConversationIds() {
        List<ChatMemory> chatMemories = chatMemoryMapper.selectObjs(new QueryWrapper<>());
        if (CollUtils.isEmpty(chatMemories)) {
            return List.of();
        }
        return chatMemories.stream().map(ChatMemory::getConversationId).toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        LambdaQueryWrapper<ChatMemory> queryWrapper = new LambdaQueryWrapper<>();

        List<ChatMemory> chatMemories = chatMemoryMapper.selectList(queryWrapper.eq(ChatMemory::getConversationId, conversationId));
        if (CollUtils.isEmpty(chatMemories)) {
            return List.of();
        }
        return chatMemories.stream().map(chatMemory -> MessageUtil.toMessage(chatMemory.getContent())).toList();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        //先删除旧的
        deleteByConversationId(conversationId);
        //构造新消息
        List<ChatMemory> list = messages.stream().map(message -> ChatMemory.builder().conversationId(conversationId)
                .content(MessageUtil.toJson(message))
                .build()).toList();
        //保存新消息
        chatMemoryMapper.insert(list);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        LambdaQueryWrapper<ChatMemory> queryWrapper = new LambdaQueryWrapper<>();
        chatMemoryMapper.delete(queryWrapper.eq(ChatMemory::getConversationId, conversationId));
    }
}
