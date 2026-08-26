package com.tianji.aigc.controller;

import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.annotations.NoWrapper;
import com.tianji.aigc.dto.ChatDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "对话管理")
public class ChatController {

    private final ChatService chatService;

    /**
     * 流式输出
     *
     * @param chatDTO
     * @return
     */
    @NoWrapper //输出格式不被包装
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)//SSE 流式输出
    public Flux<ChatEventVO> chat(@RequestBody @Valid ChatDTO chatDTO) {
        return chatService.chat(chatDTO.getSessionId(), chatDTO.getQuestion());
    }

    @PostMapping("/stop")
    public void stop(@RequestParam("sessionId") String sessionId) {
        chatService.stop(sessionId);
    }
}
