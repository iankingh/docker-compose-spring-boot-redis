# docker-compose-spring-boot-redis

以 Docker Compose 啟動外部 Tomcat、Spring Boot WAR 與 Redis 的示範專案。
應用提供 Redis 計數器及簡單的字串讀寫 API。

## 技術版本

- Java 17（Spring Boot 4.1.1 的最低需求；`pom.xml` 與 CI 一致）
- Spring Boot `4.1.1`（目前穩定版；版本及 Java 基線依 [官方系統需求](https://docs.spring.io/spring-boot/system-requirements.html) 與 [Maven Central 發行中繼資料](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml)）
- Maven Wrapper `3.9.16`
- Tomcat `11.0`（與 Spring Boot 4 所需 Servlet 6.1 相容；Tomcat 官方支援版本表：[Which Version?](https://tomcat.apache.org/whichversion.html)）
- Redis `7-alpine`

## 專案結構

```text
.
├── docker-compose.yml        # web、redis、network 與 volume
├── Dockerfile                # 外部 Tomcat 11 容器
├── build.sh                  # 測試/打包 WAR 並複製到 webapps
├── test.sh                   # Maven verify 與 JaCoCo 報告捷徑
├── tomcat/webapps/           # Compose 掛載的部署目錄
└── spring-boot-redis/        # Spring Boot WAR 專案
```

Compose 服務：

- `web`：將主機的 `tomcat/webapps/` 掛載到 Tomcat，對外提供 `8080`
- `redis`：Redis 資料存放於具名 volume `redis-data`，並只在主機 loopback
  `127.0.0.1:6379` 提供連線

WAR 會以 `tomcat/webapps/web.war` 部署，因此 HTTP context path 是 `/web`。

## 前置需求

- JDK 17 或更新版本（本機建置與測試）
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
| GET | `/web/hello` | 以 Redis 原子遞增將 key `hello` 的整數計數加一 |
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

# 多個 Maven goal／option 會逐一轉送
./test.sh clean verify
```

測試使用 MockMvc 與 mock `StringRedisTemplate` 驗證 API，並包含 Spring context
載入測試；執行測試不需要啟動 Redis。`verify` 後的 JaCoCo 報告位於：

```text
spring-boot-redis/target/site/jacoco/index.html
```

`test.sh` 尊重明確指定的 `JAVA_HOME`；只有未指定時才在 macOS 嘗試尋找 JDK 17，
其他系統使用系統 Java。可用 `JAVA_HOME=/path/to/jdk25 ./test.sh ...` 驗證 Java 25，
不會被腳本悄悄換回 JDK 17。

### 真實 Redis 與 Compose gate

```bash
# 驗證實際 Compose schema；沒有 Docker CLI 時不能視為通過
docker compose config --quiet

# 使用既有、隔離且無密碼的本機測試 Redis database（不得指向 production）
./test.sh -Predis-integration verify \
  -Dintegration.redis.host=127.0.0.1 \
  -Dintegration.redis.port=6379 -Dintegration.redis.database=15
```

`RedisEndpointIT` 啟動 random-port HTTP server，透過真實 Redis 驗證 64 個並行
`/hello` 請求的結果完整且不重複，並驗證 fixed/named key 讀寫及 missing-key 404。
測試要求 `hello` 和 `data` 起初不存在，只刪除自己使用的 keys，不會 `FLUSHDB`。
所有三個 Redis 參數必須明確提供；服務不存在或已有 fixed keys 時會失敗，
不會 silently skip。Failsafe 結果位於 `target/failsafe-reports/`。

上述 gate 不等於容器部署驗證。具備 Docker 的環境仍須執行 `./build.sh`、
`docker compose up --build -d`，再依 API 範例驗證外部 Tomcat 的 `/web` 路徑；
本機 random-port 整合測試不會驗證 WAR 掛載、容器網路或 Tomcat 部署。

2026-10-05 本機使用隔離的 Redis 7.4.2 實跑 9 個 unit tests、2 個 endpoint IT
（含 64 個並行請求）及 WAR 打包成功；standalone Compose 2.39.4 的 config
檢查通過。沒有 Docker Engine，未執行容器部署。CI 的 `compose-integration`
job 已接上 schema、WAR／Tomcat `/web`、missing-key 404 與 real Redis gate，
但遠端 job 尚未執行，不把設定存在視為容器驗收通過。
使用者更新 JDK 後，另以 Java `25.0.4.1` 完成同一個 unit／real Redis／WAR gate，
並從測試 XML 確認實際 test JVM 為 Java 25；source／target 與 CI 最低基線仍為 17。
2026-10-07 另以 Podman rootful VM 建置 Tomcat 11 映像，將 WAR 部署至 `/web`，
並以獨立 Redis 容器驗證 `/web/hello`、set/get 與 missing-key 404。此直接容器驗證
不代表 Compose schema 已驗證；當時環境沒有 Docker Compose 或 Podman Compose。

## 不使用 Docker 的本機執行

`application.yml` 預設 Redis host 為 Compose 服務名稱 `redis`。直接在主機執行時，
需先啟動本機 Redis，並覆寫為 `localhost`：

```bash
cd spring-boot-redis
SPRING_DATA_REDIS_HOST=localhost ./mvnw spring-boot:run
```

此方式使用根 context path（例如 `http://localhost:8080/hello`），與部署成
`web.war` 後的 `/web` 不同。

## 停止與清理

```bash
docker compose down

# 同時刪除 Redis 持久化資料
docker compose down -v
```

Compose 預設未設定 Redis 密碼，因此 host port 僅綁定 loopback。若要放到非本機
環境，請加入適當的 Redis 認證與網路保護，不要直接把 `6379` 發布到所有介面。
