## Why

C25 已形成申请人与学校管理双工作区，但人员仍主要以内部数字 ID 展示，缺少学校业务需要的学工号、人员类型、院系和联系方式。补齐校园身份与人员目录，能够让申请、审批和人员管理从技术演示进一步变成可理解的学校业务产品。

## What Changes

- 扩展 User-owned profile，增加学工号、人员类型、院系、电话和邮箱，并提供向后兼容迁移。
- 增加本人校园资料维护接口和前端资料中心。
- 增加有界、角色保护的管理端人员目录查询。
- 管理端增加人员目录，并在预约审批中用真实姓名和院系替代裸用户 ID。
- 本地演示注册流程收集校园身份信息；既有用户可保留并补全资料。

## Capabilities

### New Capabilities

- `campus-user-directory`: 定义校园身份资料、本人维护和管理端人员目录能力。

### Modified Capabilities

- `user-profile-management`: 用户资料从最小姓名扩展为向后兼容的校园身份，并增加有界管理查询。
- `web-application`: 申请人端增加校园资料流程，管理端增加人员目录和具名审批展示。

## Impact

主要影响 User Service 的 V002 迁移、领域模型、DTO、查询接口及 `venueflow-web`。Gateway 已有 `/api/v1/users/**` 路由，无需新增服务或第三方依赖；Booking 仍只持有用户 ID，不跨库复制人员资料。
