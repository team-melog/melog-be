#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$HOME/melog"
REPO_URL="https://github.com/team-melog/melog-be.git"
REPO_DIR="$APP_DIR/melog-be"
COMPOSE="docker compose"     # v1이면 아래 주석 해제하여 자동 대체
command -v docker-compose >/dev/null 2>&1 && COMPOSE="docker-compose"

echo "🚀 Melog 배포 시작..."

# 환경변수 디버깅
echo "🔍 환경변수 확인:"
echo "DB_HOST: ${DB_HOST:-'NOT SET'}"
echo "DB_PORT: ${DB_PORT:-'NOT SET'}"
echo "POSTGRES_DB: ${POSTGRES_DB:-'NOT SET'}"
echo "POSTGRES_USER: ${POSTGRES_USER:-'NOT SET'}"
echo "POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-'NOT SET'}"
echo "CLOVA_SPEECH_CLIENT_ID=${CLOVA_SPEECH_CLIENT_ID:-'NOT SET'}"
echo "CLOVA_SPEECH_CLIENT_SECRET=${CLOVA_SPEECH_CLIENT_SECRET:-'NOT SET'}"
echo "CLOVA_STUDIO_API_KEY=${CLOVA_STUDIO_API_KEY:-'NOT SET'}"
echo "CLOVA_APP_CLIENT_ID=${CLOVA_APP_CLIENT_ID:-'NOT SET'}"
echo "CLOVA_APP_CLIENT_SECRET=${CLOVA_APP_CLIENT_SECRET:-'NOT SET'}"
echo "NCLOUD_ACCESS_KEY=${NCLOUD_ACCESS_KEY:-'NOT SET'}"
echo "NCLOUD_SECRET_KEY=${NCLOUD_SECRET_KEY:-'NOT SET'}"
echo "NCLOUD_S3_ENDPOINT=${NCLOUD_S3_ENDPOINT:-'NOT SET'}"
echo "NCLOUD_S3_REGION=${NCLOUD_S3_REGION:-'NOT SET'}"
echo "NCLOUD_S3_BUCKET=${NCLOUD_S3_BUCKET:-'NOT SET'}"

echo "DUCKDNS_TOKEN=${DUCKDNS_TOKEN:-'NOT SET'}"

# 환경변수 파일 생성
cat <<EOF > .env
DB_HOST=${DB_HOST}
DB_PORT=${DB_PORT}
POSTGRES_DB=${POSTGRES_DB}
POSTGRES_USER=${POSTGRES_USER}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
CLOVA_SPEECH_CLIENT_ID=${CLOVA_SPEECH_CLIENT_ID}
CLOVA_SPEECH_CLIENT_SECRET=${CLOVA_SPEECH_CLIENT_SECRET}
CLOVA_STUDIO_API_KEY=${CLOVA_STUDIO_API_KEY}
CLOVA_APP_CLIENT_ID=${CLOVA_APP_CLIENT_ID}
CLOVA_APP_CLIENT_SECRET=${CLOVA_APP_CLIENT_SECRET}
NCLOUD_ACCESS_KEY=${NCLOUD_ACCESS_KEY}
NCLOUD_SECRET_KEY=${NCLOUD_SECRET_KEY}
NCLOUD_S3_ENDPOINT=${NCLOUD_S3_ENDPOINT}
NCLOUD_S3_REGION=${NCLOUD_S3_REGION}
NCLOUD_S3_BUCKET=${NCLOUD_S3_BUCKET}

DUCKDNS_TOKEN=${DUCKDNS_TOKEN}
EOF

# 혹시 남아있는 고아 컨테이너/네트워크 정리
echo "🧹 고아 컨테이너/네트워크 정리..."
$COMPOSE -f docker-compose.prod.yml down --remove-orphans || true

# Docker 캐시 정리 (용량 부족 방지)
echo "🧹 Docker 캐시 정리 중..."
docker system prune -f 2>/dev/null || true

echo "🔨 새 이미지 빌드 및 실행..."
$COMPOSE -f docker-compose.prod.yml --env-file .env up -d --build

echo "⏳ 기동 대기..."
sleep 10

echo "📊 컨테이너 상태 확인..."
$COMPOSE -f docker-compose.prod.yml ps

echo "🏥 헬스체크..."
if curl -fsS -k https://localhost/actuator/health >/dev/null; then
  echo "✅ 애플리케이션 기동 OK (HTTPS)"
else
  echo "❌ HTTPS 헬스체크 실패. 앱 로그:"
  $COMPOSE -f docker-compose.prod.yml logs --no-color app || true
  exit 1
fi

echo "🎉 배포 완료!"

# HTTPS 완료 메시지
echo "🔒 HTTPS 설정 완료!"
echo "📱 HTTPS: https://$(curl -s ifconfig.me):443"
echo "🌐 도메인: https://melog508.duckdns.org"
echo "💡 Load Balancer: https://49.50.134.32"
