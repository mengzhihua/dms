# DMS 生产部署（MySQL 8 + Docker Compose）

一键拉起「MySQL 8 + 后端 + 前端(nginx 反代)」三容器，前端 SPA 由 nginx 提供静态文件并把 `/api/` 反代到后端。

## 前置要求

- Docker 20.10+ 与 Docker Compose v2（`docker compose version`）
- 仅需一个对外端口（默认 80，可用 `DMS_HTTP_PORT` 调整）

## 首次部署

```bash
cd deploy
cp .env.example .env      # 至少填写 DMS_JWT_SECRET；建议同时设置 DMS_ADMIN_PASSWORD 与 DMS_DB_PASSWORD
./deploy.sh up            # 等价于 docker compose up -d --build
```

`.env` 变量说明：

| 变量 | 说明 | 默认 |
| --- | --- | --- |
| `DMS_HTTP_PORT` | 前端对外端口 | `80` |
| `DMS_DB_PASSWORD` | MySQL root 密码（首次初始化生效） | `root` |
| `DMS_JWT_SECRET` | JWT 签名密钥，**必填**，随机长字符串 | 无 |
| `DMS_ADMIN_PASSWORD` | 首次启动创建 admin 账号的密码；留空则随机生成并打印到 backend 日志 | 空 |

等待健康检查通过后访问 `http://localhost:<DMS_HTTP_PORT>`，用 `admin` + `DMS_ADMIN_PASSWORD`（或日志中的随机密码）登录。

> 生产 profile 不加载演示数据（`data-prod.sql` 只含字典/模板类基础数据）。如需演示数据可用非 prod 的 `mysql` profile。

## 备份与恢复

```bash
./deploy.sh backup                          # 输出 deploy/backup/dms-<时间戳>.sql.gz
zcat deploy/backup/dms-xxx.sql.gz | docker compose -f deploy/docker-compose.yml exec -T mysql mysql -proot dms
```

## 升级与 schema 变更约定

- **全新库**：由 `schema-mysql.sql` 初始化（与 H2 `schema.sql` 列集合由 `SchemaConsistencyTest` 保证一致）。
- **已有库升级**：在 `deploy/mysql/upgrade/` 放置 `V<yyyymmdd>__<desc>.sql`，利用 docker-entrypoint-initdb.d 仅对新卷执行的机制；对存量库请手工执行对应增量脚本（MySQL 8 不支持 `ADD COLUMN IF NOT EXISTS`，需自行判断列是否存在）。
- 升级镜像：`git pull && ./deploy.sh up`（compose 会重建镜像并重启容器，数据卷保留）。

## 常用命令

```bash
./deploy.sh logs backend    # 跟踪后端日志
./deploy.sh down            # 停止并移除容器（保留数据卷）
docker compose -f deploy/docker-compose.yml down -v   # 连数据卷一起删（危险）
```

## 本地直连 MySQL 验证（不用 docker 构建）

```bash
docker run -d --name dms-mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=dms -p 3307:3306 \
  mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
cd backend && DMS_DB_URL='jdbc:mysql://localhost:3307/dms?useSSL=false&allowPublicKeyRetrieval=true' \
  mvn spring-boot:run -Dspring-boot.run.arguments='--spring.profiles.active=mysql --server.port=8090'
../scripts/smoke.sh http://localhost:8090
```
