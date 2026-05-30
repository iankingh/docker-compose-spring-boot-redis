# docker-compose-spring-boot-redis

以 Docker Compose 同時啟動 Spring Boot + Redis + Tomcat 的 Demo 專案。

## 架構

```
┌──────────────────────────────────────┐
│  Docker Compose                      │
│                                      │
│  ┌──────────────┐  ┌──────────────┐  │
│  │  web (Tomcat)│  │    Redis     │  │
│  │  :8080       │──│  :6379       │  │
│  └──────────────┘  └──────────────┘  │
└──────────────────────────────────────┘
```

- **web**: Tomcat 9.0，掛載 `tomcat/webapps/`，部署 Spring Boot WAR
- **redis**: Redis 7 (alpine)，資料透過 `redis-data` volume 持久化

## 前置需求

- [Docker](https://docs.docker.com/get-docker/) & Docker Compose
- Java 11+（本地建置用）

## 建置與啟動

### 方法一：使用 build.sh（推薦）

```bash
# 建置 WAR 並複製到 tomcat/webapps/
./build.sh

# 啟動所有服務
docker-compose up --build
```

### 方法二：手動步驟

```bash
cd spring-boot-redis
./mvnw clean package
cp ./target/spring-boot-redis-0.0.1.war ../tomcat/webapps/web.war
cd ..
docker-compose up --build
```

## API

| Method | Path | 說明 |
|--------|------|------|
| GET | `/hello` | 每次呼叫計數 +1，存入 Redis |
| GET | `/redis/set/{value}` | 將 `value` 存入固定 key `data` |
| GET | `/redis/get` | 讀取固定 key `data` 的值 |
| GET | `/redis/set/{key}/{value}` | 將 `value` 存入指定 `key` |
| GET | `/redis/get/{key}` | 讀取指定 `key` 的值 |

### 範例

```bash
# 計數器
curl http://localhost:8080/web/hello

# 存入固定 key
curl http://localhost:8080/web/redis/set/world

# 讀取固定 key
curl http://localhost:8080/web/redis/get

# 存入自訂 key
curl http://localhost:8080/web/redis/set/mykey/myvalue

# 讀取自訂 key
curl http://localhost:8080/web/redis/get/mykey
```

## 停止服務

```bash
docker-compose down
```

> 加上 `-v` 旗標可同時清除 Redis volume：`docker-compose down -v`
