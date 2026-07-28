## Why

VenueFlow 已能按资源指定审批人，但管理人员仍需手工填写不可读的 UUID，且除启动时管理员外没有界面可以分配校园角色。这使人员上岗和资源责任配置无法由学校自行完成，是当前最明显的产品化缺口。

## What Changes

- 新增仅 `SYSTEM_ADMIN` 可访问的认证账号目录与角色变更 API。
- 角色变更递增令牌版本，使旧刷新会话失效，并禁止管理员撤销自己的系统管理员角色。
- 将 Auth 管理接口从公开认证路径中单独收紧为必须携带有效 JWT。
- 人员目录允许资源管理员读取，并在管理端合并用户资料与认证账号角色。
- 管理端人员目录增加角色选择和保存操作。
- 资源归属表单改为从可用审批人员目录选择，不再要求手工输入 UUID。
- 更新本地 smoke 与运行手册，覆盖角色分配和审批人目录配置。

## Capabilities

### New Capabilities

- `campus-role-management`: 系统管理员浏览认证账号、分配校园角色并安全刷新授权状态的能力。

### Modified Capabilities

- `secure-api-gateway`: Auth 管理接口必须认证，只有注册、登录、刷新和退出保持公开。
- `campus-user-directory`: 资源管理员可读取人员目录以完成资源责任配置。
- `resource-ownership`: 管理工作台从具备审批角色的人员目录选择资源审批人。

## Impact

- Auth Service：账号管理查询、角色更新、令牌版本递增和管理控制器。
- Gateway：Auth 公共端点白名单收窄。
- User Service：管理目录角色范围扩展至 `RESOURCE_MANAGER`。
- Web：账号与资料合并、角色编辑、审批人下拉选择和状态提示。
- Resource 数据模型和 Booking 快照协议保持不变，不增加数据库迁移。
