package com.tianji.aigc.service;

import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import reactor.core.publisher.Flux;

public interface ChatService {
    /**
     * 聊天
     *
     * @param sessionId
     * @param question
     * @return
     */
    Flux<ChatEventVO> chat(String sessionId, String question);

    /**
     * 聊天停止
     *
     * @param sessionId
     * @return
     */
    void stop(String sessionId);


    /**
     * 构造 conversationId
     * @param sessionId
     * @return
     */
    static String getConversationId(String sessionId) {
        return UserContext.getUser().toString() + "_" + sessionId;
    }
}
