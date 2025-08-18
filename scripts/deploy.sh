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
DOMAIN_NAME=${DOMAIN_NAME:-melog508.duckdns.org}
CERTBOT_EMAIL=${CERTBOT_EMAIL:-kioplm0211@gmail.com}
EOF

# 혹시 남아있는 고아 컨테이너/네트워크 정리
echo "🧹 고아 컨테이너/네트워크 정리..."
$COMPOSE -f docker-compose.prod.yml down --remove-orphans || true

# Docker 캐시 정리 (용량 부족 방지)
echo "🧹 Docker 캐시 정리 중..."
docker system prune -f 2>/dev/null || true

# SSL 인증서 존재 확인 (기동 전 실사)
echo "🔐 SSL 인증서 사전 점검 중..."
DOMAIN="${DOMAIN_NAME:-melog508.duckdns.org}"
EMAIL="${CERTBOT_EMAIL:-kioplm0211@gmail.com}"
echo "🔍 사용할 도메인: $DOMAIN"
echo "📧 인증서 발급 이메일: $EMAIL"

# 1) DuckDNS 레코드 업데이트
echo "🦆 DuckDNS 레코드 업데이트 중..."
curl -s "https://www.duckdns.org/update?domains=${DOMAIN%%.*}&token=${DUCKDNS_TOKEN}&ip="
echo ""

# 2) 80 포트 비우기 (HTTP-01 검증을 위해)
echo "🔓 80 포트 비우기 중..."
# Docker 컨테이너로 실행 중인 서비스만 중지
docker ps --filter "publish=80" --format "{{.ID}}" | xargs -r docker stop || true
echo "✅ 80 포트 비움 완료"

# 3) SSL 인증서 발급 또는 갱신
echo "🔐 SSL 인증서 처리 중..."
if [ -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
    echo "📋 기존 SSL 인증서가 발견되었습니다. 갱신을 시도합니다..."
    
    # Docker로 certbot 실행하여 인증서 갱신
    docker run --rm \
        -v /etc/letsencrypt:/etc/letsencrypt \
        -v /var/lib/letsencrypt:/var/lib/letsencrypt \
        -p 80:80 \
        certbot/certbot renew \
        --standalone \
        --non-interactive
    
    if [ $? -eq 0 ]; then
        echo "✅ SSL 인증서 갱신 완료!"
    else
        echo "⚠️ 인증서 갱신 실패. 새로 발급을 시도합니다..."
        # 갱신 실패 시 새로 발급
        docker run --rm \
            -v /etc/letsencrypt:/etc/letsencrypt \
            -v /var/lib/letsencrypt:/var/lib/letsencrypt \
            -p 80:80 \
            certbot/certbot certonly \
            --standalone \
            --email "$EMAIL" \
            --agree-tos \
            --no-eff-email \
            --domains "$DOMAIN" \
            --non-interactive
        
        if [ $? -eq 0 ]; then
            echo "✅ SSL 인증서 새로 발급 완료!"
        else
            echo "❌ SSL 인증서 발급 실패!"
            exit 1
        fi
    fi
else
    echo "📦 SSL 인증서가 없습니다. 새로 발급을 시작합니다..."
    
    # Docker로 certbot 실행하여 새 인증서 발급
    docker run --rm \
        -v /etc/letsencrypt:/etc/letsencrypt \
        -v /var/lib/letsencrypt:/var/lib/letsencrypt \
        -p 80:80 \
        certbot/certbot certonly \
        --standalone \
        --email "$EMAIL" \
        --agree-tos \
        --no-eff-email \
        --domains "$DOMAIN" \
        --non-interactive
    
    if [ $? -eq 0 ]; then
        echo "✅ SSL 인증서 발급 성공!"
    else
        echo "❌ SSL 인증서 발급 실패!"
        exit 1
    fi
fi

# 권한 설정 (권한 문제로 인해 제거)
echo "🔐 인증서 발급 완료 (권한 설정 생략)"

# 4) 메인 애플리케이션 실행
echo "🔨 새 이미지 빌드 및 실행..."
$COMPOSE -f docker-compose.prod.yml --env-file .env up -d --build app

# 5) 애플리케이션 상태 확인
echo "🔍 애플리케이션 상태 확인 중..."

echo "⏳ 기동 대기..."
sleep 15

echo "📊 컨테이너 상태 확인..."
$COMPOSE -f docker-compose.prod.yml ps

# 컨테이너 내부에서 SSL 인증서 확인
echo "🧪 컨테이너 내부 SSL 인증서 확인 중..."
$COMPOSE -f docker-compose.prod.yml exec app sh -lc "
  echo '== Inside app container: check cert files =='
  ls -la /etc/letsencrypt/live/$DOMAIN || exit 1
  ls -la /etc/letsencrypt/archive/$DOMAIN || true
  # 내용 확인
  head -n 1 /etc/letsencrypt/live/$DOMAIN/fullchain.pem
  head -n 1 /etc/letsencrypt/live/$DOMAIN/privkey.pem
" || {
    echo "❌ 컨테이너에서 SSL 인증서가 보이지 않습니다 (마운트/경로 문제)";
    echo "   docker-compose.prod.yml의 볼륨 마운트를 확인하세요";
    exit 1;
}
echo "✅ 컨테이너 내부 SSL 인증서 확인 완료"

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
