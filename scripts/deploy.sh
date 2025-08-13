#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$HOME/melog"
REPO_URL="https://github.com/team-melog/melog-be.git"
REPO_DIR="$APP_DIR/melog-be"
COMPOSE="docker compose"     # v1이면 아래 주석 해제하여 자동 대체
command -v docker-compose >/dev/null 2>&1 && COMPOSE="docker-compose"

echo "🚀 Melog 배포 시작..."

# 환경변수 디버깅 및 기본값 설정
echo "🔍 환경변수 확인:"
echo "DB_HOST: ${DB_HOST:-'NOT SET'}"
echo "DB_PORT: ${DB_PORT:-'NOT SET'}"
echo "POSTGRES_DB: ${POSTGRES_DB:-'NOT SET'}"
echo "POSTGRES_USER: ${POSTGRES_USER:-'NOT SET'}"
echo "POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-'NOT SET'}"
echo "DB_SSLMODE: ${DB_SSLMODE:-'NOT SET'}"

# DB_SSLMODE 기본값 설정 (비어있을 경우)
if [ -z "${DB_SSLMODE:-}" ]; then
    echo "⚠️  DB_SSLMODE가 설정되지 않음. 기본값 'require'로 설정"
    export DB_SSLMODE="require"
fi

echo "🔧 .env 생성 (managed DB)"
cat > .env <<EOF
SPRING_PROFILES_ACTIVE=prod
DB_HOST=${DB_HOST}
DB_PORT=${DB_PORT}
POSTGRES_DB=${POSTGRES_DB}
POSTGRES_USER=${POSTGRES_USER}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
DB_SSLMODE=${DB_SSLMODE}
CLOVA_SPEECH_CLIENT_ID=${CLOVA_SPEECH_CLIENT_ID}
CLOVA_SPEECH_CLIENT_SECRET=${CLOVA_SPEECH_CLIENT_SECRET}
CLOVA_STUDIO_API_KEY=${CLOVA_STUDIO_API_KEY}
EOF

# .env 파일 내용 확인
echo "📄 생성된 .env 파일 내용:"
cat .env

# 혹시 남아있는 고아 컨테이너/네트워크 정리
echo "🧹 고아 컨테이너/네트워크 정리..."
$COMPOSE -f docker-compose.prod.yml down --remove-orphans || true

echo "🔨 새 이미지 빌드 및 실행..."
$COMPOSE -f docker-compose.prod.yml up -d --build

echo "⏳ 기동 대기..."
sleep 10

echo "📊 컨테이너 상태 확인..."
$COMPOSE -f docker-compose.prod.yml ps

echo "🏥 헬스체크..."
if curl -fsS http://localhost:8080/actuator/health >/dev/null; then
  echo "✅ 애플리케이션 기동 OK"
else
  echo "❌ 헬스체크 실패. 앱 로그:"
  $COMPOSE -f docker-compose.prod.yml logs --no-color app || true
  exit 1
fi

echo "🎉 배포 완료!"
echo "📱 http://$(curl -s ifconfig.me):8080"
