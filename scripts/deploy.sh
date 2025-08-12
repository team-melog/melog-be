#!/bin/bash

# Melog 배포 스크립트
# GitHub Actions에서 자동으로 실행됨

set -e  # 에러 발생 시 스크립트 중단

echo "🚀 Melog 배포 시작..."

# 애플리케이션 디렉토리로 이동
cd ~/melog

# 최신 코드 가져오기
echo "📥 최신 코드 가져오기..."
git pull origin main

# 환경변수 파일 생성
echo "🔧 환경변수 파일 생성..."
cat > .env << 'EOF'
# Production Environment Variables

# PostgreSQL Database Configuration
POSTGRES_DB=${POSTGRES_DB}
POSTGRES_USER=${POSTGRES_USER}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}

# Spring Boot Configuration
SPRING_PROFILES_ACTIVE=prod

# Docker Hub Configuration
DOCKERHUB_USERNAME=melog-be_devcontainer
IMAGE_TAG=latest

# Clova API Configuration
CLOVA_SPEECH_CLIENT_ID=${CLOVA_SPEECH_CLIENT_ID}
CLOVA_SPEECH_CLIENT_SECRET=${CLOVA_SPEECH_CLIENT_SECRET}
CLOVA_STUDIO_API_KEY=${CLOVA_STUDIO_API_KEY}
EOF

# 기존 컨테이너 중지 및 제거
echo "🛑 기존 컨테이너 중지..."
docker-compose -f docker-compose.prod.yml down

# 새 이미지로 빌드 및 실행
echo "🔨 새 이미지 빌드 및 실행..."
docker-compose -f docker-compose.prod.yml up -d --build

# 배포 상태 확인
echo "⏳ 배포 완료 대기 중..."
sleep 15

# 컨테이너 상태 확인
echo "📊 컨테이너 상태 확인..."
docker-compose -f docker-compose.prod.yml ps

# 헬스체크
echo "🏥 헬스체크 수행..."
if curl -f http://localhost:8080/actuator/health; then
    echo "✅ 애플리케이션이 정상적으로 실행되고 있습니다!"
    
    # 스키마가 생성되었는지 확인
    echo "🔍 데이터베이스 스키마 확인 중..."
    if docker exec melog-postgres-prod psql -U $POSTGRES_USER -d $POSTGRES_DB -c "\dt" | grep -q "users"; then
        echo "✅ 데이터베이스 스키마가 정상적으로 생성되었습니다!"
        echo "🔄 이제 JPA 설정을 validate로 전환합니다..."
        
        # application.yml을 validate로 수정
        sed -i 's/ddl-auto: update/ddl-auto: validate/' src/main/resources/application.yml
        
        # 애플리케이션 재시작
        echo "🔄 애플리케이션 재시작 중..."
        docker-compose -f docker-compose.prod.yml restart app
        
        echo "✅ JPA 설정이 validate로 전환되었습니다!"
    else
        echo "⚠️ 데이터베이스 스키마가 아직 생성되지 않았습니다. update 모드로 유지합니다."
    fi
else
    echo "❌ 헬스체크 실패. 로그를 확인해주세요."
    docker-compose -f docker-compose.prod.yml logs app
    exit 1
fi

echo "🎉 Melog 배포 완료!"
echo "📱 애플리케이션 URL: http://$(curl -s ifconfig.me):8080"
