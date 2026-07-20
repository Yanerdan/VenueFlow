# ADR-0001: 建立最小可复现工程基线

- 状态：Accepted
- 日期：2026-07-20
- 决策者：VenueFlow Team

## 背景

仓库原先只有工程规范和 OpenSpec 元数据，无法通过统一命令编译、测试或在 CI 中验证。开发环境同时存在多个 Java 版本，因此构建入口和版本边界必须显式固定。

## 决策

采用 JDK 21、Maven Wrapper 3.9.16 和多模块 Maven reactor。根 POM 统一冻结框架与插件版本，内部 BOM 管理依赖，最小公共模块证明编译和测试链路可执行。默认 `clean verify` 仅运行与外部基础设施无关的质量门禁。

## 备选方案

- 依赖全局 Maven：配置简单，但难以保证本机与 CI 使用同一版本。
- 一次生成全部业务服务：短期文件较多，但会在需求和边界尚未稳定时制造空壳与错误承诺。
- 首次构建即依赖 Docker：更接近运行环境，但当前没有可验证的业务或 Migration，门禁会失真。

## 影响

开发者可在 Windows 与 Linux 使用同一 Wrapper 入口。新增模块必须继承父 POM，并通过现有质量门禁。Docker、Migration、服务骨架和运行时配置留给后续独立 Change。

## 风险与缓解

- 第三方插件与 JDK 21 的兼容风险：固定插件版本并由 `clean verify` 持续验证。
- 公共模块过度膨胀：使用架构测试阻止业务包依赖进入 common。
- 旧终端仍使用旧 Java：README 要求先检查 Wrapper 的 Java 版本。

## 验证

```powershell
.\mvnw.cmd -version
.\mvnw.cmd clean verify
openspec validate --all --strict
```

## 复审日期

2026-10-20

