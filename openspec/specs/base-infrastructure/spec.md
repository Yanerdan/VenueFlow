# base-infrastructure Specification

## Purpose

定义 VenueFlow 最小基础设施 Compose 的版本锁定、profile、持久化、健康检查、安全暴露和可重复验收要求，为后续业务服务提供可复现且适合低内存开发环境的运行底座。

## Requirements

### Requirement: Deterministic base infrastructure composition
仓库 MUST 提供 Docker Compose v2 可解析的显式 `base` profile，且该 profile MUST 只包含 MySQL 8.4 LTS、Redis 7.4、RabbitMQ 4.1 management 和 Nacos 3.1.1；每个镜像 MUST 通过版本文件引用经过验证的精确非 `latest` 标签。

#### Scenario: Render the base profile
- **WHEN** 开发者使用已提交的版本文件和 secret-safe 示例环境渲染 Compose 配置
- **THEN** 配置只包含四个基础组件，所有镜像均解析为允许基线内的精确非 `latest` 标签

#### Scenario: Reject a floating image
- **WHEN** 任一基础组件使用 `latest`、缺失标签或只表达允许漂移的版本范围
- **THEN** 基础设施静态门禁在启动容器前失败并指出对应组件

### Requirement: Profile-scoped low-memory operation
基础设施 MUST 要求显式选择 `base` profile，并 MUST 为每个组件声明适合 4~5GB infra-node 的 CPU/内存边界；Elasticsearch、观测组件和应用容器 MUST NOT 随 base profile 启动。

#### Scenario: Start on a 16GB development host
- **WHEN** 开发者在 16GB 宿主机按 Runbook 启动 `base` profile
- **THEN** 仅四个基础组件启动，配置的总资源预算不要求 Elasticsearch、观测栈或任何 Java 服务同时常驻

#### Scenario: Run Compose without selecting a profile
- **WHEN** 开发者未显式选择任何 Compose profile
- **THEN** 基础组件不会被默认整组启动

### Requirement: Parameterized and protected network exposure
基础设施宿主机端口 MUST 绑定到显式参数化地址，secret-safe 示例 MUST 默认使用回环地址；VMware 部署 MUST 可改为 Host-only 地址而无需修改已提交 Compose，且配置 MUST NOT 默认把中间件暴露到所有接口或互联网。

#### Scenario: Use local Docker safely
- **WHEN** 开发者使用示例默认绑定地址启动 base profile
- **THEN** MySQL、Redis、RabbitMQ、Nacos 及管理端口仅从本机回环接口可达

#### Scenario: Use an infra-node Host-only address
- **WHEN** 操作者在未提交的 `.env` 中将绑定地址设置为 infra-node 的 Host-only IP
- **THEN** 相同 Compose 文件在该地址发布端口且无需硬编码当前 `192.168.72.0/24` 网段

### Requirement: Secret-safe authenticated configuration
MySQL、Redis、RabbitMQ 和 Nacos MUST 使用各自支持的认证配置，实际凭据 MUST 仅来自未提交环境或 CI Secret Store；已提交示例、日志和诊断输出 MUST NOT 包含可用的真实 secret。

#### Scenario: Detect missing local credentials
- **WHEN** 启动所需的认证变量缺失或仍为禁止使用的占位值
- **THEN** preflight 在启动容器前失败且不回显凭据内容

#### Scenario: Inspect tracked configuration
- **WHEN** 审查 Git 跟踪的 Compose、版本、文档和环境示例
- **THEN** 不存在真实密码、Token、私钥或本机专用 secret

### Requirement: Bounded component health verification
四个基础组件 MUST 声明有启动宽限、单次超时、固定间隔和有限重试的健康检查；验收入口 MUST 在全局超时内等待 healthy，并执行组件协议级只读冒烟检查。

#### Scenario: All base components become ready
- **WHEN** 精确镜像在受支持的 Docker 环境成功启动
- **THEN** MySQL、Redis、RabbitMQ 和 Nacos 在限定时间内达到 healthy，且 SQL、PING、diagnostics 和 liveness 检查全部成功

#### Scenario: A component remains unhealthy
- **WHEN** 任一组件在全局超时前未达到 healthy 或认证检查失败
- **THEN** 验收返回非零并输出有界的容器状态、health 详情和相关日志，不进入无限重试

### Requirement: Non-destructive persistent lifecycle
Compose MUST 为四个组件使用独立命名卷并固定 project identity；重复启动 MUST 收敛到同一组服务、网络和卷，普通停止、失败诊断和 CI 清理 MUST NOT 自动删除数据卷。

#### Scenario: Restart the base profile
- **WHEN** 操作者停止且不删除卷后重新启动同一 Compose project
- **THEN** 四个组件复用既有命名卷且不会创建第二套逻辑基础设施

#### Scenario: Run automated cleanup
- **WHEN** smoke 或 CI 完成并执行自动清理
- **THEN** 清理命令不包含卷删除，任何清卷操作仍要求人工确认明确的 project 与卷目标

### Requirement: Layered infrastructure quality gate
仓库 MUST 提供可重复的静态配置检查和运行时 smoke；默认 Maven `clean verify` MUST 保持不依赖 Docker，CI MUST 将基础设施校验放在独立 job 或 workflow 中并在失败时提供可诊断证据。

#### Scenario: Build without Docker
- **WHEN** 开发者在没有运行 Docker 或任何基础组件的环境执行 `mvnw clean verify`
- **THEN** 现有 Java 构建和质量门禁仍可独立完成

#### Scenario: Validate infrastructure in CI
- **WHEN** 具备 Docker Compose v2 的隔离 CI job 执行基础设施验收
- **THEN** job 先完成静态策略检查，再启动 base profile、执行有界 smoke，并在成功或失败后进行非破坏清理

### Requirement: C02 scope isolation
C02 MUST NOT 创建业务服务、Java 基础设施客户端、VenueFlow 数据库 Migration、业务数据库/用户、业务消息拓扑、Nacos 业务 Data ID、Elasticsearch 或观测栈配置。

#### Scenario: Inspect the C02 change scope
- **WHEN** 审查 C02 的 Git diff 和 Maven reactor
- **THEN** Java 模块与业务模型保持不变，新增内容仅覆盖基础设施编排、配置、校验脚本和相关文档
