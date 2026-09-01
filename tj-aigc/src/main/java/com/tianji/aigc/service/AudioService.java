package com.tianji.aigc.service;

import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

public interface AudioService {

    /**
     * tts
     * 文本转语音
     *
     * @param text 文本
     * @return 语音
     */
    ResponseBodyEmitter ttsStream(String text);
}
