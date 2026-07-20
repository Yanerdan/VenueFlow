# project-bootstrap Specification

## Purpose

定义 VenueFlow 最小、可复现且不依赖业务服务或外部基础设施的工程骨架，包括构建入口、版本基线、模块边界、质量门禁、仓库卫生、交接文档与 CI 验收要求。

## Requirements
### Requirement: Reproducible Maven entry point
仓库 MUST 提交 Maven Wrapper，Windows 和 Linux MUST 能在不依赖全局 Maven版本的情况下运行构建；Wrapper MUST 固定 Maven 3.9.16。

#### Scenario: Build on Windows
- **WHEN** 开发者在配置了 JDK 21 的 Windows 新终端运行 `mvnw.cmd clean verify`
- **THEN** Wrapper 使用固定 Maven版本并完成当前全部模块验证

#### Scenario: Build in Linux CI
- **WHEN** GitHub Actions 在 Linux runner运行 `./mvnw clean verify`
- **THEN** 构建使用同一 Wrapper版本且不依赖 runner预装 Maven

### Requirement: Frozen Java and dependency baseline
根父工程 MUST 要求 JDK 21并集中声明 Spring Boot 4.0.7、Spring Cloud 2025.1.2、Spring Cloud Alibaba 2025.1.0.0和 MyBatis-Plus 3.5.17；子模块不得自行漂移这些核心版本。

#### Scenario: Reject an unsupported Java runtime
- **WHEN** 使用低于 JDK 21的运行时执行构建
- **THEN** Maven Enforcer 在编译前以明确错误终止构建

#### Scenario: Resolve managed dependencies
- **WHEN** 子模块声明由内部 BOM 管理的依赖且不指定版本
- **THEN** effective POM解析到工程冻结的兼容版本

### Requirement: Minimal module boundary
bootstrap Change MUST 只创建聚合父工程、内部依赖 BOM和最小非业务公共模块，不得创建业务服务、基础设施客户端或共享业务 Entity。

#### Scenario: Inspect the bootstrap reactor
- **WHEN** 审查根 POM及其模块列表
- **THEN** 不存在 Gateway或任何业务 Service模块，公共模块也不包含资源、预约或用户领域实现

### Requirement: Executable quality gate
默认 `clean verify` MUST 执行编译、单元测试、集成测试发现规则、覆盖率、依赖收敛、重复类检查、格式检查和适用于当前代码的静态检查；插件版本 MUST 固定。

#### Scenario: Reject an unformatted source
- **WHEN** Java源码不符合仓库格式规范
- **THEN** `clean verify` 在格式检查阶段失败并指出文件

#### Scenario: Run a real baseline test
- **WHEN** 执行 `clean verify`
- **THEN** 最小公共模块至少运行一个有业务无关断言的真实 JUnit测试，而不是只报告零测试成功

### Requirement: Repository hygiene
仓库 MUST 统一 UTF-8、LF和末尾换行，忽略 IDE、构建产物、日志、本地环境文件和 secrets，并且只提交不含真实密钥的 `.env.example`。

#### Scenario: Inspect tracked environment files
- **WHEN** 检查 Git跟踪文件
- **THEN** `.env`、密钥、Token和本机绝对路径未被提交，而 `.env.example` 只包含占位值

#### Scenario: Checkout on Windows
- **WHEN** Windows Git 配置启用自动换行转换
- **THEN** `.gitattributes` 仍保证仓库中的文本规范化为 LF

### Requirement: Baseline documentation and handoff
仓库 MUST 提供 README、版本清单、ADR模板和 `.agent/HANDOFF.md` 模板，并说明 JDK 21、Wrapper命令、当前模块、非目标和下一 Change。

#### Scenario: Onboard from a clean checkout
- **WHEN** 新开发者只阅读 README和 HANDOFF
- **THEN** 可以识别环境要求、执行验证命令、理解当前没有业务服务，并知道下一步是 `add-base-infrastructure`

### Requirement: Scope-appropriate CI
仓库 MUST 提供 GitHub Actions CI skeleton，在 push和 pull request上使用 JDK 21与 Maven Wrapper运行当前可执行门禁；CI不得宣称或伪造尚未实现的容器、Migration或业务测试结果。

#### Scenario: Validate the initial repository in CI
- **WHEN** 提交触发 GitHub Actions
- **THEN** CI执行 Wrapper验证与 `clean verify`，并上传或保留足以定位失败的构建结果
