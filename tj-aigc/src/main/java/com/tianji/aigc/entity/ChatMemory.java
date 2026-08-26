package com.tianji.aigc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_memory")
public class ChatMemory implements Serializable {

    /**
     * 数据id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 会话id，userId_sessionId
     */
    private String conversationId;

    /**
     * 内容
     */
    private String content;
}
