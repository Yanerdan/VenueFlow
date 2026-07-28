## Context

Auth 已在认证凭据中保存 `APPLICANT`、`APPROVER`、`RESOURCE_MANAGER` 和 `SYSTEM_ADMIN`，但角色只能由本地启动引导程序设置。User 人员目录已经返回 Auth UUID，Resource 也保存该 UUID 作为审批人，因此缺失的是受控的账号管理接口和管理端合并视图。

## Goals / Non-Goals

**Goals:**

- 让系统管理员在浏览器中浏览账号并分配校园角色。
- 让资源管理员和系统管理员从可读人员目录选择审批人。
- 保持 Auth、User、Resource 各自数据所有权，不跨库查询。
- 角色变化使旧刷新令牌失效，并在用户重新登录后生效。

**Non-Goals:**

- 不引入组织服务、LDAP/SSO 或自动院系同步。
- 不实现一个账号同时拥有多个角色。
- 不实现多级审批流或按组织规则自动路由。
- 不让 Resource Service 同步校验远端账号角色。

## Decisions

1. Auth 新增 `/api/v1/auth/management/accounts` 查询和 `/{userId}/role` 更新。管理控制器同时检查可信 `X-Role=SYSTEM_ADMIN`，Gateway 也要求该路径认证，形成入口与服务双重约束。
2. 角色更新以 Auth UUID 为稳定标识，并递增 `token_version`。现有访问令牌持续到短期过期，刷新令牌立即无法继续轮换；界面明确提示目标用户重新登录。
3. 禁止系统管理员把自己的角色改为非 `SYSTEM_ADMIN`，防止本地系统失去管理入口。
4. 前端分别读取 Auth 账号目录与 User 人员目录，以 `account.userId === profile.externalUserId` 合并；服务之间不新增同步调用。完整账号目录仅限系统管理员，资源管理员改用只返回 `APPROVER`/`SYSTEM_ADMIN` 安全字段的合格审批人目录。
5. 审批人下拉仅包含 `APPROVER` 和 `SYSTEM_ADMIN`，选项显示姓名、部门和用户名，提交时仍使用现有 UUID 合同。

## Risks / Trade-offs

- [访问令牌在角色变更后短暂保留旧角色] → 访问令牌本就短期有效，刷新会因版本变化失败；界面提示重新登录。
- [部分认证账号尚未建立用户资料] → 账号管理仍显示用户名，审批人选择只展示有资料的合格账号。
- [User 与 Auth 并行请求可能一方失败] → 管理数据加载整体失败并显示可追踪错误，避免展示不一致配置。
