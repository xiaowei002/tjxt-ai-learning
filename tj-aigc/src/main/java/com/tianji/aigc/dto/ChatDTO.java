package com.tianji.aigc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatDTO {
    @NotBlank(message = "问题不能为空")
    private String question;

    @NotBlank(message = "会话id不能为空")
    private String sessionId;
}
