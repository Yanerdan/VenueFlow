## Context

`venueflow-resource-service` 已在 C03 中建立为可独立启动的 Spring Boot 服务，但它只提供健康检查，尚未拥有资源事实数据。v0.2.0 的后续时段、容量和 Booking 都必须以稳定的资源目录为前提；因此 C04 只负责将 Category 与 Resource 的最小事实模型落到 Resource Service 自己拥有的 MySQL schema 中。

C03 的默认启动与验证刻意保持 Docker-free。C04 不能把数据库连接变成所有开发、单元测试和根 Maven 验证的隐含前提，但也不能用内存伪实现替代 MySQL/Flyway 的真实验收。

## Goals / Non-Goals

### Goals

- 建立 `venueflow_resource` 的独立 schema、最小权限数据库用户和可从空库执行的 Flyway V001 迁移。
- 持久化 Category 与 Resource 的资源事实数据，包括唯一资源编号、资源状态、容量和乐观锁版本。
- 提供受校验的 Category 创建/查询、Resource 创建/详情/分页查询及状态转换 API。
- 让 Controller、应用服务和 MyBatis-Plus 持久化实现保持边界清晰，API 不泄露 Entity 或 Mapper。
- 保留 C03 的默认 standalone 验证；另提供显式启用、连接真实 MySQL 的迁移/API 集成验收。

### Non-Goals

- 不实现 ResourceSlot、时段模板、容量占用或释放、排班、冲突检测。
- 不实现 Booking、审批、认证、授权、审计事件、幂等键、消息、缓存、搜索或跨服务调用。
- 不引入 Nacos、Redis、RabbitMQ、Feign、Sentinel、JWT/安全框架、可观测性客户端或容器化应用编排。
- 不提供 Category 删除、Resource 通用编辑或资源编号自动生成；管理员显式提供资源编号，后续 Change 再扩展。

## Decisions

### 1. 将真实持久化限定为显式 `persistence` profile

默认配置使用 `skeleton` profile，仍可在没有 Docker、MySQL 或任何外部服务时启动和通过 `mvn clean verify`。只有通过显式 `persistence` profile 启动时，才读取数据源环境变量、启用 Flyway 与 MyBatis-Plus，并暴露资源目录的可用持久化实现。

数据源 URL、用户名和密码只从环境变量或未提交的本地环境文件读取。已跟踪的 YAML 只包含变量占位符、profile 选择和非敏感默认值，不能保存真实凭据。`ddl-auto` 保持 `validate` 或禁用，结构只能由 Flyway 管理。

这会让默认模式不具备目录写入能力；调用方必须显式选择 persistence 模式。这样避免把本地离线骨架伪装成可写业务服务，也避免 CI 因 Docker 缺失而不稳定。

### 2. Resource Service 独立拥有数据库和迁移

Resource Service 仅访问 `venueflow_resource` schema 与其最小权限用户。`V001__init_resource_catalog.sql` 是建立 `resource_category`、`resource` 和必要唯一索引/外键的唯一初始入口；一旦合并即视为不可修改，后续结构变更新增版本化迁移。

`resource` 保存 `id`、`resource_no`、`category_id`、`name`、`description`、`location`、`capacity`、`status`、`version`、`created_at` 与 `updated_at`。`resource_no` 全局唯一，`capacity` 为正整数，状态限定为 `DRAFT`、`ACTIVE`、`SUSPENDED`、`ARCHIVED`。本 Change 不创建 slot、allocation 或 booking 表。

### 3. 以 DTO/API、应用服务、持久化适配器分层

HTTP 层只接收请求 DTO、返回响应 DTO，并执行 Bean Validation；它不返回数据库 Entity，也不直接调用 Mapper。应用服务负责类别存在性、资源编号唯一性、状态转换和错误映射。Entity 与 MyBatis-Plus Mapper 位于 Resource Service 自己的持久化适配层，不进入 `venueflow-common`。

对单表 CRUD 可使用 MyBatis-Plus `BaseMapper`；涉及分页筛选、乐观锁条件更新或状态转换的 SQL 必须显式表达并有测试覆盖。状态转换使用调用方提交的 `expectedVersion` 作条件更新；影响行数为零时，应区分资源不存在和版本冲突。

### 4. 收敛为最小、可演进的资源目录 API

本 Change 提供下列 API，统一以 `/api/v1` 为前缀：

- `POST /resource-categories`：创建类别。
- `GET /resource-categories`：列出类别，供资源录入选择。
- `POST /resources`：创建资源，初始状态固定为 `DRAFT`。
- `GET /resources/{resourceId}`：查询资源详情。
- `GET /resources`：按可选 `categoryId`、`status` 筛选的有界分页查询；默认页大小 20、最大 100。
- `PATCH /resources/{resourceId}/status`：按声明的状态图进行状态转换，并携带 `expectedVersion`。

允许的状态图为：`DRAFT -> ACTIVE | ARCHIVED`；`ACTIVE -> SUSPENDED | ARCHIVED`；`SUSPENDED -> ACTIVE | ARCHIVED`；`ARCHIVED` 为终态。C04 的“管理”只是业务接口命名，不包含任何认证/授权承诺。

重复的资源编号返回稳定的冲突错误；未知类别或资源返回未找到错误；非法字段、分页参数、状态转换或版本返回稳定的客户端错误。错误响应采用 `code`、`message`、`details`、`traceId`、`timestamp` 字段，且不得暴露 SQL、堆栈或密码。

### 5. 区分默认验证与真实 MySQL 验收

默认 `mvn clean verify` 只执行不依赖 Docker 的单元/API 边界测试，并继续验证 skeleton 可启动。新增 `mysql-it` Maven profile 用于显式执行真实 MySQL 的 Flyway/API 集成测试；该 profile 可以要求 Docker，并应使用隔离的临时数据库而不是开发者已有数据库。

真实集成测试从空库开始，验证 V001 可以执行、唯一资源编号和外键受到数据库保护、API 的创建/查询/状态转换可用。测试不应复用生产凭据，也不应让默认 CI 因缺少 Docker 而失败。

## Risks / Trade-offs

- 显式 profile 让首次运行多一步配置，但避免“看似可用、实际未持久化”的默认服务。
- V001 的表结构一旦进入共享环境不能回改，因此上线前需要审查命名、索引、字符集和时间字段；后续只追加迁移。
- `mysql-it` 依赖 Docker，故只在开发者显式执行或具备 Docker 的隔离 CI job 中运行。
- C04 暂无认证，目录写接口只能用于受控开发环境；在对外暴露前必须由后续安全 Change 接管访问控制。
- 固定的最小状态图可保证后续 Booking 依赖稳定，但通用资料编辑与类别停用需要后续 Change 设计。

## Migration Plan

1. 创建持久化 profile 的非敏感配置和数据库环境变量说明，不提交任何真实密码。
2. 在全新 `venueflow_resource` schema 执行 `V001__init_resource_catalog.sql`；该文件合并后禁止改写。
3. 使用 `mysql-it` 从空库验证迁移、目录 API、唯一约束、外键和乐观锁冲突。
4. 先在开发环境部署 persistence profile，再由后续 Change 引入 Slot 与 Booking；不存在从旧资源表导入的数据迁移。

## Open Questions

- 无。资源编号由创建请求显式提供，认证与更丰富的资料编辑均留给后续 Change。
