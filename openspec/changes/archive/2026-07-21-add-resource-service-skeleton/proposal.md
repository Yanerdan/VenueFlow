## Why

VenueFlow 已完成可复现工程基线与最小基础设施，但 Maven reactor 中仍没有可启动的业务服务，因而无法验证服务模块边界、Spring Boot 4 启动路径或 Actuator 探针契约。C03 需要以最小、独立可验收的 Resource Service 骨架完成 v0.1.0 工程里程碑，并为后续资源与预约业务 Change 提供稳定落点。

## What Changes

- 新增 `venueflow-resource-service` 可执行 Spring Boot MVC 模块并纳入根 Maven reactor。
- 只引入启动与健康探针所需依赖，提供独立应用入口和显式应用标识。
- 配置 Actuator liveness/readiness，只公开最小健康信息，不暴露危险管理端点。
- 增加不依赖 Docker、数据库、Nacos、Redis 或 RabbitMQ 的启动与 HTTP 探针测试，并继续由默认 `clean verify` 执行。
- 更新 README、CI 验收说明和 HANDOFF，使开发者能够启动、验证并理解 C03 的边界。
- 明确排除资源 API、Entity、Mapper、Migration、容量逻辑、基础设施客户端、服务发现和消息拓扑；这些能力由后续独立 Change 引入。

## Capabilities

### New Capabilities

- `resource-service-skeleton`: 定义最小 Resource Service 模块、可执行启动入口、安全的 Actuator 健康探针、离线构建与自动化验收边界。

### Modified Capabilities

无。

## Impact

- Maven reactor 新增一个 Spring Boot 应用模块，并复用现有 BOM、JDK 21 与质量插件基线。
- 新增 Resource Service 源码、配置和测试；默认构建项目数与测试数将增加。
- CI 的 Maven verify 自动覆盖新模块，但仍不要求基础设施 job 或本地 Docker 先行启动。
- 不改变现有 Compose、主规格中的基础设施契约，也不新增数据库 schema、外部业务 API 或跨服务契约。
