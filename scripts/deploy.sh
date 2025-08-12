#!/bin/bash

# Melog 배포 스크립트
# GitHub Actions에서 자동으로 실행됨

set -e  # 에러 발생 시 스크립트 중단

echo "🚀 Melog 배포 시작..."
rm -rf ~/melog
mkdir ~/melog
cd ~/melog

# 최신 코드 가져오기
echo "📥 최신 코드 가져오기..."
git clone https://github.com/team-melog/melog-be.git

echo "🔧 .env 생성 (managed DB)"
cat > .env <<EOF
SPRING_PROFILES_ACTIVE=prod
DB_HOST=${DB_HOST}
DB_PORT=${DB_PORT}
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
    
    # Flyway 마이그레이션 상태 확인
    echo "🔍 Flyway 마이그레이션 상태 확인..."
    if docker exec melog-app-prod psql -h localhost -U $POSTGRES_USER -d $POSTGRES_DB -c "\dt" | grep -q "flyway_schema_history"; then
        echo "✅ Flyway 마이그레이션이 정상적으로 실행되었습니다!"
    else
        echo "⚠️ Flyway 마이그레이션이 아직 실행되지 않았습니다."
    fi
    
    echo "✅ 배포가 완료되었습니다!"
else
    echo "❌ 헬스체크 실패. 로그를 확인해주세요."
    docker-compose -f docker-compose.prod.yml logs app
    exit 1
fi

echo "🎉 Melog 배포 완료!"
echo "📱 애플리케이션 URL: http://$(curl -s ifconfig.me):8080"
