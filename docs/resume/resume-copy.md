# VenueFlow 简历文案

## 推荐定位

**VenueFlow 校园资源预约与审批治理平台｜个人全栈工程项目｜2026.XX–2026.XX**

面向高校部委、学院和职能部门的资源预约平台，统一管理场地发布、开放排期、师生申请、多级审批、通知和签到核销。个人完成需求建模、服务拆分、后端与双端 Web 开发、数据迁移、基础设施编排及自动化验收。

**技术栈：** Java 21、Spring Boot / Spring Cloud、Spring Security、MyBatis-Plus、MySQL、Redis、RabbitMQ、Elasticsearch、Flyway、Docker Compose、OpenTelemetry

- 围绕身份、人员、资源、预约、通知和搜索等业务边界设计 Gateway + 6 个领域服务，实现资源发布、时段管理、预约申请、1–5 级审批、签到核销和运营报表的完整业务闭环。
- 针对重复提交、并发占用和跨服务调用失败，设计幂等请求、容量台账、超时释放和对账补偿机制；使用事务 Outbox、RabbitMQ 与消费去重可靠驱动通知和搜索投影，避免核心预约事务依赖下游实时可用。
- 实现 OIDC Authorization Code + PKCE、本地 Access/Refresh Token 轮换、组织架构全量/增量同步及角色和组织范围授权；在申请创建时固化审批链快照，确保后续配置变更不影响历史流程的审计语义。
- 使用 Flyway 管理 28 个数据库迁移，以 Docker Compose 和脚本完成环境启停、合成数据播种及全链路冒烟验收；仓库维护 39 份可追踪规格，当前本地报告覆盖 277 项 Java 测试和 19 项前端测试。

## 后端岗位精简版

**VenueFlow 校园资源预约与审批治理平台｜个人项目**

- 基于 Java 21、Spring Boot / Spring Cloud 实现 Gateway 与 6 个领域服务，覆盖资源排期、预约、多级审批、通知、核销和运营统计。
- 通过幂等请求、容量台账、超时释放与对账补偿处理重复提交和跨服务失败；以事务 Outbox、RabbitMQ 和消费去重保障通知及搜索投影最终一致。
- 实现 OIDC + PKCE、JWT/Refresh Token 轮换、组织同步和角色/组织范围授权，并以审批链快照保持历史流程可审计。
- 使用 MySQL、Redis、Elasticsearch、Flyway、Docker Compose 和自动化测试构建可重复启动、播种和验收的本地工程环境。

## 一行简介

独立设计并实现校园资源预约治理平台，以 7 个可独立运行的 Java/Spring 进程完成多级审批、容量一致性、可靠事件、OIDC/组织同步和申请人/管理端完整业务闭环。

## 面试开场版

这是一个个人全栈工程项目，业务背景是学校场地由不同部门管理，申请规则和审批责任不一致。我没有把它只做成预约表单，而是重点解决三个问题：并发或重试时容量不能超卖；通知和搜索故障不能阻塞预约；审批配置变化不能改写历史流程。对应实现了容量台账与补偿、事务 Outbox 与幂等消费、审批链快照，同时补齐了校园统一身份、组织同步、双工作台和自动化验收。

## 英文版

**VenueFlow — Campus Resource Booking and Approval Platform | Full-stack Personal Project**

- Designed and implemented a Java 21 / Spring Boot system consisting of an API Gateway and six domain services for resource scheduling, booking, one-to-five-stage approval, notification, check-in, and operational reporting.
- Protected booking capacity against retries and partial failures with idempotent commands, a capacity ledger, timeout release, and reconciliation; propagated notifications and search projections through transactional Outbox, RabbitMQ, and idempotent consumers.
- Implemented OIDC Authorization Code with PKCE, rotating access/refresh sessions, full and incremental organization synchronization, and role/organization-scoped authorization; snapshotted approval chains to preserve historical audit semantics.
- Built a repeatable local delivery workflow with Flyway, Docker Compose, synthetic data seeding, and end-to-end smoke tests; the repository currently contains 39 traceable specifications, 28 migrations, 277 Java tests, and 19 frontend tests.

## 使用原则

- 明确写“个人项目”或“个人全栈工程项目”，不要虚构团队规模、上线学校或真实用户量。
- 测试数量属于仓库工程规模，不等同于生产稳定性或测试覆盖率。
- 不写“高并发”“企业级”“百万级数据”等无法用压测或生产记录证明的表述。
- 面试时优先深入讲容量一致性、Outbox 或审批快照中的一个，不要逐项背诵技术栈。
- 若简历空间有限，保留前三条核心经历；测试和规格数字可移到项目链接或面试材料。
