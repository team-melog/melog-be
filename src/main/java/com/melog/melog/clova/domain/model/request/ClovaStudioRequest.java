package com.melog.melog.clova.domain.model.request;


import java.util.List;

import com.melog.melog.clova.domain.model.PromptMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClovaStudioRequest {

    private String nickname;
    private List<PromptMessage> promptMessages; // 사용자 메시지 목록
    
}
