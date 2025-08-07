# RestTemplateUtil

범용적인 HTTP 요청을 처리하는 유틸리티 클래스입니다. 재시도, 로깅, 에러 처리 등 공통 기능을 제공합니다.

## 주요 기능

### 1. 기본 HTTP 요청
```java
@Autowired
private RestTemplateUtil restTemplateUtil;

// 기본 POST 요청
MyResponse response = restTemplateUtil.sendRequest(
    "https://api.example.com/data",
    HttpMethod.POST,
    requestBody,
    MyResponse.class,
    headers
);
```

### 2. Path Variable 지원
```java
// Path Variable이 포함된 요청
Object[] pathVariables = {"userId", "123"};
MyResponse response = restTemplateUtil.sendRequestWithPathVariables(
    "https://api.example.com/users/{userId}/profile/{profileId}",
    HttpMethod.GET,
    null,
    MyResponse.class,
    headers,
    pathVariables
);
```

### 3. Query Parameter 지원
```java
// Query Parameter가 포함된 요청
Map<String, String> queryParams = new HashMap<>();
queryParams.put("page", "1");
queryParams.put("size", "10");
queryParams.put("sort", "name");

MyResponse response = restTemplateUtil.sendRequestWithQueryParams(
    "https://api.example.com/users/search",
    HttpMethod.GET,
    null,
    MyResponse.class,
    headers,
    queryParams
);
```

### 4. 커스텀 재시도 설정
```java
// 커스텀 재시도 설정
MyResponse response = restTemplateUtil.sendRequestWithCustomRetry(
    "https://api.example.com/data",
    HttpMethod.POST,
    requestBody,
    MyResponse.class,
    headers,
    null, // pathVariables
    5,    // maxRetryAttempts
    2000  // retryDelayMs
);
```

## 사용 예시

### Clova API 어댑터에서 사용
```java
@Component
public class ClovaApiAdapter {
    private final RestTemplateUtil restTemplateUtil;
    
    public <T> T sendRequest(ClovaEndpoint endpoint, Object requestBody, Class<T> responseType) {
        String url = buildUrl(endpoint);
        HttpHeaders headers = createHeaders(endpoint);
        
        return restTemplateUtil.sendRequest(url, endpoint.getMethod(), requestBody, responseType, headers);
    }
}
```

### User 도메인에서 외부 API 호출
```java
@Component
public class UserExternalApiAdapter {
    private final RestTemplateUtil restTemplateUtil;
    
    public ExternalUserInfoResponse getUserInfo(String userId) {
        Object[] pathVariables = {userId};
        
        return restTemplateUtil.sendRequestWithPathVariables(
            "https://api.external-service.com/users/{userId}",
            HttpMethod.GET,
            null,
            ExternalUserInfoResponse.class,
            createHeaders(),
            pathVariables
        );
    }
}
```

## 장점

1. **재사용성**: 모든 도메인에서 공통으로 사용 가능
2. **일관성**: 동일한 로깅, 에러 처리, 재시도 로직
3. **유지보수성**: HTTP 요청 로직이 한 곳에 집중
4. **확장성**: 새로운 HTTP 요청 방식 쉽게 추가 가능
5. **테스트 용이성**: Mock 객체 주입이 쉬움

## 에러 처리

`RestApiException`을 통해 HTTP 요청 실패를 처리합니다:

```java
try {
    MyResponse response = restTemplateUtil.sendRequest(url, method, body, responseType, headers);
} catch (RestTemplateUtil.RestApiException e) {
    // 에러 처리 로직
    logger.error("API call failed: {}", e.getMessage());
}
```
