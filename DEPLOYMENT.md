# 🚀 Melog 배포 가이드

## 📋 배포 개요

이 가이드는 NCloud 인스턴스에서 Melog 애플리케이션을 Docker를 사용해 배포하는 방법을 설명합니다.

## 🏗️ 아키텍처

```
┌─────────────────┐    ┌─────────────────┐
│   Spring Boot   │    │   PostgreSQL    │
│   Application   │◄──►│    Database     │
│   (Port 8080)   │    │   (Port 5432)   │
└─────────────────┘    └─────────────────┘
        │                        │
        └────────────────────────┘
                   │
           ┌───────▼───────┐
           │ Docker Network │
           └───────────────┘
```

## 🛠️ 사전 준비사항

### NCloud 인스턴스 요구사항
- **OS**: Ubuntu 20.04 LTS 이상
- **CPU**: 2 Core 이상
- **Memory**: 4GB 이상  
- **Storage**: 20GB 이상
- **Network**: 8080, 5432 포트 개방

### 필수 소프트웨어 설치

```bash
# 시스템 업데이트
sudo apt update && sudo apt upgrade -y

# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Docker 서비스 시작 및 부팅시 자동 시작 설정
sudo systemctl start docker
sudo systemctl enable docker

# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER
newgrp docker

# Git 설치 (소스코드 다운로드용)
sudo apt install -y git
```

## 📦 배포 방법

### 방법 1: Git Clone 후 직접 빌드 (권장)

```bash
# 1. 소스코드 클론
git clone <your-repository-url> melog
cd melog

# 2. 환경변수 설정
cp env.example .env
nano .env  # 환경변수 수정

# 3. Docker 이미지 빌드 및 실행
docker-compose -f docker-compose.prod.yml up -d --build

# 4. 로그 확인
docker-compose -f docker-compose.prod.yml logs -f
```

### 방법 2: Docker Hub 이미지 사용

```bash
# 1. docker-compose.prod.yml 다운로드
wget <your-repository-url>/docker-compose.prod.yml

# 2. 환경변수 파일 생성
cat > .env << EOF
POSTGRES_DB=melog
POSTGRES_USER=melog
POSTGRES_PASSWORD=your_secure_password_here
SPRING_PROFILES_ACTIVE=prod
EOF

# 3. docker-compose.prod.yml에서 image 라인 수정
# build 섹션을 주석처리하고 image 라인 활성화:
# image: your-dockerhub-username/melog:latest

# 4. 애플리케이션 실행
docker-compose -f docker-compose.prod.yml up -d
```

## 🔧 환경변수 설정

`.env` 파일에서 다음 변수들을 설정하세요:

```bash
# PostgreSQL Database Configuration
POSTGRES_DB=melog
POSTGRES_USER=melog
POSTGRES_PASSWORD=your_secure_password_here

# Spring Boot Configuration
SPRING_PROFILES_ACTIVE=prod
```

## 🔍 배포 확인

### 1. 컨테이너 상태 확인
```bash
docker-compose -f docker-compose.prod.yml ps
```

### 2. 애플리케이션 헬스체크
```bash
curl http://localhost:8080/actuator/health
```

### 3. API 테스트
```bash
# 사용자 등록 테스트
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"nickname": "testuser"}'

# 사용자 조회 테스트
curl http://localhost:8080/api/users/testuser
```

## 🔄 운영 명령어

### 서비스 시작/중지/재시작
```bash
# 시작
docker-compose -f docker-compose.prod.yml up -d

# 중지
docker-compose -f docker-compose.prod.yml down

# 재시작
docker-compose -f docker-compose.prod.yml restart

# 특정 서비스만 재시작
docker-compose -f docker-compose.prod.yml restart app
```

### 로그 확인
```bash
# 전체 로그
docker-compose -f docker-compose.prod.yml logs

# 실시간 로그
docker-compose -f docker-compose.prod.yml logs -f

# 특정 서비스 로그
docker-compose -f docker-compose.prod.yml logs app
```

### 업데이트
```bash
# 소스코드 업데이트
git pull origin main

# 이미지 재빌드 및 재배포
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d --build
```

## 🗄️ 데이터 백업

### PostgreSQL 백업
```bash
# 백업
docker exec melog-postgres-prod pg_dump -U melog melog > melog_backup_$(date +%Y%m%d_%H%M%S).sql

# 복원
docker exec -i melog-postgres-prod psql -U melog melog < melog_backup.sql
```

## 🔐 보안 고려사항

1. **방화벽 설정**: 필요한 포트만 개방
2. **PostgreSQL 패스워드**: 강력한 패스워드 사용
3. **정기 업데이트**: OS 및 Docker 이미지 정기 업데이트
4. **로그 모니터링**: 애플리케이션 및 시스템 로그 모니터링

## 🔧 트러블슈팅

### 일반적인 문제들

#### 1. 포트 충돌
```bash
# 포트 사용 확인
sudo netstat -tlnp | grep :8080

# 프로세스 종료
sudo kill -9 <PID>
```

#### 2. 메모리 부족
```bash
# 메모리 사용량 확인
free -h

# Docker 시스템 정리
docker system prune -a
```

#### 3. 데이터베이스 연결 실패
```bash
# PostgreSQL 컨테이너 로그 확인
docker logs melog-postgres-prod

# 네트워크 확인
docker network ls
docker network inspect melog_network
```

## 📊 모니터링

### 시스템 리소스 모니터링
```bash
# CPU, 메모리 사용량
docker stats

# 디스크 사용량
df -h
```

### 애플리케이션 모니터링
- Health Check: `http://your-server:8080/actuator/health`
- Metrics: `http://your-server:8080/actuator/metrics`

## 📞 지원

문제가 발생하면 다음을 확인하세요:
1. 애플리케이션 로그
2. PostgreSQL 로그  
3. Docker 컨테이너 상태
4. 시스템 리소스 상태

배포에 성공하시면 이제 `http://your-server-ip:8080`에서 Melog 애플리케이션을 사용할 수 있습니다! 🎉
