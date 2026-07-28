# VenueFlow Handoff

## 当前状态

- 分支：`main`
- C01-C30 均已完成、同步并归档；当前没有活动 OpenSpec change。
- C30：`add-role-management-and-approver-directory`
- 本地 Gateway、Auth、User、Resource、Booking、Notification、Search 全部为 `UP`。

## 当前产品能力

- 申请人：注册登录、校园资料、资源搜索、时段容量、完整申请、撤回、进度和审批意见。
- 管理人员：运营总览与报表、人员目录、角色管理、范围化审批、签到核销、资源归属、审批人选择和开放时段。
- 角色管理：系统管理员可维护单一校园角色，禁止自我降级；角色变化推进令牌版本，目标账号需重新登录。
- 责任分配：资源管理员和系统管理员只从 `APPROVER`/`SYSTEM_ADMIN` 合格账号目录选择审批人，无需填写 UUID。
- 可靠性：幂等、容量台账、Outbox、消费去重、超时释放、补偿、缓存和搜索投影。

## C30 验证

```powershell
.\mvnw.cmd -pl venueflow-auth-service,venueflow-gateway,venueflow-user-service -am test -DskipITs
node --test venueflow-web/test/*.test.js
powershell -ExecutionPolicy Bypass -File scripts/local-dev/smoke.ps1
openspec.cmd validate --all --strict
```

- 后端：74 项测试通过。
- 前端：12 项测试通过，脚本语法检查通过。
- 持久化 smoke：`RoleManagement = PASS`，其余注册、资料、预约、审批、报表、搜索、通知和令牌链路全部通过。
- 浏览器：人员角色下拉、资源审批人目录正确，无控制台错误。
- OpenSpec：34 个主规范严格校验通过。

## 下一阶段建议

C31 建设可配置审批策略：资源可选择“直接审批”或精简的多级审批模板，Booking 保存审批链快照，管理端展示当前节点和审批轨迹。继续采用大步长与从简原则；正式 SSO、组织同步、安全加固、压测、真实试用和发布材料仍暂缓。
