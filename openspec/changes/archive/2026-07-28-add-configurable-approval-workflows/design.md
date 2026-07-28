## Context

现有 Resource 保存单一 `approverExternalUserId`，Booking 在创建时快照该值，并在一次 confirmation 后直接进入 `CONFIRMED`。现有边界已经适合在不引入工作流引擎的前提下扩展一个固定两级模式。

## Goals / Non-Goals

**Goals:**

- 允许资源选择直接审批或固定两级审批。
- 在 Booking 内保存不可变责任快照并记录每次审批动作。
- 让队列授权跟随当前节点，申请人能看懂进度。
- 保持旧资源和旧预约完全兼容。

**Non-Goals:**

- 不实现任意数量节点、条件表达式、会签、转办或加签。
- 不引入 BPMN/工作流引擎。
- 不改变容量、超时、通知和最终预约状态模型。

## Decisions

1. Resource 增加 `approval_mode`（默认 `DIRECT`）和 `final_approver_external_user_id`。`DIRECT` 忽略终审人；`TWO_STAGE` 必须同时有两名不同审批人。
2. Booking 快照 `approval_mode`、`final_approver_external_user_id`、`current_approval_step`。旧预约按 `DIRECT`、第一级处理。
3. 新建 `booking_approval_actions` 表记录步骤、审批人、角色、结论、意见和时间。该表是展示轨迹，不替代预约主状态。
4. 初审通过只写轨迹并把步骤从 1 推进到 2；终审或直接审批才把状态改为 `CONFIRMED`。任一步驳回沿用 `CANCELLED` 并写轨迹。
5. `APPROVER` 只能处理当前步骤分配给自己的申请；`SYSTEM_ADMIN` 保留兜底全局权限。
6. API 继续复用 confirmation/cancellation，响应 DTO 添加审批阶段和轨迹，减少前后端合同扩张。

## Risks / Trade-offs

- [固定两级不能覆盖复杂学校流程] → 本阶段优先得到成熟可演示产品，后续再演进模板。
- [系统管理员可越过指定人兜底处理] → 保留当前运维语义，同时轨迹记录实际处理角色。
- [旧申请没有轨迹] → 显示为历史直接审批，不回填虚构数据。
