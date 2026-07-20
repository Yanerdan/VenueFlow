# VenueFlow 园区共享资源预约平台

VenueFlow 当前处于 v0.1.0 基础设施阶段。本仓库已经具备可复现的 Maven 多模块构建、真实单元测试、基础质量门禁，以及按需启动的 MySQL、Redis、RabbitMQ 和 Nacos `base` profile；尚未实现业务服务。

## 环境要求

- JDK 21（本机已安装 Temurin 21.0.11）
- Git
- 首次构建时可访问 Maven Central
- 不需要全局安装 Maven，仓库内的 Maven Wrapper 固定使用 Maven 3.9.16
- 仅运行基础设施时需要 Docker Engine/Desktop 与 Docker Compose v2

新开终端后先确认 Wrapper 使用 JDK 21：

```powershell
.\mvnw.cmd -version
```

输出中的 Maven 应为 `3.9.16`，Java 应为 `21.x`。若仍显示旧 Java，请确认 `JAVA_HOME` 指向 JDK 21，然后重新打开终端。

## 构建与验证

Windows PowerShell：

```powershell
.\mvnw.cmd clean verify
```

Linux、macOS 或 Git Bash：

```bash
./mvnw clean verify
```

该命令会执行编译、JUnit、Failsafe 测试发现、JaCoCo 覆盖率报告、Enforcer、Spotless、SpotBugs 和 CycloneDX SBOM 生成。默认构建不连接 VMware、Docker、数据库、Redis、RabbitMQ、Nacos 或 Elasticsearch。

## 基础设施

先复制环境示例并把所有 `replace-with-*` 值替换为仅本机使用的值：

```powershell
Copy-Item .env.example .env
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/bootstrap/validate-base-infrastructure.ps1 -EnvFile .env
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-test/base-infrastructure-smoke.ps1 -EnvFile .env
```

Linux、macOS 或 Git Bash：

```bash
cp .env.example .env
sh scripts/bootstrap/validate-base-infrastructure.sh .env deploy/versions.env
sh scripts/smoke-test/base-infrastructure-smoke.sh .env
```

smoke 会显式选择 `base` profile，等待四个容器达到 healthy，并执行只读协议检查。若标准宿主端口已被占用，只修改未提交 `.env` 中对应的 `*_PORT`；容器内部端口不变。

正常停止且保留数据卷：

```powershell
docker compose --env-file deploy/versions.env --env-file .env -f deploy/compose/compose.yml --profile base down --timeout 30
```

**禁止在普通停止、smoke 或 CI 清理中添加 `--volumes`。** 删除卷会清除基础组件数据，只能在明确确认 project 和卷名后人工执行。完整操作与故障排查见 [基础设施 Runbook](docs/runbook/base-infrastructure.md)。

## 当前模块

- `venueflow-dependencies`：内部依赖 BOM，集中管理框架版本。
- `venueflow-common`：公共模块聚合器。
- `venueflow-common/venueflow-common-core`：最小、无业务含义的公共 Java 模块和基线测试。

根 `pom.xml` 只承担模块聚合、版本管理和质量门禁，不包含业务依赖。

## 仓库约定

- `.version/` 是 Java、Maven、框架和构建插件版本清单。
- `deploy/versions.env` 保存经过 manifest 与实际启动验证的精确基础镜像标签。
- `deploy/compose/compose.yml` 定义显式 `base` profile、健康检查、资源边界和命名卷。
- `.env.example` 只提供本地配置占位符；复制为 `.env` 后填入本机值，禁止提交真实密钥。
- `docs/adr/` 记录重要架构决策。
- `.agent/HANDOFF.md` 记录当前实现状态与下一步。
- `openspec/` 保存主规格、活动 Change 和已归档 Change。

## 当前非目标

本阶段不包含 Gateway、Resource、Booking、User 等业务服务，也不包含数据库 Migration、基础设施客户端、Elasticsearch、观测栈或虚假的业务测试覆盖率。

当前下一项工作是创建 C03 `add-resource-service-skeleton`。
