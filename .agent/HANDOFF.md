# VenueFlow Handoff

- 更新时间：2026-07-20 22:53:40 +08:00
- 分支：`ops/add-base-infrastructure`
- 当前 Change：无活动 Change
- 当前目标：v0.1.0 C02 基础设施已完成、同步并归档。

## 已完成

- 仓库文本、忽略规则、环境变量示例与版本清单。
- Maven Wrapper 3.9.16，含发行包 SHA-256 校验。
- JDK 21 多模块 Maven reactor、内部 BOM 与最小 common-core 模块。
- Enforcer、Surefire、Failsafe、JaCoCo、Spotless、SpotBugs 和 CycloneDX 门禁。
- 3 个真实测试，包括错误码契约和公共模块架构边界。
- README、ADR 和 GitHub Actions CI 基线。
- C02 proposal、design、`base-infrastructure` delta spec 和 13 项任务全部完成。
- `deploy/versions.env` 固定 MySQL 8.4.10、Redis 7.4.9-bookworm、RabbitMQ 4.1.8-management、Nacos 3.1.1；官方 registry manifests 同时提供 Linux amd64/arm64。
- `deploy/compose/compose.yml` 提供显式 `base` profile、固定 project、私有 bridge、参数化地址/端口、认证、四个命名卷、约 3.63GiB 配置上限和有界 healthcheck。
- PowerShell/POSIX 静态校验与 smoke 均已实现；smoke 执行 MySQL `SELECT 1`、Redis `PING`、RabbitMQ diagnostics 和 Nacos v3 console liveness。
- README、基础设施 Runbook 和独立 GitHub Actions infrastructure job 已完成；默认 Maven verify 仍不依赖 Docker。
- 未创建业务服务、Migration、基础设施客户端、业务数据库/用户、业务消息拓扑、Elasticsearch 或观测栈。
- `base-infrastructure` delta spec 已同步到主规格，Change 已归档至 `openspec/changes/archive/2026-07-20-add-base-infrastructure/`。

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

## 待完成

- 等待在远端 CI 实际触发后确认 infrastructure job 结果。
- 本机 Docker Desktop 配置了失效/不稳定 registry mirror；显式官方 `registry-1.docker.io` 拉取成功，未修改用户 daemon 配置。
- 本机已有 MySQL/Redis/Nacos 相关端口占用，因此未提交的 `.env` 使用 13306、16379、18848、19848、19849；提交的 `.env.example` 仍使用标准端口。
- Nacos 3.1.1 console 健康端点为容器内部 `8080/v3/console/health/liveness`；旧 v1 地址返回 410。console 8080 未发布到宿主机。
- `deploy/image-digests.lock` 未创建；C02 只固定并验证精确标签，避免伪造尚未进入发布流程的 digest lock。

## 下一步

触发并检查远端 CI；之后创建独立 C03 `add-resource-service-skeleton`。不要继续修改已归档 C02，任何修订都应创建新 Change。

## 禁止操作

- 不在本 Change 中创建业务服务、Entity、Mapper、数据库 Migration 或基础设施客户端。
- 不提交 `.env`、私钥、Token、构建产物或本机绝对路径。
- 不绕过 `clean verify` 或 OpenSpec 严格校验。
- 不使用 `latest`、不伪造镜像 digest、不自动执行 `down --volumes` 或删除命名卷。
