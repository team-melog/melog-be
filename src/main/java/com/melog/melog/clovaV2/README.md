# ClovaV2 Domain

RestTemplateUtil을 활용한 새로운 Clova API 도메인입니다. Speech Recognition과 Studio 기능에 집중하여 깔끔하게 구성되었습니다.

## 구조

```
clovaV2/
├── domain/
│   └── model/
│       ├── ClovaV2Config.java           # 설정 관리
│       ├── request/                     # 요청 DTO들
│       │   ├── StudioChatRequest.java
│       │   └── SpeechRecognitionRequest.java
│       └── response/                    # 응답 DTO들
│           ├── StudioChatResponse.java
│           └── SpeechRecognitionResponse.java
├── application/
│   ├── port/
│   │   ├── in/                         # 인바운드 포트
│   │   │   └── ClovaV2UseCase.java
│   │   └── out/                        # 아웃바운드 포트
│   │       ├── ClovaV2StudioPort.java
│   │       └── ClovaV2SpeechPort.java
│   └── service/
│       └── ClovaV2Service.java
└── adapter/
    ├── external/out/                   # 외부 시스템 어댑터
    │   ├── ClovaV2StudioAdapter.java   # RestTemplateUtil 직접 사용
    │   └── ClovaV2SpeechAdapter.java   # RestTemplateUtil 직접 사용
    └── in/web/                         # 웹 어댑터
        └── ClovaV2Controller.java
```

## 주요 기능

### 1. Clova Studio
- **대화형 AI**: ChatGPT와 유사한 대화형 AI
- **엔드포인트**: `/v1/chat-completions`

### 2. Clova Speech
- **음성 인식**: 음성을 텍스트로 변환
- **엔드포인트**: `/v1/recognize`

## 설정

`application-clovaV2.yml` 파일에서 설정을 관리합니다:

```yaml
clova-v2:
  studio:
    base-url: "https://api.clovastudio.com"
    api-key: "${CLOVA_V2_STUDIO_API_KEY}"
    api-key-id: "${CLOVA_V2_STUDIO_API_KEY_ID}"
    service-id: "${CLOVA_V2_STUDIO_SERVICE_ID}"
    model-name: "clova-x1-5b"
    max-tokens: 1000
    temperature: 0.7
  speech:
    base-url: "https://naveropenapi.apigw.ntruss.com"
    api-key: "${CLOVA_V2_SPEECH_API_KEY}"
    api-key-id: "${CLOVA_V2_SPEECH_API_KEY_ID}"
    service-id: "${CLOVA_V2_SPEECH_SERVICE_ID}"
```

## 사용 예시

### Studio Chat API
```java
@Autowired
private ClovaV2UseCase clovaV2UseCase;

StudioChatRequest request = StudioChatRequest.builder()
    .messages(Arrays.asList(
        StudioChatRequest.Message.builder()
            .role("user")
            .content("안녕하세요!")
            .build()
    ))
    .maxTokens(100)
    .temperature(0.7)
    .build();

StudioChatResponse response = clovaV2UseCase.sendChatRequest(request);
```

### Speech Recognition API
```java
@Autowired
private ClovaV2UseCase clovaV2UseCase;

SpeechRecognitionRequest request = SpeechRecognitionRequest.builder()
    .audioData("base64_encoded_audio_data")
    .audioFormat("wav")
    .language("ko")
    .diarization(false)
    .build();

SpeechRecognitionResponse response = clovaV2UseCase.sendSpeechRecognitionRequest(request);
```

### REST API 호출
```bash
# Studio Chat
POST /api/v2/clova/studio/chat
{
  "messages": [
    {
      "role": "user",
      "content": "안녕하세요!"
    }
  ],
  "maxTokens": 100,
  "temperature": 0.7
}

# Speech Recognition
POST /api/v2/clova/speech/recognize
{
  "audioData": "base64_encoded_audio_data",
  "audioFormat": "wav",
  "language": "ko",
  "diarization": false
}
```

## 특징

1. **RestTemplateUtil 직접 사용**: 중간 어댑터 없이 RestTemplateUtil을 직접 활용
2. **단순한 구조**: 복잡한 계층 구조 없이 직관적인 설계
3. **설정 기반**: 모든 설정을 YAML 파일에서 관리
4. **헤더 자동화**: 각 API별 인증 헤더 자동 설정
5. **재시도 로직**: RestTemplateUtil의 재시도 기능 활용

## 환경변수 설정

```bash
# Studio API
CLOVA_V2_STUDIO_API_KEY=your_studio_api_key
CLOVA_V2_STUDIO_API_KEY_ID=your_studio_api_key_id
CLOVA_V2_STUDIO_SERVICE_ID=your_studio_service_id

# Speech API
CLOVA_V2_SPEECH_API_KEY=your_speech_api_key
CLOVA_V2_SPEECH_API_KEY_ID=your_speech_api_key_id
CLOVA_V2_SPEECH_SERVICE_ID=your_speech_service_id
```
