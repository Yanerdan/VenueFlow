# VenueFlow 简历文案

使用时请保留“个人项目”或“全栈工程作品”标签，不要把合成数据描述成真实生产流量。

## 中文精简版

**VenueFlow 校园资源预约与审批治理平台｜个人全栈项目**

- 基于 Java 21、Spring Boot 4 与 Spring Cloud 设计 7 进程微服务系统，覆盖资源发布、排期、预约、1–5 级审批、通知、核销和运营报表，并完成申请人/管理端双工作台。
- 针对并发预约设计幂等命令、容量台账、超时释放与对账补偿；通过事务 Outbox、RabbitMQ 和消费去重解耦通知与 Elasticsearch 搜索投影。
- 实现 OIDC Authorization Code + PKCE、组织架构全量/增量同步和角色/组织授权；使用 Flyway、Docker Compose、GitHub Actions、JaCoCo、Spotless、SpotBugs 与 CycloneDX 建立工程交付链路。
- 建立 39 份可追踪主规格、28 个数据库迁移和自动化冒烟验收；当前本地报告覆盖 277 项 Java 测试与 19 项前端测试。

## 中文一行版

独立完成 VenueFlow 校园资源预约治理平台，以 7 个 Java/Spring 微服务实现多级审批、容量一致性、可靠事件、OIDC/组织同步和双端运营工作台，并配套迁移、CI、可观测性与自动化验收。

## English version

**VenueFlow — Campus Resource Booking & Approval Governance Platform | Full-stack Personal Project**

- Designed a seven-process Java 21 / Spring Boot 4 system covering resource publishing, scheduling, booking, one-to-five-stage approvals, notifications, check-in, and operational analytics, with separate applicant and administration workspaces.
- Protected booking capacity with idempotent commands, a capacity ledger, timeout release, and reconciliation; decoupled notifications and Elasticsearch projections through transactional Outbox, RabbitMQ, and idempotent consumers.
- Implemented OIDC Authorization Code with PKCE, full/incremental organization synchronization, and role/organization-scoped authorization; established delivery controls with Flyway, Docker Compose, GitHub Actions, JaCoCo, Spotless, SpotBugs, and CycloneDX.
- Maintained 39 traceable specifications and 28 database migrations; the current local reports cover 277 Java tests and 19 frontend tests.

## 技能关键词

`Java 21` `Spring Boot` `Spring Cloud` `Spring Security` `MyBatis-Plus` `MySQL` `Redis` `RabbitMQ` `Elasticsearch` `OIDC` `JWT` `Flyway` `Docker Compose` `OpenTelemetry` `Prometheus` `Grafana` `GitHub Actions` `Outbox` `Idempotency` `Saga Compensation` `RBAC`

## 面试表达原则

- 说“我设计并实现”，不要虚构团队人数或管理职责。
- 说“合成演示场景”，不要说“服务了某校多少师生”。
- 测试数字只在重新执行测试或检查报告后使用。
- 优先讲一个完整权衡：为什么需要、有哪些备选、为何这样选、失败如何恢复。
