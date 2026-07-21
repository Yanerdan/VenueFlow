## Context

当前 reactor 只有内部 BOM 与业务无关的 common 聚合模块；C02 的 MySQL、Redis、RabbitMQ、Nacos 也保持为独立的按需基础设施。工程总规格把 C03 定义为 v0.1.0 的最后一个有序 Change：新增最小 Resource Service 与 Actuator，但业务闭环、持久化和微服务治理属于后续版本。

本设计需要同时证明三件事：Spring Boot 4 应用模块能够继承现有 JDK/BOM/质量门禁；服务在没有 Docker 和外部配置中心时可以独立构建并启动；健康探针具备明确而有限的暴露面。开发者、CI 和后续 Resource/Booking Change 都依赖这一骨架保持稳定。

## Goals / Non-Goals

**Goals:**

- 建立一个纳入根 reactor、可打包为可执行 jar 的 `venueflow-resource-service`。
- 以默认端口 8083 和可覆盖的标准 Spring 配置提供确定的本地启动方式。
- 提供 HTTP liveness/readiness，并将 Actuator Web 暴露限制在健康能力。
- 使用真实 Spring 上下文和 HTTP 请求验证启动、应用名、探针与暴露边界。
- 保持默认 `clean verify` 完全不依赖 Docker 或任何中间件。

**Non-Goals:**

- 不提供资源 CRUD、分类、规则、时段或容量 API。
- 不创建 Entity、DTO、Mapper、数据库、用户、Flyway Migration 或测试数据。
- 不引入 Nacos、MyBatis-Plus、MySQL、Redis、RabbitMQ、Feign、Sentinel、安全/JWT、Tracing 或 Prometheus 客户端。
- 不创建 Gateway、contract 聚合、额外 common 子模块、应用容器镜像或 Compose app profile。

## Decisions

### 1. 以单一可执行模块建立最小服务边界

根 POM 直接聚合 `venueflow-resource-service`，模块继承 `venueflow-parent` 并声明 Spring Boot Maven Plugin，以生成可执行 jar。生产代码只包含位于 `com.yanerdan.venueflow.resource` 根包的应用入口和必要配置，不为未来分层预建空类。

选择单模块而不是同时创建 resource contract、domain、application、adapter 等子模块，是因为 C03 只验证服务外壳；提前拆分会把尚未形成的业务边界固化为目录。后续业务 Change 可在真实用例出现后增加分层或契约模块。

### 2. 只引入 Web 与 Actuator 启动依赖

运行依赖限定为 `spring-boot-starter-web` 和 `spring-boot-starter-actuator`，测试使用 `spring-boot-starter-test`。骨架不依赖 common-core，因为当前没有要复用的错误模型或 API；无实际用途的依赖会模糊模块边界。

选择 MVC 是为了遵循总规格的业务服务依赖边界；WebFlux 仅属于 Gateway。暂不引入 validation、security、数据库和治理 starters，因为没有对应行为可验证。

### 3. 使用本地确定性配置，不连接 C02 基础设施

提交的 `application.yml` 只声明 `spring.application.name: venueflow-resource-service`、`server.port: ${SERVER_PORT:8083}` 和 Actuator 健康配置。默认 profile 不导入 Nacos，不读取 `.env`，也不声明任何数据源、缓存或消息连接。

8083 来自总规格中 8081~8086 的服务端口序列，并为 Resource Service 保留固定默认值；测试使用随机端口避免并发 CI 端口冲突。标准 `SERVER_PORT` 覆盖机制比新增项目专用端口变量更符合 Spring Boot 运行约定。

### 4. 将 Actuator 暴露面限制为健康端点

启用 liveness/readiness probe groups，Web exposure 只包含 `health`，健康详情不向未认证调用者显示。C03 不暴露 `env`、`configprops`、`loggers`、`mappings`、`metrics` 或写端点，也不另开管理端口。

不在本 Change 引入 Spring Security：当前没有 JWT/授权配置，加入后只能形成虚假安全骨架。最小暴露白名单先缩小攻击面，后续安全 Change 再为受保护管理端点增加认证和网络策略。

### 5. 用上下文测试与真实 HTTP 探针形成验收

测试应至少覆盖：应用上下文在无外部基础设施时启动；随机端口上的 `/actuator/health/liveness` 与 `/actuator/health/readiness` 返回成功和 `UP`；敏感或未暴露端点不能通过 Web 访问；应用名解析为固定值。测试不得通过 mock Controller 伪造探针，也不得启动 C02 Compose。

默认 Maven `clean verify` 继续作为统一门禁，CI 的现有 verify job 会自然覆盖新增模块。Spring Boot Plugin 的 repackage 目标与构建产物检查共同证明 jar 布局；本地启动命令作为 README 验收入口。

### 6. 用范围检查防止骨架偷渡业务实现

实现审查同时检查依赖树和源目录：不得出现 JDBC/MyBatis/Flyway/Nacos/Redis/AMQP/Feign 等客户端，不得出现 Resource API、Entity、Mapper、Migration 或业务表。该限制写入 spec、tasks 与 HANDOFF，而不是依赖空包或占位接口表达未来设计。

## Risks / Trade-offs

- [Actuator 在没有 Security 时可匿名访问] → 只暴露不含详情的 health，并以测试锁定敏感端点不可访问；认证在后续安全 Change 引入。
- [骨架过小，后续会修改模块结构] → 当前只固定应用边界与运行契约，不预设业务分层，降低未来重构成本。
- [默认 8083 可能与本机进程冲突] → 支持 `SERVER_PORT` 覆盖，自动化测试始终使用随机端口。
- [Spring 上下文测试增加构建时间] → 只保留一组集中式启动/HTTP 验收，不复制多套等价上下文测试。
- [仅测试类路径启动不能完全覆盖发行 jar] → 声明 Boot repackage、检查非 plain jar 产物，并在 README 提供有界的 `java -jar` 启动验收命令。

## Migration Plan

1. 将服务模块加入 reactor，并建立最小 POM、应用入口和配置。
2. 添加上下文、HTTP 探针、暴露边界及依赖范围测试。
3. 执行模块级测试和全仓 `clean verify`，再更新 README、CI 说明与 HANDOFF。
4. 若回滚，移除根 POM 中的模块声明及新模块目录即可；本 Change 没有数据库、消息、配置中心或数据迁移状态需要恢复。

## Open Questions

无阻断问题。业务 API、持久化、服务发现、安全和可观测性细节必须在后续 Change 中依据真实需求单独决策。
