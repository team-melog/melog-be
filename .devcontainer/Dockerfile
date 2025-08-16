FROM mcr.microsoft.com/devcontainers/java:17

# psql 클라이언트 설치
RUN apt-get update && apt-get install -y --no-install-recommends postgresql-client && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace