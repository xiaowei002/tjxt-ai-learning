package com.tianji.aigc.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;


@Slf4j
//@Getter
@Configuration
@RequiredArgsConstructor
public class SystemPromptConfig {

    private final NacosConfigManager nacosConfigManager;
    private final AIProperties aiProperties;

    // 使用原子引用，保证线程安全
    private final AtomicReference<String> chatSystemMessage = new AtomicReference<>();

    public String getChatSystemMessage() {
        return chatSystemMessage.get();
    }

    @PostConstruct
    public void init() {
        // 读取配置文件
        loadConfig(aiProperties.getSystem().getChat(), chatSystemMessage);
    }

    private void loadConfig(AIProperties.System.Chat chat, AtomicReference<String> chatSystemMessage) {
        try {
            //获取配置信息
            String dataId = chat.getDataId();
            String group = chat.getGroup();
            long timeoutMs = chat.getTimeoutMs();
            //从nacos读取配置
            String config = nacosConfigManager.getConfigService().getConfig(dataId, group, timeoutMs);
            log.info("配置读取成功， config: {}", config);

            //设置到chatMessage
            chatSystemMessage.set(config);

            //热更新
            nacosConfigManager.getConfigService().addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    chatSystemMessage.set(configInfo);
                    log.info("配置更新成功， config: {}", configInfo);
                }
            });
        } catch (NacosException e) {
            log.error("配置读取失败：错误信息：{}", e.getMessage(), e);
        }
    }

}
