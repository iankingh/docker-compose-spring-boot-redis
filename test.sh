#!/usr/bin/env bash
# 一鍵跑 Spring Boot + Redis 專案的測試(含覆蓋率報告)。
# 用法: ./test.sh [傳給 mvn 的參數,預設 verify]
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$ROOT_DIR/spring-boot-redis"

# 優先使用 Java 11(pom 目標版本);找不到就退回 JAVA_HOME / 系統 java。
if [ -z "${JAVA_HOME:-}" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q '"11'; then
  DETECTED="$(/usr/libexec/java_home -v 11 2>/dev/null || true)"
  if [ -n "$DETECTED" ]; then
    export JAVA_HOME="$DETECTED"
  fi
fi

cd "$APP_DIR"
echo "JAVA_HOME=${JAVA_HOME:-<unset>}"
echo "java: $("${JAVA_HOME:-}"/bin/java -version 2>&1 | head -1)"

GOAL="${*:-verify}"
echo ">> ./mvnw $GOAL"
./mvnw -B --no-transfer-progress "$GOAL"

if [ -f "$APP_DIR/target/site/jacoco/index.html" ]; then
  echo ""
  echo "覆蓋率報告: $APP_DIR/target/site/jacoco/index.html"
fi