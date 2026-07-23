# VenueFlow Handoff

## 当前状态

- 分支：`feat/add-booking-reservation-and-capacity-coordination`
- Change：C10 `add-booking-reservation-and-capacity-coordination` 已归档。
- 归档：`openspec/changes/archive/2026-07-23-add-booking-reservation-and-capacity-coordination`

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
- 补偿失败仅提供人工 Runbook；Outbox 和自动对账尚未实现。
- 不修改已归档 Migration，不跨库，不提交 secret，不自动提交或合并。

## 下一步

从已归档 C10 基线提出下一项 Change。
