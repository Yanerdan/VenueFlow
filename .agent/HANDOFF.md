# VenueFlow Handoff

## 当前状态

- 分支：`main`
- Change：C13 `add-reliable-notification-consumer` 已同步并归档。
- C11 归档：`openspec/changes/archive/2026-07-23-add-booking-outbox-and-reliable-publication`
- C12 归档：`openspec/changes/archive/2026-07-23-add-notification-service-skeleton`
- C13 归档：`openspec/changes/archive/2026-07-23-add-reliable-notification-consumer`

## 已完成

- C01-C09 已归档。
- Resource 增加按 `slotId + operationId` 查询容量操作结果的 DTO API。
- Booking 增加 V001、作用域幂等抢占、`CONFIRMED -> CANCELLED`、有界 HTTP adapters、安全 envelope。
- Resource `mysql-it` 10/10 通过。
- Booking `mysql-it` 2/2 通过，包含真实 MySQL 并发仲裁。
- 根目录 Docker-free `clean verify` 全 7 个模块通过。
- OpenSpec strict、diff、迁移不可变性、secret/路径和服务边界扫描通过。
- C10 delta specs 已同步到主 specs，归档后严格校验 11/11 通过。
- README、环境变量示例和预约 Runbook 已更新。
- C11 Booking Outbox 已同步并归档。
- C12 已加入独立可执行的 Notification Service 骨架、端口 8085、受限健康探针和
  Docker-free 验证。

## 已知限制

- 默认 `clean verify` 保持 Docker-free；真实 MySQL 仅由 `mysql-it` 显式执行。
- 容量写不自动重试；超时使用 operationId 查询。
- 补偿失败仅提供人工 Runbook；Outbox 已实现，自动对账尚未实现。
- 不修改已归档 Migration，不跨库，不提交 secret，不自动提交或合并。

## 下一步

从已同步主规范和完整工程路线选择 C14，保持一次 Change 只建立一个可独立验证的能力。

## C11 实现状态

- 已归档 Change：`add-booking-outbox-and-reliable-publication`。
- 已实现 Booking V002、确认/取消事务事件、租约扫描、RabbitMQ Confirm/Return、有界
  retry/dead、安全元数据与管理命令、指标和 opt-in 集成测试。
- 默认启动保持无外部连接；RabbitMQ 自动配置被排除，仅由 `messaging` profile 创建。
- 已有证据：根目录 Docker-free `clean verify` 报告无失败；Booking 默认验证、MySQL 2/2、
  Outbox MySQL/RabbitMQ 4/4、Enforcer、SpotBugs、SBOM、OpenSpec strict 与 diff/scope
  扫描均通过。
- C11 实现任务 20/20 完成，delta specs 已同步到主规格。

### C11 最终验证命令与范围

```powershell
mvn clean verify
mvn -pl venueflow-booking-service verify -Pmysql-it
mvn -pl venueflow-booking-service verify -Poutbox-it
openspec validate add-booking-outbox-and-reliable-publication --strict
git diff --check
```

范围仅包含 Booking V002、Booking Outbox 领域/持久化/发布/管理代码、对应测试和文档；
未修改 V001、Resource/User migration，也未加入消费者、超时任务、Resource Outbox 或对账代码。

## C12 实现状态

- `venueflow-notification-service` 已加入根 Maven reactor，默认 `skeleton` profile 监听
  `8085`，可由 `SERVER_PORT` 覆盖。
- 默认启动只暴露 liveness/readiness 探针，不创建数据库、RabbitMQ、协作者或出站通知连接。
- 模块依赖限制为 Web MVC、Actuator 和测试支持，并由 Enforcer 与架构测试守护边界。
- C12 不包含 Migration、AMQP listener、队列/绑定、`ConsumedEvent`、重试/DLQ、
  通知记录或 Compose 应用容器。
- Notification 模块验证通过：JUnit 7/7、Failsafe 9/9；根 reactor 8/8 模块通过。
- Enforcer、Spotless、SpotBugs、CycloneDX SBOM、OpenSpec strict、diff、个人绝对路径
  和生产代码服务边界扫描均通过。

### C12 最终验证命令与范围

```powershell
.\mvnw.cmd -pl venueflow-notification-service -am clean verify
.\mvnw.cmd clean verify
.\mvnw.cmd -pl venueflow-notification-service dependency:tree
openspec.cmd validate add-notification-service-skeleton --strict
git diff HEAD --check
rg -n '@RabbitListener|@Entity|CREATE\s+TABLE|ConsumedEvent|RabbitAdmin|new\s+Queue\s*\(|new\s+Binding\s*\(|JdbcTemplate|JavaMailSender' venueflow-notification-service/src/main
rg -n '[A-Za-z]:\\Users\\|/Users/|/home/' README.md .agent/HANDOFF.md venueflow-notification-service openspec/changes/add-notification-service-skeleton -g '!target/**'
rg -n '[\t ]+$' README.md .agent/HANDOFF.md pom.xml venueflow-notification-service openspec/changes/add-notification-service-skeleton -g '!target/**'
```

范围仅包含根 reactor 注册、Notification Service 骨架/配置/测试/模块文档，以及根
README、HANDOFF 和 C12 OpenSpec 制品；未修改 C11 Booking Outbox、任何数据库
Migration、部署 Compose 或其他服务业务代码。

## C13 实现状态

- Notification V001 新增消费身份 inbox、站内通知记录和有界失败审计表，全部属于
  Notification 自有 schema。
- 仅接受 C11 的 `booking.reservation.confirmed.v1` 与
  `booking.reservation.cancelled.v1`，严格校验 envelope、大小、类型和版本。
- inbox 与通知在同一事务提交后手动 ACK；精确重复安全 ACK，身份碰撞进入终态失败。
- 固定延迟重试和 DLQ 转移使用持久消息、mandatory routing 和 Publisher
  Confirm/Return；仅在确认路由后 ACK 来源消息，未知结果 NACK/requeue。
- 非 HTTP 管理命令支持安全 DLQ 预览与带身份、指纹、原因和显式确认的受控重放。
- 默认验证仍为 Docker-free；`consumer-it` 使用 Testcontainers 的 MySQL 8.4.10 和
  RabbitMQ 4.1.8，当前 13/13 Failsafe 检查通过。
- 已知至少一次窗口：本地事务提交后、broker ACK 前崩溃会导致重新投递；inbox 去重
  保证不会再写一条通知。转移确认后、来源 ACK 前崩溃可能产生重复重试/DLQ 副本，
  仍由相同身份边界处理。

### C13 验证命令与范围

```powershell
.\mvnw.cmd -pl venueflow-notification-service -am clean verify
.\mvnw.cmd -pl venueflow-notification-service verify -Pconsumer-it
.\mvnw.cmd clean verify
.\mvnw.cmd -pl venueflow-notification-service dependency:tree
openspec.cmd validate add-reliable-notification-consumer --strict
git diff HEAD --check
```

范围仅包含 Notification 的依赖/profile、V001、消费/事务/消息转移/管理代码、测试和
文档，以及 C13 OpenSpec 制品；未修改 Booking 代码或既有 migration，未修改生产
Compose，未加入邮件、通知 HTTP API、超时取消或跨服务数据库访问。

## C14 implementation status

- Archived change:
  `openspec/changes/archive/2026-07-26-add-booking-capacity-reconciliation`.
- Booking V003 adds recovery intents, runs, deduplicated issues, and immutable repair actions.
- Allocation and cancellation paths persist recovery intent before Resource writes and resolve it
  atomically with Booking/Outbox completion when the outcome is proven.
- `persistence,reconciliation` provides bounded leased reconciliation, opt-in scheduling, and
  guarded non-HTTP preview/run controls. Default startup remains infrastructure-free.
- Unit tests and `reconciliation-it` cover MySQL 8.4.10 migration/leases plus HTTP-stub orphan
  release and cancellation recovery. All final gates and strict OpenSpec validation passed.

## C15 implementation status

- Archived change:
  `openspec/changes/archive/2026-07-26-add-booking-timeout-expiration`.
- Booking V004 adds pending confirmation, deadlines, status audit, leased timeout retry, and
  expiration while preserving historical confirmed/cancelled rows.
- Creation is now breaking: it returns `PENDING_CONFIRMATION`; explicit confirmation creates the
  confirmation Outbox event.
- The opt-in `persistence,expiration` worker releases capacity with the deterministic release
  operation before atomically committing `EXPIRED` and its event.
- Notification accepts the exact expiration route and derives an inbox-idempotent in-app
  expiration notification.
