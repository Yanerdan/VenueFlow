# VenueFlow 园区共享资源预约平台

VenueFlow 当前处于 v0.1.0 工程基线阶段。本仓库已经具备可复现的 Maven 多模块构建、真实单元测试和基础质量门禁，但尚未实现业务服务或运行时基础设施。

## 环境要求

- JDK 21（本机已安装 Temurin 21.0.11）
- Git
- 首次构建时可访问 Maven Central
- 不需要全局安装 Maven，仓库内的 Maven Wrapper 固定使用 Maven 3.9.16

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

## 当前模块

- `venueflow-dependencies`：内部依赖 BOM，集中管理框架版本。
- `venueflow-common`：公共模块聚合器。
- `venueflow-common/venueflow-common-core`：最小、无业务含义的公共 Java 模块和基线测试。

根 `pom.xml` 只承担模块聚合、版本管理和质量门禁，不包含业务依赖。

## 仓库约定

- `.version/` 是 Java、Maven、框架和构建插件版本清单。
- `.env.example` 只提供本地配置占位符；复制为 `.env` 后填入本机值，禁止提交真实密钥。
- `docs/adr/` 记录重要架构决策。
- `.agent/HANDOFF.md` 记录当前实现状态与下一步。
- `openspec/` 保存主规格、活动 Change 和已归档 Change。

## 当前非目标

本基线不包含 Gateway、Resource、Booking、User 等业务服务，也不包含 Docker Compose、数据库 Migration、基础设施客户端或虚假的业务测试覆盖率。

下一项建议工作是创建 OpenSpec Change `add-base-infrastructure`，增量引入基础设施编排与可验证门禁。

