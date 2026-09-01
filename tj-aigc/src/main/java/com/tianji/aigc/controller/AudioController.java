package com.tianji.aigc.controller;

import com.tianji.aigc.service.AudioService;
import com.tianji.common.annotations.NoWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

/**
 * tts controller
 *
 */
@RestController
@RequestMapping("/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioService audioService;

    /**
     * 文本转语音
     * @param text 文本
     * @return 语音数据
     */
    @NoWrapper
    @PostMapping(value = "/tts-stream", produces = "audio/mp3")
    public ResponseBodyEmitter ttsStream(@RequestBody String text) {
        return this.audioService.ttsStream(text);
    }

    /**
     * 语音转文字
     *
     * @param audioFile 语音文件
     * @return 文本
     */
    @PostMapping("/stt")
    public String stt(@RequestParam("audioFile") MultipartFile audioFile) {
        return this.audioService.stt(audioFile);
    }
}
