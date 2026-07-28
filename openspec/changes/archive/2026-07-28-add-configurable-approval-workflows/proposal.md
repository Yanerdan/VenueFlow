## Why

VenueFlow 已能为资源指定一名审批人，但学校常见的重点场地需要“责任人初审、系统管理员终审”。当前单步确认无法表达这一规则，也没有可读审批轨迹，是演示成熟度的下一个主要缺口。

## What Changes

- 资源支持 `DIRECT` 与 `TWO_STAGE` 两种精简审批策略。
- 两级策略从合格人员目录选择初审人和终审人，并禁止两级使用同一账号。
- Booking 创建时快照审批策略与两名审批人，后续资源配置变化不影响在途申请。
- 审批动作写入独立轨迹；初审通过后申请仍为待审批并推进到第二级，终审通过后才确认。
- 管理队列只向当前节点审批人和系统管理员展示；驳回在任一节点立即结束申请。
- 用户端和管理端展示当前审批阶段与完整审批轨迹。
- smoke 覆盖两级推进与最终确认。

## Capabilities

### New Capabilities

- `configurable-approval-workflow`: 资源级直接/两级审批配置、预约审批链快照、逐级处理与轨迹展示。

### Modified Capabilities

- `resource-ownership`: 资源责任配置增加审批策略与可选终审人。
- `scoped-booking-approval`: 管理队列和审批动作按当前节点授权。
- `booking-application-details`: 申请详情返回当前审批阶段与审批轨迹。

## Impact

- Resource Service：V007 增量迁移、责任 DTO/更新接口与管理端表单。
- Booking Service：V009 增量迁移、审批链快照、动作轨迹和逐级确认。
- Web：资源审批策略配置、申请阶段和轨迹展示。
- 不新增服务，不引入通用工作流引擎或任意节点编排。
