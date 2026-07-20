## Context

C01 已提供 JDK 21/Maven 工程、版本清单和不依赖外部服务的 `clean verify`。当前仓库没有 `deploy/`、Compose 文件或容器验收入口，`.version/stack-versions.yml` 只记录 MySQL 8.4、Redis 7.4、RabbitMQ 4.1 和 Nacos 3.1.1 的架构基线，尚未形成实际镜像版本事实。

C02 是 v0.1.0 的第二个独立 Change，服务于本机 Docker 或 VMware Ubuntu infra-node。主要约束是 16GB 宿主机只能常驻约 4~5GB 的基础 profile、中间件不得暴露互联网、不得使用 `latest`、凭据不得提交、卷不得自动删除，并且 C03 之前不能创建业务数据库 Migration、客户端或 Resource Service。

## Goals / Non-Goals

**Goals:**

- 以一个可重复命令启动 MySQL、Redis、RabbitMQ 和 Nacos 的最小 `base` profile。
- 建立镜像精确标签、配置来源、端口绑定、命名卷、资源预算和健康检查的单一事实来源。
- 同时支持本机回环地址和 VMware Host-only 地址，不把 `192.168.72.0/24` 写死为通用事实。
- 提供有超时、有诊断输出、无破坏动作的静态校验与运行时冒烟检查。
- 使 CI 可验证 Compose 配置，并在具备 Docker 的隔离 job 中验证基础组件启动；保持 Maven 默认构建离线基础设施无关。

**Non-Goals:**

- 不创建 Gateway 或任何业务服务，不添加 Java 基础设施依赖。
- 不创建业务数据库、应用用户、Flyway Migration、业务队列/交换机、Nacos Data ID 或示例业务数据。
- 不引入 Elasticsearch、Prometheus、Grafana、OpenTelemetry、Jaeger、Sentinel、Seata 或 Kubernetes。
- 不提供生产集群、高可用、跨地域容灾、在线备份或自动恢复承诺。
- 不在本 Change 生成不可验证的镜像 digest；digest 锁在镜像实际解析并达到项目发布门槛后单独补齐。

## Decisions

### 1. 单一 Compose 工程与显式 `base` profile

使用 `deploy/compose/compose.yml` 管理四个服务，每个服务显式声明 `profiles: [base]`。标准入口通过仓库脚本调用 `docker compose --env-file deploy/versions.env --env-file .env --profile base ...`，并固定 Compose project name，避免不同工作目录产生多套匿名项目。

候选方案包括每个中间件一个 Compose 文件、默认无 profile 的单文件和当前方案。多文件便于单组件操作但容易产生网络、卷和变量漂移；默认全启动会让低内存机器误开全部依赖。单文件加显式 profile 能共享网络/卷约定，并要求操作者明确选择成本。

### 2. 版本基线与运行镜像事实分层

`.version/stack-versions.yml` 继续保存架构允许的主/次版本；`deploy/versions.env` 保存实现时通过 registry manifest、镜像架构和真实启动冒烟验证的精确非 `latest` 标签。Compose 只引用 `deploy/versions.env` 中的镜像变量，并由静态脚本拒绝缺失、浮动或 `latest` 值。

直接在 Compose 中散落标签更直观，但升级时难以审计；只记录主/次版本又无法重现。本设计选择独立版本文件，并要求版本升级进入单独 Change。`deploy/image-digests.lock` 不以空文件或虚假 digest 充数。

### 3. Nacos 使用 standalone 内嵌存储

C02 的 Nacos 采用 standalone 模式并挂载独立命名卷，不复用 VenueFlow MySQL。这样可以验证注册/配置服务本身，同时避免为了 Nacos 引入供应商 schema、初始化 SQL 和数据库耦合。

候选方案是 Nacos 连接同一 MySQL 容器。该方案更接近长期部署，但需要 Nacos 专用数据库/schema、初始化顺序和备份策略，会扩大 C02 数据范围。若后续真实多实例或持久化要求证明内嵌模式不足，应以独立 Change 迁移并验证备份、兼容和回滚。

### 4. 安全默认值与参数化绑定

所有宿主机端口使用 `${INFRA_BIND_ADDRESS}` 显式绑定；示例默认 `127.0.0.1`，在 infra-node 上由本地 `.env` 改为 Host-only 地址。Compose 不创建公网 ingress。MySQL、Redis、RabbitMQ 和 Nacos 启用各自可用的认证配置，secret 只从未跟踪的 `.env` 注入；示例值必须明显不可用于真实环境。

绑定 `0.0.0.0` 最方便但会扩大暴露面；完全不发布端口则宿主机应用和诊断无法访问。显式绑定在可用性与安全性之间提供可审计选择。容器日志、`docker compose config` 输出和 CI 命令不得主动打印实际 secret。

### 5. 持久化和资源预算

使用 `mysql-data`、`redis-data`、`rabbitmq-data` 和 `nacos-data` 命名卷；普通 `down` 不带 `--volumes`。每个服务声明 CPU/内存上限和组件级轻量参数，使 base profile 总预算适合 4~5GB infra-node。MySQL、RabbitMQ 和 Nacos获得主要内存，Redis 设置显式内存上限及非淘汰策略；具体值以首次启动的健康和峰值记录调整。

绑定宿主机目录便于直接查看文件，但跨 Windows/Linux 权限和性能差异较大。命名卷更适合作为可移植默认值；备份/恢复属于后续 Change。任何清卷操作只写入带醒目标记的 Runbook，不进入自动验收脚本。

### 6. 健康状态、启动时序与失败处理

容器状态模型为 `created -> starting -> healthy | unhealthy -> stopped`。四个服务都使用组件原生或官方支持的只读探针；探针设置启动宽限、固定间隔、单次超时和有限重试。base 组件彼此不通过 Compose 启动顺序伪装 readiness，Nacos standalone 也不依赖 VenueFlow MySQL。

标准时序：校验 Docker/Compose 和环境文件 → 渲染并静态检查 Compose → pull/启动 base → 轮询容器健康（全局超时）→ 执行协议级只读检查 → 输出摘要。失败时收集 `ps`、health inspect 和有界日志后返回非零；不自动重启循环、不删容器或卷。restart 策略只处理宿主机/daemon 重启后的有限恢复，不代替 readiness。

### 7. 验收脚本与 CI 分层

提供跨仓库约定明确的 PowerShell 与 POSIX 入口，或一个平台中立实现加薄包装器；两端执行相同检查：工具版本、变量完整性、镜像策略、`docker compose config`、健康等待和只读协议冒烟。所有等待必须有总超时，失败日志限制行数且不泄漏 secret。

CI 分两层：现有 Maven job 继续运行 `clean verify`；独立 infrastructure job 先做静态校验，再启动 base profile、运行 smoke，并在 `always()` 清理容器但不使用持久化 runner 数据。CI 可使用专用一次性环境文件，不能提交或回显真实凭据。

## Service Sequence

```text
operator/CI
  -> preflight: Docker + Compose v2 + env completeness
  -> compose config + version-policy check
  -> compose pull/up --profile base
  -> wait: MySQL, Redis, RabbitMQ, Nacos health
  -> read-only smoke: SQL SELECT 1 / Redis PING /
                      RabbitMQ diagnostics / Nacos liveness
  -> success summary

on failure
  -> bounded ps + health + logs
  -> non-zero exit
  -> CI only: compose down (without --volumes)
```

## Data, Transaction, Consistency and Idempotency Boundaries

C02 只创建四个组件的内部运行数据卷，不创建 VenueFlow 业务 schema、表或事件拓扑，因此没有跨服务事务、业务状态机或业务幂等要求。Compose 重复执行 `up -d` 必须收敛到同一 project、network、service 和命名卷，不重复创建逻辑资源；smoke 全部只读，可安全重复执行。

持久化一致性由各组件自身负责，C02 不宣称容器级单节点具备高可用。`down`/重启后卷保留是本 Change 的生命周期契约；备份、恢复、升级和数据迁移必须由后续 Change 建立可验证流程。

## Failure Matrix

| Failure | Detection | Behavior / Recovery |
|---|---|---|
| Docker/Compose 缺失或版本不支持 | preflight | 启动前失败并给出安装/版本提示 |
| 镜像标签不存在或架构不兼容 | pull/manifest 与启动结果 | 停止验收，修订精确标签；不得回退到 `latest` |
| 环境变量或 secret 缺失 | preflight/config | 渲染前失败，不打印 secret 值 |
| 端口已占用或绑定地址不存在 | Compose start | 输出冲突服务和端口，用户修订本地 `.env` 后重试 |
| 单组件长期 starting/unhealthy | 有界健康轮询 | 输出 health 与有界日志，返回非零；不无限重启 |
| 资源不足/OOM | 容器状态与 daemon 日志 | 关闭无关 profile，按文档调整预算后重试，不静默放宽全部限制 |
| Nacos 内嵌数据损坏 | health/smoke 失败 | 保留卷供诊断；恢复/迁移必须走显式 Runbook 或后续 Change |
| 误操作请求删除卷 | 命令审查/Runbook 警示 | 自动脚本拒绝执行；仅人工确认后单独操作 |

## Timeouts, Retry and Observability

- healthcheck 单次命令和重试次数固定；smoke 使用独立全局超时，不允许无限等待。
- 只重试预期的启动未就绪状态；认证失败、配置错误、镜像错误和端口冲突立即失败。
- 验收输出组件名、容器状态、health 状态、耗时和错误类别；不声称已经具备 Prometheus/Trace。
- 日志诊断按组件限制最近行数，并避免把 Compose 完整渲染结果或环境变量内容上传为公开 artifact。

## Security

- `.env`、凭据和本地网络值保持未跟踪；`.env.example` 只含占位符或明确的开发默认值。
- 端口不得默认绑定 `0.0.0.0`；VMware 部署只绑定 Host-only NIC，并由防火墙再限制来源。
- 不开放危险管理写接口到互联网；RabbitMQ Management 与 Nacos Console 仅供受控网络诊断。
- 镜像来源、精确标签和后续 digest 可审计；不在 C02 中伪造漏洞扫描通过结论。

## Test Strategy

1. 静态测试：环境变量完整性、Compose schema/config、profile、命名卷、绑定地址、资源限制、healthcheck 和非 `latest` 镜像策略。
2. 运行时 smoke：四个容器在限定时间内 healthy，并分别执行 SQL `SELECT 1`、Redis `PING`、RabbitMQ diagnostics/API 探测和 Nacos liveness。
3. 生命周期测试：重复 `up` 不产生第二套资源；`down` 后再次启动仍复用命名卷。测试不写业务数据，也不自动执行 `down --volumes`。
4. 回归：`mvnw.cmd clean verify` 继续在 Docker 未启动时通过；OpenSpec `validate --all --strict` 通过。
5. CI：静态 job 必跑；隔离 Docker job 启动、诊断、smoke 和非破坏清理，失败保留足够日志。

## Migration Plan

1. 先锁定并验证四个精确镜像标签及目标架构，把结果写入 `deploy/versions.env`。
2. 添加 Compose、参数化网络、认证、卷、资源限制和 healthcheck，先通过静态配置检查。
3. 在本机或 infra-node 启动 base profile，逐组件完成健康与协议冒烟，记录实际资源占用。
4. 添加重复启动/保卷验证、Runbook、README 和 CI infrastructure job。
5. 运行 Maven 回归、OpenSpec 严格校验和仓库卫生检查，再更新 HANDOFF。

回滚优先执行不带 `--volumes` 的 `docker compose down`，然后恢复上一版配置；由于 C02 不创建业务 schema 或 Migration，不存在应用数据迁移。若必须删除测试卷，必须由用户明确确认目标 project 和卷名后单独执行，绝不作为自动回滚步骤。

## Open Questions

- 四个镜像的最终精确补丁标签和目标架构支持需在 T01 通过 registry manifest 与真实启动验证后确定，不能仅凭文档猜测。
- Nacos 3.1.1 镜像的最终健康端点、认证变量名和数据目录需以该精确镜像的官方说明与容器内行为验证为准。
- CI runner 若持续无法在合理时间/资源内稳定运行四组件 smoke，应保留强制静态门禁，并将运行时 smoke 移至受控 integration workflow；该调整必须记录实际失败证据，不能静默跳过。
