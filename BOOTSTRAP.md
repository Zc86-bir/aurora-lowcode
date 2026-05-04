# Aurora Low-Code Platform — Bootstrap Guide

> 企业级 AI 驱动低代码平台。用自然语言构建应用，30 秒生成 CRUD 全栈代码。

---

## 一、环境要求

| 组件 | 最低版本 | 推荐版本 | 说明 |
|------|----------|----------|------|
| JDK | 25 LTS | 25.0.2 LTS | 必须为 JDK 25（虚拟线程 + StructuredTaskScope） |
| Maven | 3.9+ | 3.9.9 | 项目管理 |
| Docker | 24+ | 27+ | 容器运行时 |
| Node.js | 18+ | 20 LTS | 前端开发（可选） |
| PostgreSQL | 15+ | 17 | 生产数据库 |

### 一键检查环境

```bash
make help          # 查看所有可用命令
java -version      # 应显示 openjdk 25.x
mvn -version       # 应显示 Maven 3.9+
docker --version   # 应显示 Docker 24+
```

---

## 二、快速启动

### 方式一：开发模式（推荐）

```bash
# 1. 启动基础设施（PostgreSQL + Redis）
make dev

# 2. 启动后端（新终端）
export JAVA_HOME=/path/to/jdk-25
mvn spring-boot:run -Dspring.profiles.active=dev

# 3. 验证启动
curl http://localhost:8080/actuator/health
# 应返回: {"status":"UP"}
```

### 方式二：Docker Compose 生产模式

```bash
# 设置必要的环境变量
export DATABASE_PASSWORD=your_secure_db_password
export REDIS_PASSWORD=your_secure_redis_password
export JWT_SECRET=your_jwt_secret_min_32_chars
export MINIO_SECRET_KEY=your_minio_secret_key

# 一键启动
make docker-up

# 验证
curl http://localhost:8080/actuator/health
```

### 方式三：手动构建

```bash
# 1. 构建 JAR
make build

# 2. 运行
export JAVA_HOME=/path/to/jdk-25
java -jar target/aurora-lowcode-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=dev
```

---

## 三、验证步骤

启动后运行以下命令验证所有组件正常：

```bash
# 1. 健康检查
curl -s http://localhost:8080/actuator/health | jq .
# 应返回: {"status":"UP"}

# 2. API 文档
curl -s http://localhost:8080/v3/api-docs | jq .info

# 3. Swagger UI
open http://localhost:8080/swagger-ui.html

# 4. 运行完整验证脚本
./scripts/verify.sh
```

---

## 四、验证脚本

> 注意：`verify.sh` 需要 bash 环境（Linux/macOS/WSL/Git Bash），不支持原生 PowerShell。

`scripts/verify.sh` 自动化验证所有端点：

```bash
chmod +x scripts/verify.sh
./scripts/verify.sh
```

检查项：
- [x] 应用健康状态
- [x] 数据库连接
- [x] Redis 连接
- [x] Skill 加载
- [x] 审计链完整性
- [x] MCP 端点可达

---

## 五、故障排查

### JDK 版本不对

```
Error: 源发行版 25 与 --enable-preview 一起使用时无效
```

**修复**：确保 `JAVA_HOME` 指向 JDK 25+：
```bash
export JAVA_HOME="/path/to/jdk-25"
java -version  # 确认输出 openjdk 25.x
```

### 数据库连接失败

```
org.postgresql.util.PSQLException: Connection refused
```

**修复**：
```bash
# 检查 PostgreSQL 是否运行
docker ps | grep postgres

# 启动数据库
docker compose -f docker-compose.dev.yml up -d postgres

# 验证连接
psql -h localhost -U aurora -d aurora -c "SELECT 1"
```

### 端口被占用

```
Port 8080 was already in use
```

**修复**：
```bash
# 查找占用端口的进程
lsof -i :8080  # Linux/Mac
netstat -ano | findstr :8080  # Windows

# 或使用自定义端口
export SERVER_PORT=9090
mvn spring-boot:run
```

### Maven 依赖下载慢

```bash
# 使用阿里云镜像（在 pom.xml 中已配置）
# 或手动设置 ~/.m2/settings.xml
```

### Flyway 迁移失败

```
org.flywaydb.core.api.FlywayException: Validate failed
```

**修复**：
```bash
# 检查迁移脚本顺序
ls -la src/main/resources/db/migration/

# 空数据库重新运行
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up -d
```

### 内存不足

```
java.lang.OutOfMemoryError: Java heap space
```

**修复**：
```bash
export JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseZGC"
java $JAVA_OPTS -jar target/*.jar
```

---

## 五、生产环境数据备份与恢复

### 5.1 定时备份（K8s CronJob）

部署 Helm Chart 时自动启用数据库定时备份：

```bash
helm upgrade --install aurora ./deploy/helm/aurora \
  --set backup.enabled=true \
  --set backup.schedule="0 2 * * *" \
  --set minio.secretKey=your_minio_key \
  --namespace aurora --create-namespace
```

**备份流程**：
1. 每天凌晨 2:00 UTC，CronJob 启动
2. 使用 `pg_dump --format=custom --compress=9` 导出数据库
3. 通过 `mc` (MinIO Client) 上传到 MinIO `aurora-backups` 桶
4. 保留最近 30 天备份，自动清理旧文件

**查看备份状态**：
```bash
# 查看 CronJob 历史记录
kubectl get cronjob aurora-db-backup -n aurora
kubectl get jobs --selector=app.kubernetes.io/component=backup -n aurora

# 查看备份文件
mc ls minio/aurora-backups/
```

**手动触发备份**：
```bash
kubectl create job --from=cronjob/aurora-db-backup manual-backup-$(date +%Y%m%d) -n aurora
```

### 5.2 PITR 时间点恢复（推荐生产启用）

对于生产环境，**强烈建议**开启 PostgreSQL 17 的 WAL 归档 + pgBackRest，实现任意秒级回滚。

**启用方式**：
```bash
helm upgrade --install aurora ./deploy/helm/aurora \
  --set backup.pitr.enabled=true \
  --set backup.pitr.encryption.cipherPass=your_encryption_key \
  --namespace aurora --create-namespace
```

**恢复场景**：

| 场景 | 恢复方式 | 命令 |
|------|----------|------|
| 误删租户数据 | 恢复到删除前 1 秒 | `pgbackrest --target="2026-05-04 14:29:59"` |
| 错误迁移 | 恢复到命名恢复点 | `pgbackrest --target="before_migration_v4"` |
| 数据库损坏 | 恢复到最新完整备份 | `pgbackrest --type=full` |

**详细文档**：[deploy/backup/PITR-GUIDE.md](deploy/backup/PITR-GUIDE.md)

### 5.3 恢复时间目标

| 指标 | 目标 | 实现方式 |
|------|------|----------|
| RPO（数据丢失窗口） | < 1 分钟 | WAL 归档（archive_timeout=60s） |
| RTO（恢复时间） | < 30 分钟 | pgBackRest 并行恢复 |
| 保留期 | 30 天 | MinIO + pgBackRest 自动清理 |

---

## 六、配置文件说明

| 文件 | 用途 |
|------|------|
| `pom.xml` | Maven 依赖 + 插件配置 |
| `application.yml` | Spring Boot 配置（dev/test/prod） |
| `docker-compose.dev.yml` | 开发环境基础设施 |
| `docker-compose.prod.yml` | 生产环境完整栈 |
| `Dockerfile.prod` | 生产容器构建 |
| `.github/workflows/ci.yml` | CI 流水线 |
| `.github/workflows/cd.yml` | CD 流水线 |
| `Makefile` | 快捷命令 |
| `deploy/helm/aurora/` | Kubernetes Helm Chart |

---

## 七、环境变量参考

| 变量 | 默认值 | 必填(生产) | 说明 |
|------|--------|:----------:|------|
| `DATABASE_HOST` | localhost | 是 | PostgreSQL 地址 |
| `DATABASE_PASSWORD` | — | **是** | 数据库密码 |
| `REDIS_HOST` | localhost | 是 | Redis 地址 |
| `REDIS_PASSWORD` | — | **是** | Redis 密码 |
| `JWT_SECRET` | — | **是** | JWT 签名密钥（≥32 字符） |
| `SERVER_PORT` | 8080 | 否 | HTTP 端口 |
| `SPRING_PROFILES_ACTIVE` | dev | 否 | 运行环境 |
| `CORS_ORIGINS` | http://localhost:3000 | 否 | CORS 白名单 |

---

## 八、技能列表

平台内置 13 个 AI Skill：

| Skill ID | 名称 | 用途 |
|----------|------|------|
| `jeecg-codegen` | Jeecg 代码生成 | 实体+Controller+Service+Mapper+DDL |
| `jeecg-bpmn` | Jeecg 工作流 | BPMN 2.0 XML 生成 |
| `jeecg-onlform` | Jeecg Online 表单 | Online 表单配置 |
| `jeecg-onlreport` | Jeecg Online 报表 | Online 报表配置 |
| `jeecg-onlchart` | Jeecg Online 图表 | 在线图表 |
| `jeecg-desform` | Jeecg DesForm | 设计器表单 JSON |
| `jeecg-system` | Jeecg 系统管理 | 系统配置管理 |
| `jimubi-dashboard` | JimuBI 仪表板 | 数据仪表板 |
| `jimubi-bigscreen` | JimuBI 大屏 | 数据大屏 |
| `jimureport` | JimuReport | 积木报表 |
| `api_designer` | API 设计器 | REST API 合同 |
| `permission_designer` | 权限设计器 | RBAC/ABAC 策略 |
| `template_generator` | 模板生成器 | 文档模板 |

---

## 九、下一步

- [ ] 接入 LLM API（配置 Spring AI 提供商密钥）
- [ ] 创建 Vue 3 前端入口
- [ ] 配置 CI/CD 流水线（GitHub Actions）
- [ ] 部署到 Kubernetes 集群
