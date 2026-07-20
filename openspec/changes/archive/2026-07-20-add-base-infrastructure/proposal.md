## Why

VenueFlow 已完成可复现的工程基线，但尚无可启动、可检查的运行时基础设施，后续 Resource Service 无法在受控环境中验证数据库、缓存、消息和服务注册依赖。C02 需要建立一个适合 16GB 宿主机、版本固定且不会误暴露到公网的最小基础设施底座。

## What Changes

- 新增 Docker Compose `base` profile，编排 MySQL 8.4 LTS、Redis 7.4、RabbitMQ 4.1 management 和 Nacos 3.1.1；不引入 Elasticsearch、观测栈或应用容器。
- 新增 `deploy/versions.env`，将首次验证可用的镜像固定到精确标签；所有 Compose 镜像禁止使用 `latest`，并为后续 digest 锁定保留清晰入口。
- 为四个组件配置持久化命名卷、资源预算、restart 策略、健康检查和基于健康状态的验收脚本。
- 扩展环境变量示例和部署文档，使本机与 VMware infra-node 均可通过参数化地址、端口和本地 secret 启动，不把当前 VMnet1 地址当成全局事实。
- 新增静态配置检查和 Compose 冒烟门禁，验证配置可解析、版本无漂移、组件可达且基础健康；默认 Maven `clean verify` 继续保持不依赖 Docker。
- 明确安全边界：中间件仅绑定显式的内网/本机地址，凭据只来自未提交的 `.env`，不提交真实 secret。
- 不创建业务服务、基础设施客户端、数据库/用户初始化脚本、Flyway Migration、业务队列/交换机、Nacos 业务配置或任何业务数据。

## Capabilities

### New Capabilities

- `base-infrastructure`: 定义 VenueFlow 最小基础设施 Compose 的版本锁定、profile、持久化、健康检查、安全暴露和可重复验收要求。

### Modified Capabilities

<!-- 本 Change 实施既有 engineering-baseline 的分阶段与 16GB 环境约束，不修改既有规格要求。 -->

## Impact

- 新增 `deploy/compose/`、`deploy/versions.env`、基础设施配置、冒烟/静态校验脚本与部署 Runbook；更新 `.env.example`、README、CI 和 HANDOFF。
- 运行验收需要 Docker Engine 与 Docker Compose v2；日常 `mvnw clean verify` 仍不需要 Docker 或外部依赖。
- C02 不影响现有 Java 模块、API、服务边界或业务数据模型，也不执行数据库 Migration。
- 本地启动会创建 MySQL、Redis、RabbitMQ 和 Nacos 命名卷并占用对应端口；停止服务不得自动删除卷，清卷仍需人工二次确认。
- 主要风险包括镜像架构/标签不可用、Nacos 对数据库或启动参数的隐含依赖、低内存环境资源竞争、端口冲突和错误绑定公网；设计与任务将分别给出验证、诊断和回滚路径。

## Acceptance Overview

- `docker compose config` 可在不暴露真实 secret 的前提下解析 `base` profile，且所有镜像均为精确非 `latest` 标签。
- 在受支持的 Docker 环境启动后，四个基础组件在限定时间内达到 healthy，并通过协议级只读冒烟检查。
- 停止并重新启动 Compose 后命名卷仍保留；正常回滚仅停止容器和恢复配置，不自动删除数据卷。
- OpenSpec 严格校验、现有 `mvnw.cmd clean verify`、基础设施静态检查及适用环境下的 Compose 冒烟均通过。
