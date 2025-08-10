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
        return role.name().toLowerCase() + " >> " + (content == null ? "" : content.strip());
    }
}
