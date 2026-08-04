# docker-compose-spring-boot-redis

以 Docker Compose 啟動外部 Tomcat、Spring Boot WAR 與 Redis 的示範專案。
應用提供 Redis 計數器及簡單的字串讀寫 API。

## 技術版本

- Java 11（`pom.xml` 編譯目標及 CI 使用版本）
- Spring Boot `2.7.0`
- Maven Wrapper `3.8.4`
- Tomcat `9.0` 映像
- Redis `7-alpine`

## 專案結構

```text
.
├── docker-compose.yml        # web、redis、network 與 volume
├── Dockerfile                # 外部 Tomcat 9 容器
├── build.sh                  # 測試/打包 WAR 並複製到 webapps
├── test.sh                   # Maven verify 與 JaCoCo 報告捷徑
├── tomcat/webapps/           # Compose 掛載的部署目錄
└── spring-boot-redis/        # Spring Boot WAR 專案
```

Compose 服務：

- `web`：將主機的 `tomcat/webapps/` 掛載到 Tomcat，對外提供 `8080`
- `redis`：Redis 資料存放於具名 volume `redis-data`，對外提供 `6379`

WAR 會以 `tomcat/webapps/web.war` 部署，因此 HTTP context path 是 `/web`。

## 前置需求

- JDK 11（本機建置與測試）
- Docker Engine 與 Docker Compose plugin
- 可存取 Maven Central，以便 Maven Wrapper 下載 Maven 與相依套件

不需要在主機安裝 Maven 或 Redis。

## 建置與啟動

```bash
# 在專案根目錄執行：測試、打包並複製 WAR
./build.sh

# 啟動 Tomcat 與 Redis
docker compose up --build
```

若使用只提供舊版獨立指令的環境，可將 `docker compose` 換成
`docker-compose`。

`build.sh` 必須先執行；Dockerfile 本身不會編譯或複製 WAR，而 Compose 的 bind
mount 會直接使用本機 `tomcat/webapps/` 內容。

背景啟動及查看日誌：

```bash
docker compose up --build -d
docker compose logs -f web redis
```

## API

| Method | Path | 說明 |
| --- | --- | --- |
| GET | `/web/hello` | 將 Redis key `hello` 的整數計數加一 |
| GET | `/web/redis/set/{value}` | 將 `value` 寫入固定 key `data` |
| GET | `/web/redis/get` | 讀取固定 key `data`；不存在時回傳 404 |
| GET | `/web/redis/set/{key}/{value}` | 寫入指定 key/value |
| GET | `/web/redis/get/{key}` | 讀取指定 key；不存在時回傳 404 |

```bash
curl http://localhost:8080/web/hello
curl http://localhost:8080/web/redis/set/world
curl http://localhost:8080/web/redis/get
curl http://localhost:8080/web/redis/set/mykey/myvalue
curl http://localhost:8080/web/redis/get/mykey
```

這些 GET 路由會修改資料，只是 Demo 設計，不建議直接套用為正式 API。

## 測試

```bash
# 跨平台的主要測試命令
cd spring-boot-redis
./mvnw -B --no-transfer-progress verify

# 回到根目錄後，也可使用便利腳本
./test.sh
```

測試使用 MockMvc 與 mock `StringRedisTemplate` 驗證 API，並包含 Spring context
載入測試；執行測試不需要啟動 Redis。`verify` 後的 JaCoCo 報告位於：

```text
spring-boot-redis/target/site/jacoco/index.html
```

`test.sh` 會在 macOS 嘗試尋找 JDK 11；其他系統請先正確設定 `JAVA_HOME`。

## 不使用 Docker 的本機執行

`application.yml` 預設 Redis host 為 Compose 服務名稱 `redis`。直接在主機執行時，
需先啟動本機 Redis，並覆寫為 `localhost`：

```bash
cd spring-boot-redis
SPRING_REDIS_HOST=localhost ./mvnw spring-boot:run
```

此方式使用根 context path（例如 `http://localhost:8080/hello`），與部署成
`web.war` 後的 `/web` 不同。

## 停止與清理

```bash
docker compose down

# 同時刪除 Redis 持久化資料
docker compose down -v
```

Compose 預設未設定 Redis 密碼。若要放到非本機環境，請先限制 `6379` 暴露範圍並
加入適當的 Redis 認證與網路保護。
