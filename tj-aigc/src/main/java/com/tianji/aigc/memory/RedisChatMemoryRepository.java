package com.tianji.aigc.memory;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONUtil;
import com.tianji.aigc.util.MessageUtil;
import com.tianji.common.utils.CollUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

/**
 * Redis实现对话记忆
 */
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private final String DEFAULT_PREFIX = "CHAT:";
    private final String prefix;

    // 注入spring redis模板，进行redis的操作
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public RedisChatMemoryRepository() {
        this.prefix = DEFAULT_PREFIX;
    }

    public RedisChatMemoryRepository(String prefix) {
        this.prefix = prefix;
    }


    @Override
    public List<String> findConversationIds() {
        Set<String> keys = stringRedisTemplate.keys(this.prefix + "*");
        if (CollUtils.isEmpty(keys)) {
            return List.of();
        }
        return keys.stream().map(key -> key.replace(this.prefix, "")).toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        //查询数据并反序列化
        String key = getKey(conversationId);
        List<String> messages = stringRedisTemplate.boundListOps(key).range(0, -1);
        return CollStreamUtil.toList(messages, MessageUtil::toMessage);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.notEmpty(messages, "消息列表不能为空");
        //获取redis key
        String key = getKey(conversationId);
        // 保存数据时，会传入全部的消息数据，包括之前的数据，所以需要先删除之前的数据，再添加新的数据
        this.deleteByConversationId(conversationId);
        //把数据保存到redis中
        messages.forEach(message -> {
            stringRedisTemplate.boundListOps(key).rightPush(MessageUtil.toJson(message));
        });
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        String key = getKey(conversationId);
        stringRedisTemplate.delete(key);
    }

    /**
     * 获取Redis key
     *
     * @param conversationId
     * @return
     */
    private String getKey(String conversationId) {
        return prefix + conversationId;
    }
}
