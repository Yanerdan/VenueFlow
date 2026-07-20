# VenueFlow Handoff

- 更新时间：2026-07-20 20:10:00 +08:00
- 分支：`main`
- 当前 Change：`bootstrap-engineering-baseline`
- 当前目标：完成 v0.1.0 C01 最小工程基线，不引入业务或运行时基础设施。

## 已完成

- 仓库文本、忽略规则、环境变量示例与版本清单。
- Maven Wrapper 3.9.16，含发行包 SHA-256 校验。
- JDK 21 多模块 Maven reactor、内部 BOM 与最小 common-core 模块。
- Enforcer、Surefire、Failsafe、JaCoCo、Spotless、SpotBugs 和 CycloneDX 门禁。
- 3 个真实测试，包括错误码契约和公共模块架构边界。
- README、ADR 和 GitHub Actions CI 基线。

## 验证记录

- `.\mvnw.cmd -version`：Maven 3.9.16，Temurin Java 21.0.11。
- `./mvnw -version`（Git Bash）：Maven 3.9.16，Temurin Java 21.0.11。
- `.\mvnw.cmd clean verify`：通过；4 个 reactor 项目成功，3 个测试通过。
- 负向单测校验：临时错误断言使 Surefire 返回退出码 1，随后已恢复源码。
- `help:effective-pom`：已解析 Java 21 和冻结的框架/插件版本。
- JDK 17 负向校验：Enforcer 在编译前失败并明确提示需要 JDK 21。
- `openspec validate --all --strict`：2 项通过，0 项失败。
- 仓库卫生检查：LF 属性生效，无未忽略的构建产物、密钥文件或本机绝对路径。

## 待完成

- 本 Change 的 12 项实现任务已全部完成。
- 由用户决定是否同步主规格、归档并提交/推送。

## 下一步

完成并归档本 Change 后，创建 `add-base-infrastructure`，再引入 Docker Compose、基础设施版本锁定、健康检查和相应验收门禁。

## 禁止操作

- 不在本 Change 中创建业务服务、Entity、Mapper、数据库 Migration 或基础设施客户端。
- 不提交 `.env`、私钥、Token、构建产物或本机绝对路径。
- 不绕过 `clean verify` 或 OpenSpec 严格校验。
