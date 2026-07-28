## Why

VenueFlow 已具备稳定的预约技术链路，但当前产品只呈现为面向单个用户的接口演示，缺少学校管理部门能够直接使用和展示的身份、审批、资源运营与数据总览。现在需要用一个大步长补齐校园管理端基础，使系统首先成为可演示、可操作的完整校园资源预约产品。

## What Changes

- 为认证身份增加校园角色，并由 Gateway 向下游传递可信角色上下文。
- 新增校园管理工作台，集中展示资源、时段、预约与待办概况。
- 新增全局预约查询和审批操作入口，复用现有确认、取消、签到生命周期。
- 新增资源与开放时段管理界面，复用现有 Resource Service 写接口。
- 将用户端预约流程调整为“提交申请、等待审批”，完善校园场景文案、状态和导航。
- 增加本地管理员引导、前后端验证与使用说明；暂不包含正式校园 SSO、复杂多级审批、压测和发布材料。

## Capabilities

### New Capabilities

- `campus-administration`: 定义校园角色、管理工作台、全局预约审批和资源运营管理的最小可用能力。

### Modified Capabilities

- `auth-credential-token-lifecycle`: 认证令牌增加稳定的校园角色声明。
- `secure-api-gateway`: Gateway 清理外部身份头并向下游传递由 JWT 提取的可信角色。
- `booking-reservation-management`: 增加有界的管理端全局预约查询，并将确认与签到作为管理操作。
- `web-application`: 浏览器应用扩展为用户端与管理端双工作区，用户预约改为申请审批语义。

## Impact

主要影响 Auth、Gateway、Booking 与 `venueflow-web`；Resource 继续复用现有目录和时段管理 API。Auth 数据库增加向后兼容迁移，Booking 增加只读管理查询，不引入新微服务或第三方前端依赖。
