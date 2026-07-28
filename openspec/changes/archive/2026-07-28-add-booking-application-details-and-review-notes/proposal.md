## Why

VenueFlow 已能完成申请和审批状态流转，但申请单只有用户、时段和人数，管理人员缺少判断所需的活动信息，申请人也看不到明确的审批说明。补齐业务申请详情与审批意见，是从流程演示迈向可实际理解的校园管理产品的最短路径。

## What Changes

- 预约申请增加活动名称、申请用途、联系人、联系电话和备注。
- 审批通过、驳回和取消动作可携带有界处理意见；驳回必须填写原因。
- Booking 保存当前审批结论与处理时间，并提供有界申请详情 DTO。
- 申请人端增加完整申请表单和详情展示。
- 管理端增加申请详情面板、审批意见和驳回原因输入。
- 保持现有容量、幂等、状态机、Outbox 和通知链路不变。

## Capabilities

### New Capabilities

- `booking-application-details`: 定义申请业务字段、审批意见、驳回原因和有界详情展示。

### Modified Capabilities

- `booking-reservation-management`: 创建与状态动作 DTO 扩展为携带申请详情和处理意见。
- `web-application`: 申请人和管理人员工作区增加申请详情与意见交互。

## Impact

主要影响 Booking Service 的 V006 迁移、预约领域模型、创建哈希、状态动作和 DTO，以及 `venueflow-web` 两个工作区和本地验收脚本。无需新增服务或第三方依赖，Resource、User、Notification 的所有权边界保持不变。
