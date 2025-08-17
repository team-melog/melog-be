# Ncloud Storage 설정 가이드

이 문서는 Melog 프로젝트에서 Ncloud Storage를 사용하여 음성 파일을 저장하기 위한 설정 방법을 설명합니다.

## 1. Ncloud 계정 및 서비스 활성화

### 1.1 Ncloud 계정 생성
1. [Ncloud 웹사이트](https://www.ncloud.com/)에 접속
2. **회원가입** 또는 **로그인**
3. 본인인증 및 결제 수단 등록

### 1.2 Object Storage 서비스 활성화
1. Ncloud 콘솔에 로그인
2. **Storage > Object Storage** 메뉴로 이동
3. **서비스 신청** 클릭
4. 서비스 약관 동의 및 활성화

## 2. 버킷 생성 및 설정

### 2.1 버킷 생성
1. **Object Storage** 메뉴에서 **버킷 생성** 클릭
2. 버킷 설정:
   - **버킷명**: `melog-audio-files` (전역적으로 유니크해야 함)
   - **리전**: `KR` (한국)
   - **접근 제어**: `Private` (보안을 위해)
   - **버전 관리**: `비활성화` (필요시 활성화)
   - **암호화**: `기본 암호화` (권장)
3. **생성** 버튼 클릭

### 2.2 버킷 권한 설정
1. 생성된 버킷 클릭
2. **권한 관리** 탭으로 이동
3. **ACL 설정**에서 Private으로 설정 확인
4. **CORS 설정** (필요시):
   ```json
   [
     {
       "AllowedHeaders": ["*"],
       "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
       "AllowedOrigins": ["*"],
       "ExposeHeaders": ["ETag"]
     }
   ]
   ```

## 3. Access Key 생성

### 3.1 Sub Account 생성
1. **Management > Sub Account** 메뉴로 이동
2. **Sub Account 생성** 클릭
3. 계정 정보 입력:
   - **계정명**: `melog-storage-user`
   - **이메일**: 실제 이메일 주소
   - **권한**: Object Storage 관련 권한만 부여

### 3.2 API 인증키 생성
1. 생성된 Sub Account 클릭
2. **API 인증키 관리** 탭으로 이동
3. **Access Key ID 생성** 클릭
4. **Secret Access Key 생성** 클릭
5. 생성된 키들을 안전한 곳에 저장

## 4. 프로젝트 환경 변수 설정

### 4.1 .env 파일 생성
프로젝트 루트에 `.env` 파일을 생성하고 다음 내용을 추가:

```bash
# Ncloud Storage Configuration
NCLOUD_ACCESS_KEY=your-access-key-here
NCLOUD_SECRET_KEY=your-secret-key-here
NCLOUD_S3_ENDPOINT=https://kr.object.ncloudstorage.com
NCLOUD_S3_REGION=KR
NCLOUD_S3_BUCKET=melog-audio-files
```

### 4.2 환경 변수 확인
```bash
# 환경 변수가 제대로 설정되었는지 확인
echo $NCLOUD_ACCESS_KEY
echo $NCLOUD_SECRET_KEY
echo $NCLOUD_S3_BUCKET
```

## 5. 애플리케이션 설정

### 5.1 프로파일 설정
`application-ncloud.yml` 프로파일을 사용하여 Ncloud Storage 설정을 활성화:

```yaml
spring:
  profiles:
    active: ncloud
```

### 5.2 설정 파일 확인
`src/main/resources/application-ncloud.yml` 파일이 올바르게 설정되었는지 확인:

```yaml
ncloud:
  s3:
    access-key: ${NCLOUD_ACCESS_KEY:your-access-key}
    secret-key: ${NCLOUD_SECRET_KEY:your-secret-key}
    endpoint: ${NCLOUD_S3_ENDPOINT:https://kr.object.ncloudstorage.com}
    region: ${NCLOUD_S3_REGION:KR}
    bucket: ${NCLOUD_S3_BUCKET:melog-audio-files}
```

## 6. 테스트 및 검증

### 6.1 연결 테스트
1. 애플리케이션 실행
2. 로그에서 S3 클라이언트 초기화 확인
3. 음성 파일 업로드 테스트

### 6.2 파일 업로드 확인
1. Ncloud 콘솔에서 Object Storage 확인
2. 버킷 내 파일 구조 확인:
   ```
   melog-audio-files/
   ├── users/
   │   ├── {userId}/
   │   │   ├── audio/
   │   │   │   ├── 2024/
   │   │   │   │   ├── 01/
   │   │   │   │   │   └── {fileName}.mp3
   ```

## 7. 보안 고려사항

### 7.1 Access Key 보안
- Access Key와 Secret Key를 소스 코드에 하드코딩하지 마세요
- 환경 변수나 보안 관리 시스템을 사용하세요
- 정기적으로 키를 로테이션하세요

### 7.2 버킷 보안
- 버킷은 Private으로 설정하세요
- 필요한 경우에만 특정 IP에서 접근 허용
- 버킷 정책을 통해 세밀한 접근 제어 설정

### 7.3 파일 보안
- 민감한 파일은 암호화하여 저장
- 파일 접근 로그 활성화
- 정기적인 보안 감사 수행

## 8. 문제 해결

### 8.1 연결 오류
**문제**: S3 클라이언트 초기화 실패
**해결방법**:
1. Access Key와 Secret Key 확인
2. 네트워크 연결 상태 확인
3. 방화벽 설정 확인

### 8.2 업로드 실패
**문제**: 파일 업로드 실패
**해결방법**:
1. 버킷 권한 설정 확인
2. 파일 크기 제한 확인
3. CORS 설정 확인

### 8.3 권한 오류
**문제**: Access Denied 오류
**해결방법**:
1. Sub Account 권한 설정 확인
2. 버킷 ACL 설정 확인
3. IAM 정책 확인

## 9. 모니터링 및 로깅

### 9.1 로그 설정
```yaml
logging:
  level:
    com.melog.melog.common.service.S3FileService: DEBUG
    software.amazon.awssdk: DEBUG
```

### 9.2 메트릭 모니터링
- 파일 업로드/다운로드 성공률
- 응답 시간 모니터링
- 에러율 모니터링

## 10. 비용 최적화

### 10.1 스토리지 클래스
- **Standard**: 자주 접근하는 파일
- **Archive**: 장기 보관 파일
- **Deep Archive**: 장기 보관 및 거의 접근하지 않는 파일

### 10.2 수명 주기 관리
- 자동으로 오래된 파일을 저비용 스토리지로 이동
- 불필요한 파일 자동 삭제
- 버전 관리 비활성화 (비용 절약)

## 11. 백업 및 복구

### 11.1 백업 전략
- 중요 파일은 여러 리전에 복제
- 정기적인 백업 스케줄링
- 백업 파일 검증

### 11.2 재해 복구
- 크로스 리전 복제 설정
- RTO/RPO 목표 설정
- 복구 절차 문서화

## 12. 추가 리소스

- [Ncloud Object Storage 공식 문서](https://www.ncloud.com/product/storage/ncloudStorage)
- [AWS S3 호환 API 가이드](https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/welcome.html)
- [Spring Boot S3 연동 가이드](https://spring.io/guides/gs/uploading-files/)

---

이 가이드를 따라 설정하면 Melog 프로젝트에서 Ncloud Storage를 사용하여 음성 파일을 안전하고 효율적으로 저장할 수 있습니다.


