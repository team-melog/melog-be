# Clova Domain

NCloud의 Clova Studio와 Clova Speech Recognition (CSR) 기능을 사용하기 위한 헥사고날 아키텍처 기반 도메인입니다.

## 구조

```
clova/
├── domain/                    # 도메인 계층
│   ├── ClovaApiHistory.java   # API 호출 히스토리 엔티티
│   └── model/
│       ├── ClovaConfig.java   # 설정 관리
│       ├── ClovaEndpoint.java # API 엔드포인트 정의
│       ├── ClovaProperties.java # API 속성
│       ├── request/           # 요청 DTO들
│       └── response/          # 응답 DTO들
├── application/               # 애플리케이션 계층
│   ├── port/
│   │   ├── in/               # 인바운드 포트 (유스케이스)
│   │   └── out/              # 아웃바운드 포트
│   └── service/              # 애플리케이션 서비스
└── adapter/                  # 어댑터 계층
    ├── external/out/         # 외부 시스템 어댑터
    │   ├── ClovaApiAdapter.java      # 공통 API 어댑터 (RestTemplateUtil 사용)
    │   ├── ClovaStudioAdapter.java   # Studio 전용 어댑터
    │   └── ClovaSpeechAdapter.java   # Speech 전용 어댑터
    ├── in/web/               # 웹 어댑터 (컨트롤러)
    └── persistence/          # 영속성 어댑터
```

## 주요 기능

### 1. Clova Studio
- **대화형 AI**: `STUDIO_CHAT` - ChatGPT와 유사한 대화형 AI
- **텍스트 생성**: `STUDIO_TEXT` - 텍스트 생성 및 완성
- **모델 정보 조회**: `STUDIO_MODEL_INFO` - 특정 모델 정보 조회 (Path Variable 사용)

### 2. Clova Speech
- **음성 인식**: `SPEECH_STT` - 음성을 텍스트로 변환
- **음성 합성**: `SPEECH_TTS` - 텍스트를 음성으로 변환

## 설정

`application-clova.yml` 파일에서 각 API의 설정을 관리합니다:

```yaml
clova:
  config:
    STUDIO_CHAT:
      url: "https://api.clovastudio.com"
      apiKey: "${CLOVA_STUDIO_API_KEY}"
      apiKeyId: "${CLOVA_STUDIO_API_KEY_ID}"
      serviceId: "${CLOVA_STUDIO_SERVICE_ID}"
      modelName: "clova-x1-5b"
      maxTokens: 1000
      temperature: 0.7
```

## 사용 예시

### 감정 분석
```java
@Autowired
private AnalyzeSentimentUseCase analyzeSentimentUseCase;

AnalyzeSentimentRequest request = new AnalyzeSentimentRequest();
request.setText("오늘은 정말 좋은 날씨네요!");
AnalyzeSentimentResponse response = analyzeSentimentUseCase.execute(request);
```

### 음성 인식
```java
@Autowired
private SpeechSttUseCase speechSttUseCase;

SpeechSttRequest request = SpeechSttRequest.builder()
    .audioData("base64_encoded_audio_data")
    .audioFormat("wav")
    .language("ko")
    .diarization(false)
    .build();
SpeechSttResponse response = speechSttUseCase.execute(request);
```

### Studio API 직접 사용
```java
@Autowired
private ClovaStudioPort clovaStudioPort;

// 대화형 AI
ClovaStudioChatResponse chatResponse = clovaStudioPort.sendChatRequest(request);

// 텍스트 생성
ClovaStudioChatResponse textResponse = clovaStudioPort.sendTextGenerationRequest(request);

// 모델 정보 조회
ClovaStudioChatResponse modelInfo = clovaStudioPort.getModelInfo("clova-x1-5b");
```

## 공통 어댑터

`ClovaApiAdapter`는 모든 Clova API 호출에 공통적으로 사용되는 어댑터입니다:

- **RestTemplateUtil 활용**: HTTP 요청의 공통 로직 위임
- **Clova 인증 관리**: 엔드포인트별 인증 전략 적용
- **설정 관리**: ClovaConfig를 통한 설정 관리
- **에러 처리**: Clova 특화 예외 처리

## 확장성

새로운 Clova API를 추가하려면:

1. `ClovaEndpoint`에 새로운 엔드포인트 추가
2. 요청/응답 DTO 생성
3. 포트 인터페이스에 메서드 추가
4. 어댑터에 구현 추가

이 구조를 통해 새로운 API를 쉽게 추가하고 기존 코드에 영향을 주지 않을 수 있습니다.
