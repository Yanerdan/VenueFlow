## Context

规范修订已归档，`engineering-baseline` 已成为主规格。仓库目前没有 `pom.xml`、Maven Wrapper、Java源码、Git友好文件或 CI，唯一提交主要包含工程规范和 OpenSpec 元数据。本机已安装 Temurin 21.0.11 与 Maven 3.9.16，但工程不能依赖全局 Maven 或个人绝对路径。

本 Change 是 v0.1.0 的 C01，只建立可验证工程骨架；C02 才引入基础 Compose，C03 才创建 Resource Service Skeleton。

## Goals / Non-Goals

**Goals:**

- 让 Windows 和 Linux 都能以仓库内 Wrapper 执行 `clean verify`。
- 集中冻结 Java、Spring和构建插件版本，阻止依赖漂移和错误 Java 版本。
- 提供一个最小 Java 模块与真实单元测试，证明编译、测试、覆盖率聚合和 CI 链路有效。
- 建立 LF、UTF-8、忽略规则、环境变量示例、ADR/HANDOFF和 README 基线。
- 保持模块边界简洁，为后续服务逐步加入留出稳定父工程。

**Non-Goals:**

- 不创建 Gateway、Auth、User、Resource、Booking、Notification 或 Search Service。
- 不引入 MySQL、Redis、RabbitMQ、Nacos、Elasticsearch 的运行配置或客户端代码。
- 不创建 Dockerfile、Compose、Flyway Migration 或业务 API。
- 不在本 Change 中运行故障演练、压测或完整供应链扫描。

## Decisions

1. **Maven Wrapper 固定 3.9.16。** 本机 Maven 仅用于生成/验证 Wrapper；所有项目命令使用 `mvnw.cmd` 或 `./mvnw`。相比依赖全局 Maven，这可确保 CI 和新机器复现。

2. **根 POM 作为聚合父工程。** 根 POM 继承 Spring Boot Starter Parent 4.0.7，负责模块聚合、Java 21、编码、统一版本属性、插件管理和质量门禁。业务依赖不放入根 `<dependencies>`。

3. **建立内部 BOM 与最小公共模块。** `venueflow-dependencies` 集中导入 Spring Cloud、Spring Cloud Alibaba 和 MyBatis-Plus BOM；`venueflow-common/venueflow-common-core` 只包含与业务无关的最小类型及测试。此结构验证 reactor 构建，但不预生成所有服务。

4. **默认 verify 保持离线基础设施无关。** 默认门禁包含编译、JUnit、Failsafe发现规则、JaCoCo、Enforcer、Spotless和基础静态分析；Docker、Testcontainers、镜像和 Compose 门禁在具备相应模块后由后续 Change 增加。相比首次就加入空壳 Docker 门禁，这能保证验收真实且稳定。

5. **仓库统一 LF 与 UTF-8。** `.gitattributes` 对文本强制 LF，`.editorconfig` 统一 UTF-8/末尾换行；解决当前 Windows `core.autocrlf` 警告。Wrapper 的 Windows脚本保留适合 Windows 的处理规则。

6. **CI 只做当前范围可证明的工作。** GitHub Actions 使用 JDK 21 和 Wrapper运行 `clean verify`，检查 Wrapper 完整性并缓存 Maven仓库。安全扫描、镜像构建和 Compose smoke 在对应能力落地后扩展。

7. **版本事实分层。** `.version/stack-versions.yml` 记录架构版本基线；POM/Wrapper是构建实际版本；后续 `deploy/versions.env` 与 digest lock 由 C02 创建。避免在没有镜像时生成虚假的 digest 文件。

## Risks / Trade-offs

- [Spring Boot 4 与第三方 Maven插件存在新版本兼容风险] → 在实现任务中先解析 effective POM，并逐个启用插件；任何不兼容按 Stop Condition暂停。
- [公共模块过早膨胀] → 只加入无业务含义的核心标识/错误契约示例和测试，使用 ArchUnit或结构测试阻止业务类型进入。
- [默认 verify 未包含容器和漏洞扫描] → 明确这是 C01 范围；C02及安全专项 Change 增量增加门禁，不宣称当前已经覆盖。
- [Windows 用户环境仍可能有旧 Java位于系统 PATH 前部] → Wrapper/Maven以 `JAVA_HOME` 为准，README要求新终端验证 `mvnw.cmd -version` 显示 Java 21。

## Migration Plan

1. 创建仓库卫生、版本和文档基线。
2. 生成 Maven Wrapper并校验 Wrapper下载 URL和 SHA-256 配置。
3. 创建根父工程、内部 BOM、最小公共模块和测试。
4. 逐项启用构建门禁并运行 `mvnw.cmd clean verify`。
5. 添加 CI并进行本地 YAML/结构检查。
6. 更新 HANDOFF和任务状态；不自动提交，除非用户另行要求。

回滚时只需移除本 Change 新增的工程骨架文件；没有数据库、虚拟机或运行环境数据迁移。

## Open Questions

- SpotBugs、Spotless、JaCoCo、Surefire/Failsafe、Enforcer和CycloneDX的最终补丁版本在实现时以 Maven Central 可解析且与 JDK 21兼容为准，并写入根 POM。
- `venueflow-common-test` 是否在 C01 创建取决于是否存在两个以上模块需要共享测试工具；否则推迟到首次真实复用时。
