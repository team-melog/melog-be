# 멀티스테이지 빌드를 사용한 Production용 Dockerfile

# Stage 1: Build Stage
FROM mcr.microsoft.com/devcontainers/java:17 AS builder

WORKDIR /app

# Gradle 캐시를 위해 gradle 파일들 먼저 복사
COPY gradle/ gradle/
COPY gradlew .
COPY gradlew.bat .
COPY gradle.properties* ./
COPY settings.gradle .
COPY build.gradle .

# 의존성 다운로드 (캐시 활용)
RUN ./gradlew --no-daemon dependencies

# 소스 코드 복사
COPY src/ src/

# 애플리케이션 빌드 (테스트 제외)
RUN ./gradlew --no-daemon clean bootJar -x test

# Stage 2: Runtime Stage
FROM mcr.microsoft.com/devcontainers/java:17

# Microsoft devcontainer 이미지에는 curl과 필요한 도구들이 이미 포함되어 있음

# 애플리케이션 실행을 위한 사용자 생성
RUN addgroup --system spring && adduser --system spring --ingroup spring

# 애플리케이션 디렉토리 생성
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 사용자 권한 변경
RUN chown -R spring:spring /app
USER spring

# 포트 노출
EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM 최적화 옵션과 함께 애플리케이션 실행
ENTRYPOINT ["java", \
    "-server", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-XX:+UseG1GC", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", \
    "app.jar"]
