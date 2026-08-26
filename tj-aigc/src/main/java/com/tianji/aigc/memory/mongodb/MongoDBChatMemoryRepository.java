package com.tianji.aigc.memory.mongodb;

import cn.hutool.core.collection.CollStreamUtil;
import com.tianji.aigc.util.MessageUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Objects;

/***
 * 使用MongoDB实现对话记忆
 */
public class MongoDBChatMemoryRepository implements ChatMemoryRepository {

    @Resource
    private MongoTemplate mongoTemplate;


    @Override
    public List<String> findConversationIds() {
        List<ChatMemory> chatMemoryList = this.mongoTemplate.findAll(ChatMemory.class);
        return CollStreamUtil.toList(chatMemoryList, ChatMemory::getConversationId);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Query query = Query.query(Criteria.where("conversationId").is(conversationId));
        ChatMemory chatMemory = this.mongoTemplate.findOne(query, ChatMemory.class);
        if (Objects.isNull(chatMemory)) {
            return List.of();
        }
        return CollStreamUtil.toList(chatMemory.getMessages(), MessageUtil::toMessage);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        //先删除数据，
        this.deleteByConversationId(conversationId);
        //保存数据
        ChatMemory chatMemory = ChatMemory.builder()
                .conversationId(conversationId)
                .messages(messages.stream().map(MessageUtil::toJson).toList())
                .build();
        //保存
        this.mongoTemplate.save(chatMemory);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        Query query = Query.query(Criteria.where("conversationId").is(conversationId));
        this.mongoTemplate.remove(query, ChatMemory.class);
    }
}
