package com.melog.melog.clova.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class PromptMessage {
    MessangerType role;
    String content;

    public String toPrompt() {
        // SYSTEM/USER/ASSISTANT를 명령어 형태로 변환
        return role.name().toLowerCase() + " >> " + (content == null ? "" : content.strip());
    }
}
