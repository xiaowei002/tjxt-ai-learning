package com.tianji.aigc.memory.mongodb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("chat_memory")
public class ChatMemory {

    @Id
    private ObjectId id;

    @Indexed
    private String conversationId;

    //对话数据
    private List<String> messages;
}
