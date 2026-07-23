# VenueFlow Handoff

## 当前状态

- 分支：`main`
- Change：C11 `add-booking-outbox-and-reliable-publication` 已同步并归档。
- 归档：`openspec/changes/archive/2026-07-23-add-booking-outbox-and-reliable-publication`

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

## 已知限制

- 默认 `clean verify` 保持 Docker-free；真实 MySQL 仅由 `mysql-it` 显式执行。
- 容量写不自动重试；超时使用 operationId 查询。
- 补偿失败仅提供人工 Runbook；Outbox 已实现，自动对账尚未实现。
- 不修改已归档 Migration，不跨库，不提交 secret，不自动提交或合并。

## 下一步

从已归档 C11 基线提出下一项 Change。

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
