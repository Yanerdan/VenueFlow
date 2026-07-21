# VenueFlow Handoff

- 更新时间：2026-07-21 11:09:20 +08:00
- 分支：`ops/add-resource-service-skeleton`
- 当前 Change：无活动 Change
- 当前目标：v0.1.0 C03 最小 Resource Service 与 Actuator 已完成、同步并归档。

## 已完成

- 仓库文本、忽略规则、环境变量示例与版本清单。
- Maven Wrapper 3.9.16，含发行包 SHA-256 校验。
- JDK 21 多模块 Maven reactor、内部 BOM 与最小 common-core 模块。
- Enforcer、Surefire、Failsafe、JaCoCo、Spotless、SpotBugs 和 CycloneDX 门禁。
- common-core 有 3 个真实测试，包括错误码契约和公共模块架构边界。
- README、ADR 和 GitHub Actions CI 基线。
- C02 proposal、design、`base-infrastructure` delta spec 和 13 项任务全部完成。
- `deploy/versions.env` 固定 MySQL 8.4.10、Redis 7.4.9-bookworm、RabbitMQ 4.1.8-management、Nacos 3.1.1；官方 registry manifests 同时提供 Linux amd64/arm64。
- `deploy/compose/compose.yml` 提供显式 `base` profile、固定 project、私有 bridge、参数化地址/端口、认证、四个命名卷、约 3.63GiB 配置上限和有界 healthcheck。
- PowerShell/POSIX 静态校验与 smoke 均已实现；smoke 执行 MySQL `SELECT 1`、Redis `PING`、RabbitMQ diagnostics 和 Nacos v3 console liveness。
- README、基础设施 Runbook 和独立 GitHub Actions infrastructure job 已完成；默认 Maven verify 仍不依赖 Docker。
- C02 未创建业务服务、Migration、基础设施客户端、业务数据库/用户、业务消息拓扑、Elasticsearch 或观测栈。
- `base-infrastructure` delta spec 已同步到主规格，Change 已归档至 `openspec/changes/archive/2026-07-20-add-base-infrastructure/`。
- C02 提交 `39269de` 已推送至 `origin/ops/add-base-infrastructure` 并建立 upstream。
- C03 proposal、design、`resource-service-skeleton` delta spec 与 13 项 tasks 已完成。
- `venueflow-resource-service` 已加入根 reactor；生产内容仅为 Spring Boot 应用入口和 secret-free `application.yml`，默认端口 8083，支持 `SERVER_PORT` 覆盖。
- Resource Service 运行依赖限定为 Spring MVC 与 Actuator；模块 Enforcer 会拒绝数据库、Nacos、Redis、AMQP、Feign、Sentinel、安全、Tracing 与 Prometheus 客户端。
- Actuator Web 只暴露不含详情的 health；liveness/readiness 可用，`env`、`configprops`、`loggers`、`mappings`、`metrics` 均不可访问。
- 新增上下文、随机端口 HTTP 和实际可执行 jar 三层验收；jar 测试有界启动并只终止自身创建的进程。
- `resource-service-skeleton` delta spec 已同步到主规格，Change 已归档至 `openspec/changes/archive/2026-07-21-add-resource-service-skeleton/`。

## 验证记录

- `.\mvnw.cmd -version`：Maven 3.9.16，Temurin Java 21.0.11。
- `./mvnw -version`（Git Bash）：Maven 3.9.16，Temurin Java 21.0.11。
- `.\mvnw.cmd clean verify`：通过；4 个 reactor 项目成功，3 个测试通过。
- 负向单测校验：临时错误断言使 Surefire 返回退出码 1，随后已恢复源码。
- `help:effective-pom`：已解析 Java 21 和冻结的框架/插件版本。
- JDK 17 负向校验：Enforcer 在编译前失败并明确提示需要 JDK 21。
- `openspec validate --all --strict`：2 项通过，0 项失败。
- 仓库卫生检查：LF 属性生效，无未忽略的构建产物、密钥文件或本机绝对路径。
- `openspec status --change add-base-infrastructure`：4/4 规划工件完成。
- `openspec validate add-base-infrastructure --strict`：通过。
- `openspec validate --all --strict`：3 项通过，0 项失败。
- `git diff --check`：通过。
- Docker Hub registry API/manifest：4 个精确标签存在并包含 Linux amd64/arm64；本机实际镜像为 Linux/amd64。
- PowerShell 与 Git Bash 静态检查：通过；`latest`、`0.0.0.0` 和缺失 secret 负向样例均被拒绝。
- PowerShell smoke：4/4 healthy 且四项只读协议检查通过；最终热运行 6.2 秒。
- Git Bash smoke：4/4 healthy 且四项只读协议检查通过；最终冷重启 165 秒。
- `down`/重启生命周期：四卷创建时间均保持 `2026-07-20T14:01:16Z`；最终容器和网络已停止，卷仍保留。
- 空闲资源一次采样：MySQL 458MiB、Redis 6MiB、RabbitMQ 99MiB、Nacos 819MiB，合计约 1.35GiB；这不是性能承诺。
- `mvnw.cmd --batch-mode --no-transfer-progress clean verify`：通过；4 个 reactor 项目成功，3 个测试通过。
- CI YAML 只读解析：`verify` 与 `infrastructure` 两个 job 存在；CI 等价命令已在本机执行，远端 GitHub Actions 尚未触发。
- secret/私钥扫描、忽略规则和 `git diff --check`：通过；本地 `.env` 与 Maven `target/` 均被忽略。
- 归档前主规格同步核对：8/8 requirements 一致；同步后 OpenSpec 4 项严格校验通过。
- 归档结构检查：无活动 Change，归档目录保留 `.openspec.yaml`。
- `openspec status --change add-resource-service-skeleton`：4/4 规划工件完成，apply-ready。
- `openspec validate add-resource-service-skeleton --strict`：通过；`openspec validate --all --strict`：4 项通过，0 项失败。
- 模块级 `-pl venueflow-resource-service -am clean verify`：通过；1 个 Surefire 测试和 3 个 Failsafe 测试成功。
- 全仓 `clean verify`：5 个 reactor 项目成功，合计 7 个测试通过，0 failure/error/skipped；执行时 C02 容器数量为 0。
- 实际 jar 验收：Manifest 使用 Spring Boot `JarLauncher` 和正确 `Start-Class`，包含 43 个 `BOOT-INF/lib` 依赖；`SERVER_PORT` 覆盖下两项探针均返回 `UP`。
- 运行依赖树检查：顶层只有 `spring-boot-starter-web` 与 `spring-boot-starter-actuator`，禁止依赖扫描通过。
- C03 范围检查：生产文件仅应用入口与 `application.yml`；无业务 API、Entity、Mapper、Migration 或基础设施配置。
- `git diff --check`、构建产物忽略、已知本地凭据指纹、私钥头和提交候选配置复核均通过。
- C03 主规格同步：6/6 requirements 一致；同步后 `openspec validate --all --strict`：5 项通过，0 项失败。
- C03 归档结构检查：无活动 Change，归档目录保留 `.openspec.yaml`。

## 待完成

- 等待在远端 CI 实际触发后确认 infrastructure job 结果。
- 本机 Docker Desktop 配置了失效/不稳定 registry mirror；显式官方 `registry-1.docker.io` 拉取成功，未修改用户 daemon 配置。
- 本机已有 MySQL/Redis/Nacos 相关端口占用，因此未提交的 `.env` 使用 13306、16379、18848、19848、19849；提交的 `.env.example` 仍使用标准端口。
- Nacos 3.1.1 console 健康端点为容器内部 `8080/v3/console/health/liveness`；旧 v1 地址返回 410。console 8080 未发布到宿主机。
- `deploy/image-digests.lock` 未创建；C02 只固定并验证精确标签，避免伪造尚未进入发布流程的 digest lock。
- C03 实现尚未提交或推送；需要在复核后单独提交并推送。

## 下一步

复核 C03 diff 后提交并推送；随后通过新的 OpenSpec Change 规划 v0.2.0 最小业务闭环。不要继续修改已归档 C02 或 C03，任何修订都应创建新 Change。

## 禁止操作

- 后续 Change 必须保持服务边界；不共享业务 Entity、不跨库访问、不提前引入无验收的基础设施客户端。
- 不提交 `.env`、私钥、Token、构建产物或本机绝对路径。
- 不绕过 `clean verify` 或 OpenSpec 严格校验。
- 不使用 `latest`、不伪造镜像 digest、不自动执行 `down --volumes` 或删除命名卷。
