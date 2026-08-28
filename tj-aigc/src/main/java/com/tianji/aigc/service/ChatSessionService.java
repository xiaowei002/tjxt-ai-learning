package com.tianji.aigc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.vo.ChatSessionVO;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;

import java.util.List;
import java.util.Map;

public interface ChatSessionService extends IService<ChatSession> {

    /**
     * 创建会话session
     *
     * @param num 热门问题的数量
     * @return 会话信息
     */
    SessionVO createSession(Integer num);

    /**
     * 获取热门问题
     *
     * @param num
     * @return
     */
    List<SessionVO.Example> hotExamples(Integer num);

    /**
     * 获取对话详情
     *
     * @param sessionId
     * @return
     */
    List<MessageVO> queryBySessionId(String sessionId);

    /**
     * 更新session 标题
     *
     * @param sessionId 会话id
     * @param title     会话标题
     * @param userId    用户id
     */
    void update(String sessionId, String title, Long userId);

    /**
     * 查询历史对话
     *
     * @return 分组展示
     */
    Map<String, List<ChatSessionVO>> queryHistorySession();

    /**
     * 删除历史对话
     *
     * @param sessionId 会话id
     */
    void deleteHistorySession(String sessionId);

    /**
     * 更新历史对话标题
     *
     * @param sessionId 会话信息
     * @param title     标题
     */
    void updateTitle(String sessionId, String title);
}
