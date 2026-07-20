## Why

当前工程规范包含若干会阻止首次落地的问题：Spring Boot 4 主线与 Elasticsearch 8.18.1 不匹配，VMware 地址和资源配置与本机环境不一致，预约幂等流程缺少可审计的容量占用记录，并且 v0.1.0 与首次 Change 的实施顺序相互矛盾。应在工程骨架生成前修订这些约束，避免把已知冲突固化到代码和部署配置中。

## What Changes

- 将文档版本升级为 1.1.0，并记录本次兼容性与环境基线修订。
- 保留 Spring Boot 4.0.7 / Spring Cloud 2025.1.2 / Spring Cloud Alibaba 2025.1.0.0 主线，将 Elasticsearch 基线改为与 Spring Data 2025.1 对齐的 9.2.x，并要求首次使用前锁定具体补丁。
- 将 VMware Host-only 地址改为环境参数，补充当前 16GB 宿主机的轻量、集成和演练运行模式。
- 完善预约幂等键作用域、请求指纹、容量占用明细与跨服务操作 ID 约束。
- 统一预约待确认状态名，明确“确认”仅为模拟履约确认而非真实支付。
- 将 v0.1.0 拆成三个有序 Change，消除与首次 Change 规则的矛盾。
- 增加首次仓库基线建立前的一次性治理例外，并补充 Windows 使用 `openspec.cmd` 的约束。

## Capabilities

### New Capabilities

- `engineering-baseline`: 定义可执行的技术版本、宿主机/VMware运行模式、预约一致性基线和首次 OpenSpec 引导规则。

### Modified Capabilities

<!-- 当前尚无已归档 capability。 -->

## Impact

- 修改根目录 `VENUEFLOW_FULL_CHAIN_ENGINEERING_SPEC.md`。
- 新增本 Change 的 proposal、spec、design 和 tasks 工件。
- 不修改应用代码、数据库、虚拟机、Docker 配置或 Git 分支。
- 后续 `bootstrap-engineering-baseline`、基础设施和 Resource Service Change 必须采用修订后的约束。
