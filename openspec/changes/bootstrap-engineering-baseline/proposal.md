## Why

VenueFlow 当前只有工程规范和 OpenSpec 工件，尚不能通过标准命令编译、测试或在 CI 中验证。需要建立一个最小、可重复且无业务实现的 Maven 工程基线，为后续基础设施和服务 Change 提供稳定入口。

## What Changes

- 初始化 JDK 21、Maven Wrapper 和多模块聚合工程，固定 Spring Boot 4.0.7、Spring Cloud 2025.1.2、Spring Cloud Alibaba 2025.1.0.0 与 MyBatis-Plus 3.5.17。
- 创建依赖 BOM/约束模块与最小公共模块骨架，但不加入业务 Entity、Service 或基础设施客户端。
- 固定 compiler、Surefire、Failsafe、JaCoCo、Enforcer、格式化与基础静态检查插件版本。
- 建立可在 Windows 和 Linux 执行的基础测试与 `clean verify` 验收入口。
- 添加 `.editorconfig`、`.gitattributes`、`.gitignore`、`.env.example`、版本清单、ADR/HANDOFF 模板和 README。
- 添加最小 CI workflow，仅执行工程骨架当前具备的格式、编译、测试和验证步骤。
- 不创建 Docker Compose、数据库 Migration、Resource Service 或其他业务服务；这些属于后续 Change。

## Capabilities

### New Capabilities

- `project-bootstrap`: 定义 VenueFlow 工程骨架、版本锁定、构建门禁、仓库卫生和基础文档的可验证要求。

### Modified Capabilities

<!-- 本 Change 实施既有 engineering-baseline，不修改其要求。 -->

## Impact

- 新增根 `pom.xml`、Maven Wrapper、版本文件、基础模块和仓库级配置。
- 新增 README、ADR/HANDOFF 模板和 CI workflow。
- 构建环境要求 JDK 21；本机已安装 Temurin 21.0.11。
- 不改变 VMware、Docker、数据库、网络端口或应用运行状态。
