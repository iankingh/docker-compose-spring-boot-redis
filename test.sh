#!/usr/bin/env bash
# 一鍵跑 Spring Boot + Redis 專案的測試(含覆蓋率報告)。
# 用法: ./test.sh [傳給 mvn 的參數,預設 verify]
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$ROOT_DIR/spring-boot-redis"

# 尊重指定的 JAVA_HOME；未指定時在 macOS 優先尋找 Java 17。
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
  DETECTED="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
  if [ -n "$DETECTED" ]; then
    export JAVA_HOME="$DETECTED"
  fi
fi

cd "$APP_DIR"
echo "JAVA_HOME=${JAVA_HOME:-<unset>}"
if [ -n "${JAVA_HOME:-}" ]; then
  JAVA_COMMAND="$JAVA_HOME/bin/java"
else
  JAVA_COMMAND="$(command -v java)"
fi
echo "java: $("$JAVA_COMMAND" -version 2>&1 | head -1)"

if [ "$#" -eq 0 ]; then
  set -- verify
fi

echo ">> ./mvnw $*"
./mvnw -B --no-transfer-progress "$@"

if [ -f "$APP_DIR/target/site/jacoco/index.html" ]; then
  echo ""
  echo "覆蓋率報告: $APP_DIR/target/site/jacoco/index.html"
fi
