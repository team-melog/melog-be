package com.melog.melog.clova.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VoiceType {

    ARA("vara", "아라"),
    MIKYUNG("vmikyung", "미경"),
    DAIN("vdain", "다인"),
    YUNA("vyuna", "유나"),
    GOEUN("vgoeun", "고은"),
    DAESUNG("vdaeseong", "대성");

    private final String voiceKey;
    private final String voiceName;

}
