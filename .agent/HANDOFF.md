# VenueFlow Handoff

## 当前状态

- 分支：`main`
- C01-C29 均已完成、同步并归档，当前没有活动 OpenSpec change。
- C29：`add-operational-audit-and-reports`
- 本地 Gateway、Auth、User、Resource、Booking、Notification、Search 全部为 `UP`。

## 当前产品能力

- 申请人：注册登录、校园资料、资源搜索、时段剩余容量、完整申请、撤回、进度和审批意见。
- 管理人员：运营总览、运营报表、人员目录、申请详情、范围化审批、签到核销、资源归属和开放时段。
- 运营报表：申请总量、待审批量、通过率、参与人数、资源排行、部门分布和最近审批记录。
- 可靠性：幂等、容量台账、Outbox、消费去重、超时释放、补偿、缓存和搜索投影。

## C29 验证

```powershell
.\mvnw.cmd -pl venueflow-booking-service -am test -DskipITs
node --test venueflow-web/test/*.test.js
powershell -ExecutionPolicy Bypass -File scripts/local-dev/smoke.ps1
openspec.cmd validate --all --strict
```

- Booking：57 项测试通过。
- Frontend：11 项测试通过。
- 持久化 smoke：`OperationalReport = PASS`。
- 浏览器：系统管理员报表指标、排行、部门和审批审计均正确，控制台无错误。
- OpenSpec：33 个主规范严格校验通过。

## 下一阶段建议

C30 优先解决学校管理可用性：管理员维护人员角色，并在资源配置中从人员目录选择审批人，替代手工填写 UUID。随后再考虑可配置多级审批。正式 SSO、安全加固、压测、真实试用和发布材料继续暂缓。
