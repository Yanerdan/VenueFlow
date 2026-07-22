## Why

Resource Service 目前只能启动和报告健康状态，尚不能管理任何园区资源事实数据。v0.2.0 的预约闭环以可审计、独立拥有的资源目录为前提，因此需要先建立 MySQL/Flyway 持久化边界和最小资源目录 API，而不是直接进入时段、容量或 Booking。

## What Changes

- 为 `venueflow-resource-service` 增加仅在显式 persistence profile 中启用的 MySQL、Flyway 和 MyBatis-Plus 持久化能力；数据源凭据只来自未提交环境变量。
- 建立 `venueflow_resource` 独立 schema、最小权限用户和不可修改的 `V001__init_resource_catalog.sql`，保存 Category 与 Resource 事实数据、状态和乐观锁版本。
- 增加 Resource Category/Resource 的管理写入、详情查询和有界分页查询 API；Controller 使用请求/响应 DTO，不直接暴露 Entity 或调用 Mapper。
- 为输入校验、统一错误响应、唯一资源编号、状态转换和分页边界增加自动化测试；另提供基于真实 MySQL 的 opt-in migration/API 集成验收，默认 Maven `clean verify` 继续不要求 Docker。
- 更新 C03 skeleton 主规格：保留默认无持久化的 `skeleton` 启动模式，同时允许显式 profile 为已接受的资源目录能力引入数据库依赖。
- 明确排除 Slot、容量占用/释放、Booking、认证/授权、Nacos、Redis、RabbitMQ、Feign、事件、搜索和容器化应用编排。

## Capabilities

### New Capabilities

- `resource-catalog`: 定义 Resource Service 对 Category 与 Resource 事实数据的独立持久化、管理 API、查询契约、错误语义和真实 MySQL 验收。

### Modified Capabilities

- `resource-service-skeleton`: 调整最小依赖与配置要求，使 C03 的 standalone skeleton 保持可验证，同时允许 C04 的显式 persistence profile 使用已验收的数据库能力。

## Impact

- `venueflow-resource-service` 将新增 Validation、MyBatis-Plus、MySQL Connector/J 和 Flyway 依赖、配置、迁移、领域内 Entity/Mapper/Service/API DTO 与测试。
- 本地 `.env`、Runbook 和独立 MySQL 集成测试入口将需要新增 resource 数据库连接参数；真实凭据不提交。
- 根 Maven 默认验证保持 Docker-free；真实 MySQL 迁移/API 验收通过显式 profile、脚本或隔离 CI job 执行。
- 不新增其他服务、跨服务契约、消息拓扑或共享业务模型。
