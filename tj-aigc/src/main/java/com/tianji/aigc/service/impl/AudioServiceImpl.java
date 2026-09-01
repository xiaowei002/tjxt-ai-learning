package com.tianji.aigc.service.impl;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.tianji.aigc.service.AudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AudioServiceImpl implements AudioService {


    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;
    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

    /**
     * stt
     *
     * @param audioFile 语音
     * @return 文本
     */
    @Override
    public String stt(MultipartFile audioFile) {
        // 将MultipartFile转换为Resource
        Resource audioResource = audioFile.getResource();
        AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioResource);
        // 调用OpenAiAudioTranscriptionModel进行语音识别
        AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(transcriptionRequest);
        // 获取识别结果
        String output = response.getResult().getOutput();
        // 将繁体转换为简体
        return ZhConverterUtil.toSimple(output);
    }

    /**
     * tts
     *
     * @param text 文本
     * @return 语音
     */
    @Override
    public ResponseBodyEmitter ttsStream(String text) {
        ResponseBodyEmitter responseBodyEmitter = new ResponseBodyEmitter();
        log.info("开始语音合成, 文本内容：{}", text);
        SpeechPrompt speechPrompt = new SpeechPrompt(text);
        //调用openai转换
        Flux<SpeechResponse> stream = openAiAudioSpeechModel.stream(speechPrompt);
        //订阅响应流并发送数据
        stream.subscribe(speechResponse -> {
                    byte[] output = speechResponse.getResult().getOutput();
                    try {
                        responseBodyEmitter.send(output);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                responseBodyEmitter::completeWithError,
                responseBodyEmitter::complete);
        return responseBodyEmitter;
    }
}
