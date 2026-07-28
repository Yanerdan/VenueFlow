# VenueFlow Handoff

## 当前状态

- 分支：`main`
- C01-C31 均已完成、同步并归档；当前没有活动 OpenSpec change。
- C31：`add-configurable-approval-workflows`
- 本地 Gateway、Auth、User、Resource、Booking、Notification、Search 全部为 `UP`。

## 当前产品能力

- 申请人：注册登录、校园资料、资源搜索、时段容量、完整申请、撤回、审批进度和有序审批轨迹。
- 管理人员：运营总览与报表、人员与角色、直接/两级审批、签到核销、资源归属和开放时段。
- 审批策略：资源支持 `DIRECT` 与固定 `TWO_STAGE`；两级审批人必须不同，预约提交时保存不可变审批链快照。
- 审批执行：初审通过后推进到终审但仍保持待审批；终审通过后确认，任一级驳回即结束并释放容量。
- 授权：普通审批人只看到当前节点分配给自己的申请，系统管理员保留全局兜底视图。
- 可靠性：幂等、容量台账、Outbox、消费去重、超时释放、补偿、缓存和搜索投影。

## C31 验证

```powershell
.\mvnw.cmd -pl venueflow-resource-service -am test -DskipITs
.\mvnw.cmd -pl venueflow-booking-service -am test -DskipITs
node --test venueflow-web/test/*.test.js
powershell -ExecutionPolicy Bypass -File scripts/local-dev/smoke.ps1
openspec.cmd validate --all --strict
```

- Resource：76 项测试通过。
- Booking：59 项测试通过。
- Frontend：13 项测试通过，脚本语法检查通过。
- 持久化 smoke：`TwoStageApproval = PASS`，注册、角色、预约、初审、终审、轨迹、报表、搜索、通知和令牌链路全部通过。
- 浏览器：审批模式、初审人、终审人控件正确，控制台无错误。
- OpenSpec：35 个主规范严格校验通过。

## 下一阶段建议

C32 建设校园运营规则：资源级申请须知、最早/最晚提前预约天数、单次最长使用时长和黑名单式停用时段，让系统更像学校可落地的管理产品。继续采用大步长与从简原则；正式 SSO、组织同步、安全加固、压测、真实试用和发布材料仍暂缓。
