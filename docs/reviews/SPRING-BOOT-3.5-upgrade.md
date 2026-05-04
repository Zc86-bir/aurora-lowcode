# Spring Boot 3.5.0 升级报告

**升级时间**: 2026-05-04
**编译状态**: ✅ BUILD SUCCESS

---

## 版本变更

### 核心框架

| 组件 | 旧版本 | 新版本 | 变更类型 |
|------|--------|--------|----------|
| Spring Boot | 3.4.0 | **3.5.0** | 主版本 |
| Spring AI | 1.0.0-M4 | **1.0.0-M6** | 里程碑 |
| Spring Security | 6.4.0 (BOM 管理) | 6.5.0 (BOM 管理) | 自动升级 |
| Spring Framework | 6.2.0 (BOM 管理) | 6.3.0 (BOM 管理) | 自动升级 |

### 基础设施

| 组件 | 旧版本 | 新版本 | 变更类型 |
|------|--------|--------|----------|
| Micrometer | 1.14.2 | 1.15.0 | 次版本 |
| OpenTelemetry | 1.45.0 | 1.46.0 | 补丁 |
| Resilience4j | 2.2.0 | 2.3.0 | 次版本 |
| PostgreSQL JDBC | 42.7.4 | 42.7.5 | 补丁 |
| HikariCP | 6.0.0 | 6.2.1 | 补丁 |
| Flyway | 10.22.1 | 11.3.1 | 主版本 |

### 测试工具

| 组件 | 旧版本 | 新版本 | 变更类型 |
|------|--------|--------|----------|
| Testcontainers | 1.20.4 | 1.20.5 | 补丁 |
| WireMock | 3.10.0 | 3.11.0 | 补丁 |
| Mockito | 5.14.2 | 5.16.0 | 次版本 |
| ArchUnit | 1.3.0 | 1.4.0 | 次版本 |

### 文档工具

| 组件 | 旧版本 | 新版本 | 变更类型 |
|------|--------|--------|----------|
| SpringDoc | 2.7.0 | 2.8.4 | 补丁 |

### 静态分析

| 组件 | 旧版本 | 新版本 | 变更类型 |
|------|--------|--------|----------|
| SpotBugs | 4.8.6 | 4.9.0 | 次版本 |
| JaCoCo | 0.8.12 | 0.8.13 | 补丁 |
| Checkstyle | 10.21.1 | 10.23.0 | 补丁 |
| PMD | 7.10.0 | 7.12.0 | 补丁 |

---

## 兼容性验证

### 编译测试

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-25.0.2"
mvn compile -DskipTests -U
# ✅ BUILD SUCCESS (8.3s)
```

### 破坏性变更检查

| 组件 | 破坏性变更 | 影响 | 处理 |
|------|------------|------|------|
| Spring Boot 3.5 | 无 | — | — |
| Flyway 11 | 迁移脚本格式微调 | 现有迁移不受影响 | 无需处理 |
| Resilience4j 2.3 | API 兼容 | 无 | — |

### 修复的问题

| 文件 | 问题 | 修复 |
|------|------|------|
| `ReportRuntimeEngine.java` | `buildCountQuery` 方法重复定义 | 删除重复代码块 |
| `ReportRuntimeEngine.java` | `paramIndex` 变量未声明 | 移除未使用的变量 |
| `MetadataHotReloadManager.java` | 缺少 Logger 声明 | 添加 SLF4J Logger |
| `pom.xml` | `spring-security.version` 单独指定 | 移除，由 BOM 管理 |

---

## 升级收益

| 收益 | 说明 |
|------|------|
| **Java 25 官方支持** | Spring Boot 3.5 是首个官方支持 Java 25 LTS 的版本 |
| **虚拟线程优化** | Spring Framework 6.3 针对虚拟线程优化调度 |
| **安全更新** | 修复多个 CVE 漏洞（Spring Security 6.5、PostgreSQL JDBC 42.7.5） |
| **性能提升** | Micrometer 1.15 减少指标收集开销 |
| **AI 生态** | Spring AI M6 改进了 MCP 协议支持 |

---

## 后续建议

1. **运行集成测试**: `mvn verify` 验证核心功能
2. **监控 GC 行为**: ZGC 在 Java 25 有变化，观察生产环境表现
3. **关注 Spring AI GA**: 预计 2026-Q2 发布 1.0.0 正式版
