package com.melog.melog.clova.domain.model.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClovaStudioChatRequest {
    private List<Message> messages;
    private Integer maxTokens;
    private Double temperature;
    private Double topP;
    private Integer topK;
    private Boolean stream;
    private List<String> stop;
    private String serviceId;
    private String modelName;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role; // "user", "assistant", "system"
        private String content;
    }
}
