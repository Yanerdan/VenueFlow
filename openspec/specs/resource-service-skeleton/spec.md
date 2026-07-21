## Purpose

Define the runnable, infrastructure-independent Resource Service skeleton introduced in C03.

## Requirements

### Requirement: Reactor-integrated executable Resource Service
仓库 MUST 提供由根 Maven reactor 聚合的 `venueflow-resource-service` Spring Boot MVC 模块；该模块 MUST 继承既有 JDK 21、BOM 和质量门禁，并 MUST 生成具有独立应用入口的可执行 jar。

#### Scenario: Build the complete reactor
- **WHEN** 开发者在受支持的 JDK 21 环境执行 Maven Wrapper `clean verify`
- **THEN** Resource Service 随全部既有模块完成编译、测试、质量检查和可执行 jar 打包

#### Scenario: Start the packaged service
- **WHEN** 操作者使用 `java -jar` 启动构建得到的 Resource Service 可执行 jar
- **THEN** 服务以应用名 `venueflow-resource-service` 启动并接受 HTTP 健康探针请求

### Requirement: Minimal service dependency boundary
Resource Service 骨架 MUST 只引入启动 HTTP 应用、Actuator 和测试所必需的依赖，MUST NOT 引入 JDBC、MyBatis-Plus、Flyway、MySQL、Nacos、Redis、RabbitMQ、Feign、Sentinel、JWT、Tracing 或 Prometheus 客户端。

#### Scenario: Inspect the skeleton dependency tree
- **WHEN** 审查 Resource Service 的声明依赖和解析后的 Maven 依赖树
- **THEN** 运行时依赖仅服务于 Spring MVC 与 Actuator，且不存在尚无可验证用途的基础设施或治理客户端

### Requirement: Deterministic standalone configuration
Resource Service MUST 提交 secret-free 的最小本地配置，MUST 将应用名固定为 `venueflow-resource-service`，MUST 默认监听 8083，并 MUST 允许通过标准 `SERVER_PORT` 环境变量覆盖端口；默认启动 MUST NOT 要求 `.env`、Docker、Nacos 或其他外部服务。

#### Scenario: Start with defaults
- **WHEN** 开发者没有运行 C02 基础设施且未提供外部配置中心或数据源配置时启动服务
- **THEN** 服务使用应用名 `venueflow-resource-service` 和默认端口 8083 独立进入可用状态

#### Scenario: Override a conflicting local port
- **WHEN** 操作者设置有效的 `SERVER_PORT` 后启动服务
- **THEN** 服务监听指定端口且不需要修改已提交配置文件

### Requirement: Restricted Actuator health surface
Resource Service MUST 提供 `/actuator/health/liveness` 与 `/actuator/health/readiness` HTTP 探针，MUST 仅通过 Actuator Web 暴露健康能力，MUST NOT 匿名暴露 `env`、`configprops`、`loggers`、`mappings`、`metrics` 或危险写端点，并 MUST NOT 向匿名调用者显示组件健康详情。

#### Scenario: Probe a healthy standalone process
- **WHEN** 已启动服务收到 liveness 和 readiness 请求
- **THEN** 两个请求均返回成功响应且状态为 `UP`

#### Scenario: Request a sensitive management endpoint
- **WHEN** 匿名调用者请求未列入健康白名单的 Actuator Web 端点
- **THEN** 服务不提供该端点且不泄露环境、配置、日志或映射信息

### Requirement: Infrastructure-independent automated verification
Resource Service 的启动与 HTTP 探针测试 MUST 由默认 Maven `clean verify` 自动执行，MUST 使用隔离的随机端口，并 MUST 在 Docker、MySQL、Redis、RabbitMQ 和 Nacos 均未运行时仍可通过。

#### Scenario: Verify without the base profile
- **WHEN** CI 或开发者在未启动任何 Compose profile 的环境执行默认 Maven 验证
- **THEN** 测试启动真实 Spring 应用上下文、通过 HTTP 验证健康探针和暴露边界，并在有界时间内完成

#### Scenario: Run tests concurrently with another local service
- **WHEN** 默认 8083 已被其他进程占用但 Maven 测试使用随机端口
- **THEN** Resource Service 自动化测试不因固定端口冲突而失败

### Requirement: C03 scope isolation
C03 MUST NOT 创建资源业务 API、Entity、DTO、Mapper、数据库或用户、Flyway Migration、容量占用/释放逻辑、基础设施客户端、服务发现配置、消息拓扑、其他业务服务或应用容器编排。

#### Scenario: Inspect the C03 implementation diff
- **WHEN** 审查新增源码、配置、依赖和部署文件
- **THEN** 变更仅包含最小 Resource Service 外壳、Actuator 验收及相应工程文档，不包含资源业务或后续阶段能力
