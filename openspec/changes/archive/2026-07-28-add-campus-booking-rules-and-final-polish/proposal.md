## Why

VenueFlow 的核心业务闭环已经完整，但资源仍缺少学校管理中最常用的申请须知和时间约束，管理端也会向不同角色展示超出职责的入口。补齐这些规则与角色化体验后，当前约定范围内的应用构建即可收口。

## What Changes

- 资源增加申请须知、最少提前小时数、最多提前天数和单次最长使用分钟数。
- 系统管理员和资源管理员可在资源卡片中维护规则，更新采用现有乐观版本控制。
- Resource Slot 协作响应携带当前规则；Booking 在占用容量前验证提交时间和时段长度。
- 申请人资源详情展示清晰的预约规则与须知，避免提交后才发现不符合要求。
- 管理工作台按 `APPROVER`、`RESOURCE_MANAGER`、`SYSTEM_ADMIN` 隐藏无权功能入口，并补齐提交中状态与一致错误提示。
- 扩展持久化 smoke、运行手册和最终验收说明。
- 不新增黑名单表或通用日历引擎；临时停用继续复用资源暂停和时段关闭。

## Capabilities

### New Capabilities

- `campus-booking-rules`: 资源级申请须知、提前预约范围、最长使用时间及预约提交校验。

### Modified Capabilities

- `resource-catalog`: 资源目录返回并允许管理人员维护预约规则。
- `resource-slot-management`: 单时段协作详情携带所属资源的预约规则。
- `booking-reservation-management`: Booking 在容量占用前验证资源快照中的时间规则。
- `web-application`: 申请人展示规则，管理端按角色收敛导航和操作状态。

## Impact

- Resource Service：一项加法 Flyway 迁移、规则更新 API、DTO 和时段协作字段。
- Booking Service：协作合同扩展和创建前规则校验，不增加 Booking 数据迁移。
- Web：资源规则编辑、申请须知展示、角色导航和表单提交状态。
- Smoke、README、管理运行手册、OpenSpec 主规范和交接文档。
