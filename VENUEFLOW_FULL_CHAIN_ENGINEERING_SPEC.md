# VenueFlow 微服务项目全链路工程约束与 Codex 执行规范

> 文档用途：作为 Codex、开发者及后续 AI 会话共同遵守的项目工程宪法。  
> 项目名称：VenueFlow 园区共享资源预约与履约平台  
> 文档版本：1.1.0  
> 基线日期：2026-07-20  
> 适用环境：本机开发 + VMware Ubuntu 虚拟机 + Docker Compose  
> 核心目标：形成真实业务闭环，并用自动化测试、并发压测、故障演练、监控、对账和可回滚发布证明系统质量。

---

## 0. 执行总则

### 0.1 约束优先级

Codex 执行任务时按以下顺序理解约束：

1. 用户当前明确指令；
2. 本文档；
3. `openspec/specs/` 中已归档规格；
4. 当前 `openspec/changes/<change-id>/` 的 proposal、spec、design、tasks；
5. `docs/adr/` 中已接受的架构决策；
6. 现有测试所表达的行为；
7. 现有实现；
8. `.agent/HANDOFF.md` 临时上下文；
9. Codex 自行推断。

冲突不得静默处理，必须在当前 Change 的 `design.md` 中记录冲突、选择及影响。

### 0.2 专业质量目标

不承诺“绝对无缺陷”。交付标准是：

- 无已知 P0/P1 缺陷；
- 核心链路具有自动化测试；
- 幂等、并发和一致性可重复验证；
- 关键依赖故障有降级、重试、补偿或人工 Runbook；
- 数据具有对账和修复入口；
- 发布与回滚可执行；
- 简历中的数字均来自原始测试证据。

禁止使用“绝对可靠”“消息永不丢失”“完全无缺陷”等不可证明表述。

### 0.3 技术引入门槛

任何新中间件或复杂机制引入前必须回答：

1. 业务问题是什么；
2. 当前方案有什么可观察缺陷；
3. 候选方案有哪些；
4. 为什么选择当前方案；
5. 新增了哪些失败模式；
6. 如何监控；
7. 如何恢复；
8. 如何验证；
9. 什么情况下应移除或替换。

无法回答时不得引入。

### 0.4 Codex 修改规则

Codex 必须：

- 一次只处理一个 OPSX Change；
- 一次只完成一个可验证任务或高度内聚的小任务组；
- 修改前检查 Git 状态、当前分支和最近提交；
- 修改后运行最小必要测试；
- 不顺手重构无关代码；
- 不覆盖用户未提交修改；
- 不删除未知文件；
- 不修改已发布 Flyway Migration；
- 不使用 `latest` 镜像标签；
- 不提交密码、Token、私钥或本机绝对路径；
- 不在测试失败时继续堆叠功能；
- 不自动提交、合并、清库或删除数据卷，除非用户明确要求。

---

# 1. 业务定义

## 1.1 项目定位

VenueFlow 面向高校、企业园区、实验室、共享办公或社区，管理会议室、实验室、球场、摄影棚、摄影器材、实验设备、公共车辆等具有容量、时段和使用规则的共享资源。

## 1.2 角色

| 角色 | 能力 |
|---|---|
| 普通用户 | 注册、登录、搜索资源、查看时段、预约、取消、查看记录、核销、评价 |
| 资源管理员 | 创建资源、维护规则、发布时段、停用资源、查看预约 |
| 运营管理员 | 查看指标、处理异常订单、重放消息、执行对账、人工补偿 |
| 系统任务 | 超时取消、Outbox 投递、消息重试、索引重建、数据对账 |

## 1.3 核心闭环

```text
管理员创建资源
  -> 配置预约规则
  -> 发布可预约时段
  -> 用户搜索资源
  -> 查看资源与时段
  -> 提交预约
  -> 校验用户与容量
  -> 占用名额
  -> 创建预约订单
  -> 模拟确认（非真实第三方支付）
  -> 到场核销
  -> 完成、取消或超时
  -> 通知
  -> 搜索和运营数据更新
  -> 对账与异常处理
```

## 1.4 v1.0 非目标

v1.0 前不做：真实第三方支付、真实短信收费、Kubernetes、服务网格、分库分表、Redis Cluster、RabbitMQ/Nacos 生产集群、多地域容灾、复杂财务结算、推荐算法、AI 客服和多租户计费。

---

# 2. 成功与验收标准

## 2.1 业务验收

真实用户必须能完成：注册登录、资源浏览与搜索、查看时段、创建预约、重复提交保护、取消、超时取消、核销、查询历史和管理员异常处理。

## 2.2 技术验收

| 主题 | 最低验证 |
|---|---|
| 防超卖 | 容量 100，并发请求不少于 1000，成功订单不超过 100，剩余容量不为负 |
| 幂等 | 相同 `Idempotency-Key` 并发 10 次，只生成一张订单并只扣一次容量 |
| 消息重复 | 同一 eventId 重放 10 次，业务只生效一次 |
| 消费者宕机 | 停消费者产生积压，恢复后自动消费并最终一致 |
| 搜索同步 | Elasticsearch 停机时 MySQL 仍可写，恢复后通过重试或重建收敛 |
| 服务多实例 | 停止一个 Resource 实例，剩余实例继续提供服务 |
| 熔断 | 下游持续超时后触发保护，线程资源不被无限占用 |
| 可观测性 | 可查看请求量、错误率、P95/P99、JVM、连接池、MQ 堆积和业务指标 |
| 可重复部署 | 空白 VM 按文档能启动基础设施和应用 |
| 可回滚 | 至少完成一次应用版本回滚演练 |
| 数据修复 | 对账发现并修复至少一项人为制造的不一致 |

## 2.3 证据目录

每次压测或故障实验必须保存：

```text
docs/benchmark/<test-id>/
  environment.md
  scenario.md
  raw/
  result.md
  screenshots/
  commit.txt

docs/failure-drills/<date>-<scenario>/
  plan.md
  commands.sh
  before.md
  during.md
  after.md
  result.md
  screenshots/
```

简历数字只能从这些目录引用。

---

# 3. Git、OPSX 与 HANDOFF

## 3.1 职责划分

- Git：代码、配置和文档版本的唯一事实来源；
- OPSX/OpenSpec：需求、设计、任务、验收和归档；
- HANDOFF：中断会话时的临时上下文，不替代 Git/OPSX/ADR。

## 3.2 Git 分支

```text
main
feat/<change-id>
fix/<change-id>
refactor/<change-id>
test/<change-id>
ops/<change-id>
```

规则：`main` 始终可编译；重要 Change 对应独立分支；合并前更新 main 并重新测试；禁止在 main 直接堆叠大功能。

## 3.3 Commit 规范

使用 Conventional Commits：

```text
feat(booking): add request id idempotency
fix(mq): prevent duplicate timeout cancellation
test(booking): add concurrent capacity test
docs(adr): record outbox decision
chore(build): pin plugin versions
```

Commit 必须单一目的，不混合全仓格式化与业务修改，不提交生成物、日志和 secrets。

## 3.4 Tag 路线

```text
v0.1.0 工程骨架与基础设施
v0.2.0 最小业务闭环
v0.3.0 微服务拆分与服务治理
v0.4.0 并发预约、幂等与状态机
v0.5.0 Outbox、RabbitMQ 与补偿
v0.6.0 Redis 缓存与 Elasticsearch
v0.7.0 Sentinel 与可观测性
v0.8.0 自动化测试与故障演练
v0.9.0 真实用户试用与修复
v1.0.0 简历可用发布版
```

## 3.5 OPSX 标准流程

```text
explore -> propose -> 人工检查 -> 创建分支 -> apply -> test -> verify -> merge -> sync -> archive
```

Change ID 使用动词短语：`add-booking-idempotency`、`add-booking-outbox`。

`proposal.md` 必须包含背景、问题、价值、范围、非范围、影响服务、数据影响、风险和验收概述。

`design.md` 必须包含目标、现状、候选方案、方案对比、服务时序、数据模型、状态机、事务边界、一致性、幂等、失败矩阵、重试、超时、安全、观测、测试、迁移和回滚。

`tasks.md` 每项任务必须附验收和测试，例如：

```markdown
- [ ] T01 添加 requestId 唯一约束
  - 验收：重复请求返回同一订单
  - 测试：BookingIdempotencyIT
```

## 3.6 HANDOFF 模板

路径：`.agent/HANDOFF.md`

```markdown
# HANDOFF
## 当前时间
## 当前分支
## 当前 OPSX Change
## 当前目标
## 已完成
## 未完成
## 修改文件
## 已运行测试及结果
## 已知问题
## 失败过的方案及原因
## 下一步准确操作
## 禁止误操作
```

---

# 4. 技术版本冻结

## 4.1 主线版本

| 技术 | 固定版本/策略 |
|---|---|
| JDK | 21 LTS |
| Spring Boot | 4.0.7 |
| Spring Cloud | 2025.1.2 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| MyBatis-Plus | 3.5.17 |
| Elasticsearch Server | 9.2.x；首次 Search Change 锁定具体补丁 |
| Maven | Wrapper 固定 3.9.x 具体补丁 |
| 编码 | UTF-8 |
| Git 行尾 | LF |
| 内部时区 | UTC |
| JSON 时间 | ISO-8601 |

选择 Boot 4.0.7 而非 4.1.0，是为了贴近 Spring Cloud Alibaba 2025.1.0.0 的 Boot 4.0 基线并减少兼容变量。Spring Data 2025.1 对应 Elasticsearch 9.2.x；首次引入 Search Service 前必须解析实际依赖树、运行连接冒烟测试，并在 `deploy/versions.env` 与镜像锁文件中固定具体补丁及 digest。

## 4.2 基础设施

| 技术 | 基线 |
|---|---|
| Ubuntu Server | 24.04 LTS |
| MySQL | 8.4 LTS |
| Redis | 7.4 Community |
| RabbitMQ | 4.1.x management |
| Nacos Server | 3.1.1 |
| Prometheus/Grafana/OTel/Jaeger | 启用时固定具体补丁及镜像 digest |

规则：Compose 禁用 `latest`；第一次可用后在 `deploy/versions.env` 固定精确标签；v1.0 前生成 `deploy/image-digests.lock`；升级必须单独 Change 并包含备份、兼容与回滚验证。

## 4.3 版本文件

```text
.version/java.version
.version/maven.version
.version/stack-versions.yml
deploy/versions.env
deploy/image-digests.lock
```

`stack-versions.yml`：

```yaml
java: "21"
springBoot: "4.0.7"
springCloud: "2025.1.2"
springCloudAlibaba: "2025.1.0.0"
mybatisPlus: "3.5.17"
elasticsearch: "9.2.x" # 首次 Search Change 验证后改为具体补丁
mysql: "8.4"
redis: "7.4"
rabbitmq: "4.1"
nacos: "3.1.1"
```

---

# 5. 本机与 VMware 拓扑

## 5.1 16GB 宿主机

```text
dev-lite：宿主机运行 IDE、前端和最多 1~2 个当前任务所需的 Java 服务；
          infra-node 建议 4 vCPU、4~5GB RAM、60~80GB Disk，仅启动 base profile。
integration：Docker Desktop 与 VMware 容器后端二选一；运行集成测试时关闭无关服务。
demo-drill：按演练场景启动 search 或 observe profile，不默认同时全开。
```

16GB 环境不得把 Docker Desktop、VMware、全部 Java 服务、ES 和完整观测栈同时常驻作为默认前提。ES、Prometheus、Grafana 使用 Compose profile 按需启动；压测报告必须记录实际启用组件和宿主机内存压力。

## 5.2 24GB 以上

`infra-node` 4 vCPU、8~10GB、100GB；宿主机可运行全部 Java 服务。

## 5.3 32GB 以上

```text
host       ${VMWARE_HOST_IP}  IDE/前端/压测
infra-node ${INFRA_NODE_IP}   基础设施
app-node   ${APP_NODE_IP}     Docker 化应用实例
```

## 5.4 VMware 网络

每台 VM：NAT 用于联网，Host-only 用于稳定内部地址。服务注册地址使用 Host-only IP，不使用动态 NAT IP。网段不得硬编码为所有机器通用事实，使用以下环境参数：

```text
VMWARE_HOST_ONLY_CIDR VMWARE_HOST_IP INFRA_NODE_IP APP_NODE_IP
```

当前本机 VMnet1 可采用 `192.168.72.0/24`、宿主机 `192.168.72.1`、infra-node `192.168.72.11`；若主动将 VMnet1 调整为其他网段，必须同步环境文件、静态地址、Firewall 和部署文档。

仅局域网开放 Gateway 和前端。MySQL、Redis、RabbitMQ、Nacos、ES、敏感 Actuator 端点不得暴露互联网。

---

# 6. 仓库结构

```text
venueflow/
├── README.md
├── LICENSE
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/
├── .editorconfig / .gitattributes / .gitignore
├── .env.example
├── VENUEFLOW_FULL_CHAIN_ENGINEERING_SPEC.md
├── venueflow-dependencies/
├── venueflow-common/
│   ├── venueflow-common-core/
│   ├── venueflow-common-web/
│   ├── venueflow-common-security/
│   ├── venueflow-common-observability/
│   └── venueflow-common-test/
├── venueflow-contracts/
│   ├── venueflow-user-contract/
│   ├── venueflow-resource-contract/
│   ├── venueflow-booking-contract/
│   └── venueflow-event-contract/
├── venueflow-gateway/
├── venueflow-auth-service/
├── venueflow-user-service/
├── venueflow-resource-service/
├── venueflow-booking-service/
├── venueflow-notification-service/
├── venueflow-search-service/
├── venueflow-web/
├── deploy/
│   ├── compose/
│   ├── mysql/ redis/ rabbitmq/ nacos/ elasticsearch/
│   ├── prometheus/ grafana/ otel/
│   ├── versions.env
│   └── image-digests.lock
├── openspec/specs/ and openspec/changes/
├── docs/adr/ architecture/ api/ benchmark/ failure-drills/ security/ runbook/ resume/
├── scripts/bootstrap/ init-data/ smoke-test/ integration-test/ pressure-test/ failure-test/ backup/ restore/
├── .agent/HANDOFF.md
└── .github/workflows/
```

Common 只能放错误模型、日志/Trace、安全公共配置、测试工具等横切能力。禁止放所有服务共享 Entity、业务 Service、跨服务 Mapper 和巨型 `CommonUtils`。


# 7. Maven 工程与依赖

## 7.1 父工程职责

根 POM 只负责：模块聚合、Java 级别、Spring Boot Parent、Spring Cloud/SCA/MyBatis-Plus BOM、插件管理、Enforcer、测试和覆盖率聚合。业务依赖不得全部塞入父工程 `<dependencies>`。

## 7.2 BOM

```xml
<properties>
    <java.version>21</java.version>
    <spring-cloud.version>2025.1.2</spring-cloud.version>
    <spring-cloud-alibaba.version>2025.1.0.0</spring-cloud-alibaba.version>
    <mybatis-plus.version>3.5.17</mybatis-plus.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-bom</artifactId>
            <version>${mybatis-plus.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## 7.3 MVC 业务服务按需依赖

```text
spring-boot-starter-web
spring-boot-starter-validation
spring-boot-starter-actuator
spring-boot-starter-security
spring-boot-starter-oauth2-resource-server
mybatis-plus-spring-boot4-starter
mybatis-plus-jsqlparser
mysql-connector-j
flyway-core
flyway-mysql
spring-cloud-starter-alibaba-nacos-discovery
spring-cloud-starter-alibaba-nacos-config
spring-cloud-starter-openfeign
spring-cloud-starter-alibaba-sentinel
spring-boot-starter-data-redis
spring-boot-starter-amqp
micrometer-registry-prometheus
micrometer-tracing-bridge-otel
opentelemetry-exporter-otlp
```

只在需要的服务引入 Redis、AMQP、Feign。禁止所有服务无差别引入全部依赖。

## 7.4 Gateway 依赖边界

Gateway 使用 WebFlux：

```text
spring-cloud-starter-gateway-server-webflux
spring-cloud-starter-alibaba-nacos-discovery
spring-cloud-starter-alibaba-nacos-config
spring-cloud-starter-alibaba-sentinel
spring-boot-starter-oauth2-resource-server
spring-boot-starter-actuator
micrometer-registry-prometheus
micrometer-tracing-bridge-otel
opentelemetry-exporter-otlp
```

禁止引入 MVC、MyBatis-Plus、MySQL、Flyway、业务 Mapper 和业务事务。

## 7.5 Search Service

```text
spring-boot-starter-web
spring-boot-starter-validation
spring-boot-starter-actuator
spring-boot-starter-data-elasticsearch
spring-boot-starter-amqp
spring-cloud-starter-alibaba-nacos-discovery
spring-cloud-starter-alibaba-nacos-config
```

Search Service 不拥有资源事实数据，只拥有可重建索引。

## 7.6 MyBatis-Plus 使用边界

允许：`BaseMapper` 单表 CRUD、Lambda Wrapper、分页、自动填充、乐观锁插件、必要的逻辑删除。

复杂查询、条件扣减、状态迁移、批量对账必须写明确 SQL/XML。不得用 `saveOrUpdate` 声称解决并发安全。不得同时引入原生 MyBatis Starter 与 MyBatis-Plus Starter。

## 7.7 构建插件

必须固定：compiler、surefire、failsafe、JaCoCo、Enforcer、格式化、静态分析、CycloneDX、漏洞扫描、Spring Boot Maven Plugin。

Enforcer 至少检查：Java/Maven 最低版本、依赖收敛、重复类、Release 禁止 SNAPSHOT、插件版本固定。

---

# 8. 服务边界

## 8.1 Gateway

职责：入口、JWT 第一层校验、路由、CORS、请求大小限制、traceId、基础限流、安全响应头、外部错误格式。不得做资源归属、订单权限、数据库访问和业务事务。

## 8.2 Auth Service

职责：用户名密码认证、Access/Refresh Token、登录失败控制、密码策略、Token 撤销版本或黑名单。完整用户资料归 User Service。

## 8.3 User Service

职责：用户资料、用户状态、角色、违约状态、预约资格。

## 8.4 Resource Service

职责：资源、分类、规则、状态、时段、容量占用/释放和资源变更事件。v1.0 将 Resource 与 Slot 放在同一事务边界，禁止为展示服务数量强行拆库存服务。

## 8.5 Booking Service

职责：预约订单、Idempotency-Key、状态机、创建/确认/取消/超时、状态日志、Outbox、补偿与对账。不得访问 Resource 数据库。

## 8.6 Notification Service

职责：订阅事件、站内通知、模拟邮件、发送记录、去重、重试和死信。失败不得阻塞预约主链路。

## 8.7 Search Service

职责：ES 索引、搜索 API、变更消费、全量重建、Alias 切换和搜索降级。

---

# 9. 服务通信

## 9.1 同步调用

Feign 仅用于立即决定当前请求的短调用，例如 Booking 检查 User、占用或释放 Resource Slot。

每个 Client 必须明确 connect/read timeout、重试条件、fallback、指标和超时预算。GET 在明确幂等时可有限重试；创建、扣容量、取消等写操作默认不自动重试。写操作依赖 requestId 和状态查询解决“超时但可能成功”。

## 9.2 异步事件

RabbitMQ 用于通知、搜索同步、运营统计、超时触发、缓存失效广播等非核心副作用。

## 9.3 调用链长度

在线主链路最多：

```text
Gateway -> Booking -> User
                    -> Resource
```

禁止 A->B->C->D->E 长链；额外副作用改事件驱动。

---

# 10. API 规范

## 10.1 路径

```text
/api/v1/auth/**
/api/v1/users/**
/api/v1/resources/**
/api/v1/resource-slots/**
/api/v1/bookings/**
/api/v1/search/**
/api/v1/admin/**
```

## 10.2 HTTP 与响应

业务错误不得统一 HTTP 200。

成功：

```json
{"code":"OK","message":"success","data":{},"traceId":"..."}
```

错误：

```json
{
  "code":"BOOKING_SLOT_SOLD_OUT",
  "message":"该时段已无可用名额",
  "details":[],
  "traceId":"...",
  "timestamp":"2026-07-20T12:00:00Z"
}
```

错误码使用 `<DOMAIN>_<REASON>`，例如 `AUTH_INVALID_CREDENTIALS`、`BOOKING_DUPLICATE_REQUEST`、`DOWNSTREAM_TIMEOUT`、`RATE_LIMITED`。

## 10.3 幂等 Header

所有外部写请求评估幂等。创建预约使用：

```http
Idempotency-Key: UUID
```

Gateway 校验格式，Booking 持久化。不得同时定义多个互相冲突的规范来源。

## 10.4 分页

`pageNumber`、`pageSize`、`sort`；默认 20，最大 100。ES 深分页使用游标或 `search_after`，禁止无限制全表导出。

## 10.5 文档

核心 API 使用 Spring REST Docs，由测试生成真实请求、响应和错误码。OpenAPI UI 可附加，不得替代测试生成文档。

---

# 11. 安全基线

## 11.1 认证授权

- Spring Security + JWT Access Token + Refresh Token；
- Access Token 短时有效；
- 密码使用安全哈希；
- Gateway 做粗粒度入口检查，服务内部做资源级授权；
- 用户只能操作自己的预约；
- 管理员操作写审计日志；
- 服务推荐自身作为 Resource Server 验证 JWT，避免仅信任内部 Header。

## 11.2 输入与数据安全

- Bean Validation、枚举白名单、时间范围校验；
- MyBatis 参数绑定，禁止拼接用户 SQL；
- 禁止直接透传 ES DSL；
- 禁止任意类型反序列化；
- 文件上传限制大小、类型和存储路径；
- 日志不打印密码、完整 JWT、密钥和敏感个人信息。

## 11.3 Secrets

`.env` 不提交，只提交 `.env.example`；CI 使用 Secret Store；Nacos 不明文存放 JWT 私钥和生产式密码；日志必须脱敏。

## 11.4 Actuator

可公开 liveness/readiness；metrics、env、configprops、loggers、mappings 仅内网并受保护。演示环境禁用危险写端点。

---

# 12. 数据库原则

## 12.1 隔离

一个 MySQL 实例，多个库：

```text
venueflow_auth
venueflow_user
venueflow_resource
venueflow_booking
venueflow_notification
```

每个服务使用独立用户和最小权限。禁止跨库 JOIN、万能账号和直接访问其他服务数据库。

## 12.2 Flyway

结构变更必须通过：

```text
V001__init_schema.sql
V002__add_booking_request_id.sql
V003__add_outbox.sql
```

已发布脚本不修改；修复用新版本；测试从 Migration 建库；Hibernate `ddl-auto` 关闭或 validate。

## 12.3 主键与业务号

推荐：内部 `BIGINT` 自增主键；外部 `booking_no` 使用 ULID/不可枚举号；`event_id`、`request_id` 使用 UUID。不为“高技术”强行引入独立 ID 服务。

## 12.4 时间与金额

数据库存 UTC；Java 使用 `Instant`/`OffsetDateTime`；时段边界 `[start,end)`。金额使用 `DECIMAL` + `BigDecimal`，禁止 float/double。

---

# 13. 核心数据模型

## 13.1 Resource

```text
id, resource_no, name, category_id, description, location,
capacity, status, version, created_at, updated_at
```

状态：`DRAFT/ACTIVE/SUSPENDED/ARCHIVED`。

## 13.2 ResourceSlot

```text
id, slot_no, resource_id, start_time, end_time,
total_capacity, remaining_capacity, status, version,
created_at, updated_at
```

约束：起始早于结束；容量非负；剩余不超过总量；避免非法重叠和重复时段。

容量汇总必须配套可审计的 `SlotAllocation`：

```text
id, operation_id, booking_no, slot_id, quantity,
status, created_at, updated_at, released_at
```

状态：`HELD/CONFIRMED/RELEASED/EXPIRED`。`operation_id` 唯一；占用、查询、确认和释放均以该 ID 幂等。不得只依赖 `remaining_capacity` 推断某张预约是否已经占用或释放。

## 13.3 BookingIdempotency

```text
id, user_id, operation, idempotency_key, request_hash,
request_id, booking_no, status, created_at, updated_at, expires_at
```

唯一键：`(user_id, operation, idempotency_key)`。状态：`PROCESSING/SUCCEEDED/FAILED`。相同 Key 与相同请求指纹返回同一处理结果；相同 Key 与不同指纹返回冲突，不得再次调用 Resource。

## 13.4 BookingOrder

```text
id, booking_no, request_id, user_id, resource_id, slot_id,
quantity, status, expire_at, version, cancel_reason,
created_at, confirmed_at, cancelled_at, completed_at, updated_at
```

至少唯一：`booking_no`、`request_id`。

## 13.5 BookingStatusLog

记录订单、旧/新状态、来源、操作人、原因、traceId、eventId 和时间。

## 13.6 OutboxEvent

```text
id, event_id, aggregate_type, aggregate_id, event_type,
event_version, payload, headers, status, retry_count,
next_retry_at, created_at, published_at, last_error, version
```

状态：`NEW/PUBLISHING/PUBLISHED/RETRY/DEAD`。

## 13.7 ConsumedEvent

```text
id, consumer_name, event_id, event_type, consumed_at, result
```

唯一键：`(consumer_name,event_id)`。

---

# 14. 预约状态机

状态：

```text
PENDING_CONFIRMATION
CONFIRMED
CANCELLED
COMPLETED
EXPIRED
```

合法转换：

```text
PENDING_CONFIRMATION -> CONFIRMED/CANCELLED/EXPIRED
CONFIRMED -> CANCELLED（受规则限制）/COMPLETED
```

禁止已取消、完成或过期订单回到确认状态。

状态迁移必须条件更新：

```sql
UPDATE booking_order
SET status = :newStatus, version = version + 1, updated_at = :now
WHERE id = :id AND status = :expectedStatus AND version = :expectedVersion;
```

影响行数为 0 时重新查询并判断幂等或冲突，不得盲目覆盖。

---

# 15. 防超卖、幂等和跨服务一致性

## 15.1 第一版数据库扣减

```sql
UPDATE resource_slot
SET remaining_capacity = remaining_capacity - :quantity,
    version = version + 1,
    updated_at = :now
WHERE id = :slotId
  AND status = 'AVAILABLE'
  AND remaining_capacity >= :quantity;
```

影响 1 行才成功。

## 15.2 创建预约

```text
1. 校验 Idempotency-Key，并计算规范化请求的 request_hash
2. Booking 原子创建/读取 BookingIdempotency，唯一键为用户+操作+Key
3. 相同 Key 不同 request_hash 返回 409；相同请求由唯一执行者继续
4. 检查用户状态
5. 使用服务端 request_id/operation_id 调 Resource 幂等占用容量
6. Booking 本地事务创建订单、更新幂等结果并写 Outbox
7. 提交；并发重复请求返回同一订单结果
8. 若本地事务失败，按 operation_id 幂等释放容量或由补偿任务修复
```

v0.4 可先同步占用+补偿；v0.5 必须补齐状态查询、幂等释放、Outbox 和对账。

## 15.3 幂等层次

```text
Gateway 格式校验
+ BookingIdempotency 作用域唯一索引与 request_hash
+ Resource SlotAllocation.operation_id 唯一约束
+ 已有请求返回同一订单结果
+ 可选 Redis 短期快速拦截
```

Redis 不是最终事实，Booking 与 Resource 两侧的数据库约束必须存在。相同 Key 复用不同请求内容必须返回幂等冲突，不能静默返回旧订单。

## 15.4 Redis Lua 高并发版

只在数据库版有基准后引入。必须闭环解决 Redis 成功但 MQ/订单失败、数据丢失、重复释放、容量对账、过期和恢复。无法闭环不得作为主线。


# 16. Redis 设计

## 16.1 用途

Cache：资源详情、热门资源、时段摘要和搜索辅助数据。State：幂等快速标记、短期预约资格、分布式锁和限流状态。资源允许时拆实例；资源不足时至少分前缀、TTL 和监控。

## 16.2 Key 规范

```text
venueflow:{env}:resource:detail:{resourceId}
venueflow:{env}:resource:slot-summary:{resourceId}:{date}
venueflow:{env}:booking:idempotency:{requestId}
venueflow:{env}:booking:user-slot:{userId}:{slotId}
venueflow:{env}:lock:cache-rebuild:{resourceId}
```

全部带项目和环境前缀；禁止敏感信息、无 TTL 临时缓存、大 Key 和生产式 `KEYS`。

## 16.3 Cache Aside

读：缓存 miss 后查库并写缓存。写：更新数据库，事务提交后删除 Redis，再发布缓存失效事件通知其他实例清理本地缓存。

## 16.4 穿透、击穿、雪崩

- 穿透：空值短 TTL；确有需要才上布隆过滤器；
- 击穿：互斥重建或逻辑过期；
- 雪崩：TTL 抖动、分批预热、限流；
- 热点：Caffeine + Redis，但必须有失效广播和版本/TTL。

## 16.5 分布式锁

适用于单 Key 缓存重建、多实例定时任务互斥和少量临界区。必须有唯一 owner token、TTL、原子加锁、Lua 比较 token 解锁、等待/持有指标和超时处理。

不用于所有库存扣减，也不替代数据库唯一约束和状态机。

---

# 17. RabbitMQ 与可靠消息

## 17.1 拓扑

```text
venueflow.booking.exchange
venueflow.resource.exchange
venueflow.notification.exchange
venueflow.dead.exchange
```

业务事件使用 topic；队列、交换机 durable；消息 persistent；死信有独立路由。延迟消息优先 TTL+DLX；使用插件时必须记录部署依赖和回滚。

## 17.2 Event Envelope

```json
{
  "eventId":"uuid",
  "eventType":"booking.created.v1",
  "eventVersion":1,
  "aggregateId":"bookingNo",
  "occurredAt":"2026-07-20T12:00:00Z",
  "producer":"venueflow-booking-service",
  "traceId":"...",
  "payload":{}
}
```

事件必须全局唯一、带版本、限制大小、不含 secrets。大文件只传引用。

## 17.3 Producer

必须：Publisher Confirm、Publisher Return、mandatory 路由、confirm timeout、Outbox 状态更新、失败重试和指标。禁止以“发送方法没抛异常”作为成功依据。

## 17.4 Consumer

必须：手动 ACK、有限重试、退避、幂等、消费事务、死信、错误记录、人工重放入口。重放前必须验证幂等。

## 17.5 Outbox

业务本地事务同时写业务数据与 Outbox。独立投递器扫描 `NEW/RETRY`，抢占任务、发布、等待 Confirm 后标记 `PUBLISHED`。

多实例扫描可用 `FOR UPDATE SKIP LOCKED`、状态+版本 CAS 或分片。即使重复发布，消费者仍必须幂等。

## 17.6 Inbox/ConsumedEvent

消费事务中先插入 `(consumer_name,event_id)`，再执行业务并提交，最后 ACK。唯一键冲突表示已消费，校验后 ACK。

## 17.7 延迟取消

```text
booking.created -> 延迟 15 分钟 -> 查询订单最新状态
仅 PENDING_CONFIRMATION 执行条件取消 -> 幂等释放容量 -> booking.expired
```

延迟消息仅是触发器，不是订单事实。

## 17.8 堆积 Runbook

监控 ready、unacked、publish/ack rate、redeliver、consumer count、oldest age。处理顺序：判断生产消费差、消费者错误、暂停非关键生产、扩消费者、调整 prefetch、排查数据库、隔离毒消息、恢复后对账。

---

# 18. MySQL 与 Elasticsearch

## 18.1 主数据

MySQL 是事实来源，ES 是可重建投影。

## 18.2 增量同步

```text
Resource 本地事务：更新资源 + 写 resource.changed Outbox
Publisher：发布 resource.changed.v1
Search Consumer：按 resourceId 获取最新快照，依据 version 覆盖 ES
```

推荐事件携带 ID、版本和必要元数据，消费者获取最新快照，避免旧事件覆盖新数据。

## 18.3 索引版本与重建

```text
venueflow-resource-v1
venueflow-resource-read  -> alias
venueflow-resource-write -> alias
```

重建：新建索引、批量读 MySQL、写入、数量/抽样校验、原子切 Alias、保留旧索引、延迟删除。

## 18.4 开发资源

单节点、1 primary、0 replica、Heap 512MB~1GB，通过 Compose `search` profile 按需启动。

## 18.5 降级

ES 不可用时资源详情仍可用；搜索返回明确降级错误或有限 MySQL 查询，不能把故障伪装成“无结果”。

---

# 19. Nacos

## 19.1 Namespace 与 Group

```text
venueflow-dev
venueflow-test
venueflow-demo
Group: VENUEFLOW_GROUP
```

配置使用真实 Namespace ID。

## 19.2 Data ID

```text
venueflow-common.yml
venueflow-gateway.yml
venueflow-auth-service.yml
venueflow-user-service.yml
venueflow-resource-service.yml
venueflow-booking-service.yml
venueflow-notification-service.yml
venueflow-search-service.yml
```

## 19.3 配置导入

Spring Cloud Alibaba 2025.1 禁止 `bootstrap.yml`，使用：

```yaml
spring:
  application:
    name: venueflow-booking-service
  config:
    import:
      - optional:nacos:venueflow-common.yml?group=VENUEFLOW_GROUP
      - optional:nacos:venueflow-booking-service.yml?group=VENUEFLOW_GROUP
```

本地仅保留 Nacos 启动必需配置，敏感值通过环境变量。

## 19.4 内容边界

可存非敏感公共配置、超时、开关、日志级别和业务阈值。不明文存数据库生产密码、JWT 私钥和第三方 Token。

## 19.5 故障验证

验证已启动服务在 Nacos 暂时不可用时的行为、新实例注册失败、配置缓存、实例摘除、优雅上下线和恢复重连。

---

# 20. Gateway

## 20.1 显式路由

```text
/api/v1/auth/**      -> auth-service
/api/v1/users/**     -> user-service
/api/v1/resources/** -> resource-service
/api/v1/bookings/**  -> booking-service
/api/v1/search/**    -> search-service
```

禁止无控制自动暴露全部注册服务。

## 20.2 Filter 顺序

1. requestId/traceId；
2. 请求大小和安全头；
3. JWT；
4. 用户上下文；
5. 限流；
6. 路由；
7. 响应日志与指标。

进入网关先删除客户端伪造的 `X-User-Id/X-Role`。推荐业务服务自行验证 JWT。

## 20.3 CORS

只允许明确 Origin；credentials 时禁用 `*`；限制 Method/Header；dev 和 demo 分开。

---

# 21. Sentinel 与稳定性

## 21.1 接口策略

- 搜索：可限流和降级；
- 资源详情：核心数据可返回，非核心字段可降级；
- 创建预约：不能降级成成功，只能拒绝或排队；
- 通知：异步失败不阻塞主链路。

## 21.2 超时预算

Gateway 总预算 > Booking 预算 > Feign 子调用预算。禁止下游超时大于上游。

## 21.3 资源隔离

Web、Feign、Hikari、MQ Consumer、定时任务、ES 客户端分别关注。线程池必须命名、有界、有拒绝策略和指标，禁止所有任务共用无界公共池。

## 21.4 规则版本化

```text
deploy/sentinel/gateway-rules.json
deploy/sentinel/booking-rules.json
deploy/sentinel/search-rules.json
```

阈值来自压测，不拍脑袋填写。

---

# 22. 可观测性

## 22.1 日志字段

```text
timestamp level service instance traceId spanId
userId requestId bookingNo eventId errorCode durationMs
```

敏感字段脱敏；高频路径不打印巨大对象。

## 22.2 Metrics

系统：HTTP count、P50/P95/P99、errors、JVM、GC、CPU、线程、uptime。  
数据库：Hikari active/idle/pending、事务失败、慢操作。  
Redis：hit/miss、command latency、memory、evictions、lock wait。  
RabbitMQ：confirm failure、ready/unacked、redelivery、dead letter、consume duration。  
业务：booking success/rejected/duplicate/sold-out、timeout cancel、outbox pending/dead、search lag、reconciliation mismatch。

## 22.3 Trace

Micrometer Tracing + OpenTelemetry，跨 Gateway、Feign、RabbitMQ Header、关键数据库和 ES。异步事件传播 trace 上下文或建立 link。

## 22.4 Health

Liveness 只表示进程是否应重启；Readiness 表示是否可接流量。非关键依赖不得全部变成 readiness 强依赖。

## 22.5 Dashboard

至少：系统总览、Gateway、Booking、JVM、MySQL/Hikari、RabbitMQ、Outbox、ES 同步和故障演练。


# 23. 测试策略

## 23.1 分层

```text
Unit -> Slice -> Repository Integration -> Service Integration
-> Contract -> End-to-End -> Concurrency -> Performance -> Failure Drill
```

## 23.2 单元与集成

单元覆盖状态机、规则、过期、取消、事件和重试决策。集成使用 Testcontainers 启动真实 MySQL、Redis、RabbitMQ、ES；禁止用 H2 替代全部 MySQL 测试。

测试依赖按需：`spring-boot-starter-test`、Testcontainers JUnit/MySQL/RabbitMQ/Elasticsearch、Awaitility、ArchUnit。

## 23.3 架构测试

ArchUnit 保证：domain 不依赖 interfaces/infrastructure；Controller 不直接调用 Mapper；Gateway 不依赖 JDBC；Entity 不作为 API DTO；Common 不包含业务；服务不依赖其他服务 persistence。

## 23.4 契约

Feign 契约记录字段、错误码、超时和兼容策略。新增字段向后兼容，删除/变义需版本升级。可用 Spring Cloud Contract，或 contract module + Provider Integration Test。

## 23.5 并发场景

必须测试：同 requestId、不同 requestId 抢同一容量、取消与确认竞争、超时与确认竞争、重复释放、重复消息、Outbox 多实例扫描、缓存重建竞争。

## 23.6 覆盖率

建议 domain 90%、application 80%、全项目 70%。关键分支必须覆盖，覆盖率不替代断言质量。

测试命名使用 `givenX_whenY_thenZ`；集成测试以 `*IT` 结尾并由 Failsafe 执行。

---

# 24. 质量门禁

## 24.1 本地

```bash
./mvnw -pl <module> -am test
```

## 24.2 合并

```bash
./mvnw clean verify
```

包含：编译、单元/集成/架构测试、格式、静态分析、JaCoCo、依赖收敛、Flyway 空库迁移、API 文档、镜像构建和分阶段 Compose 冒烟。

## 24.3 安全

生成 SBOM；依赖、Secret、Dockerfile 和镜像扫描。高危漏洞必须修复，或在 `docs/security/accepted-risks.md` 记录缓解、责任人和到期时间。

## 24.4 Definition of Done

```text
[ ] proposal/spec/design/tasks 完整
[ ] 服务边界未破坏
[ ] Migration 完成
[ ] API/事件契约完成
[ ] 正常和失败流程完成
[ ] 幂等、并发、超时、重试完成
[ ] 日志、指标、Trace 完成
[ ] 单元、集成、架构测试完成
[ ] 安全检查完成
[ ] 文档和回滚完成
[ ] OPSX verify 通过
[ ] HANDOFF 更新或清空
```

---

# 25. Docker Compose 与端口

## 25.1 Profiles

```text
base: mysql redis rabbitmq nacos
search: elasticsearch
observe: prometheus grafana otel-collector jaeger
app: gateway services
```

所有组件有 healthcheck。应用不能只依赖 `depends_on` 顺序，必须支持重连和 readiness。

## 25.2 数据卷

```text
mysql-data redis-data rabbitmq-data nacos-data
elasticsearch-data grafana-data prometheus-data
```

删除卷必须二次确认。

## 25.3 资源预算

- ES：`-Xms512m -Xmx512m` 起；
- Java 服务：`-Xms128m -Xmx256m` 起；
- Gateway 可 `-Xmx384m`；
- 观察组件按 profile 启动；
- 不在低内存环境默认全开。

## 25.4 Dockerfile

多阶段、非 root、只复制最终 JAR、exec form ENTRYPOINT、JVM 参数环境化、固定 JRE 镜像，v1.0 锁 digest。

## 25.5 端口

| 组件 | 端口 |
|---|---:|
| Gateway | 8080 |
| Auth/User/Resource/Booking/Notification/Search | 8081~8086 |
| MySQL | 3306 |
| Redis | 6379 |
| RabbitMQ / Management | 5672 / 15672 |
| Nacos HTTP/gRPC | 8848 / 9848 / 9849 |
| Elasticsearch | 9200 |
| Prometheus/Grafana | 9090 / 3000 |
| OTLP gRPC/HTTP | 4317 / 4318 |
| Jaeger UI | 16686 |

---

# 26. 配置分层

本地 `application.yml` 只保留应用名、端口、profile、Nacos 地址和 `spring.config.import`。

Nacos common 放 JSON、日志、Actuator、Tracing、通用超时等非敏感配置；服务配置放数据源引用、Redis、MQ、Feign、Sentinel 和业务阈值；密码和密钥走环境变量。

环境变量规范：

```text
VENUEFLOW_ENV NACOS_SERVER_ADDR NACOS_NAMESPACE
VMWARE_HOST_ONLY_CIDR VMWARE_HOST_IP INFRA_NODE_IP APP_NODE_IP
MYSQL_HOST MYSQL_PORT MYSQL_DATABASE MYSQL_USERNAME MYSQL_PASSWORD
REDIS_HOST REDIS_PASSWORD
RABBITMQ_HOST RABBITMQ_USERNAME RABBITMQ_PASSWORD
JWT_PUBLIC_KEY JWT_PRIVATE_KEY
OTEL_EXPORTER_OTLP_ENDPOINT
```

不得在启动日志打印实际敏感值。

---

# 27. 事务、补偿与对账

本地事务只覆盖本服务数据库。事务内禁止长网络调用、等待 MQ、文件上传、ES 操作和人为 sleep。

v1.0 跨服务主线：

```text
本地事务 + 幂等接口 + 状态机 + Outbox + 补偿 + 定时对账
```

Seata 仅做 `labs/seata-at/` 对比实验，不作为默认主线。

补偿必须幂等、可重试、可观察、可人工触发并记录原因。至少实现 Booking/Resource 占用、Outbox/业务状态、MySQL/ES 版本、通知事件/发送记录对账。

对账表建议：`reconciliation_run`、`reconciliation_issue`、`repair_action`。

---

# 28. CI/CD 与回滚

## 28.1 PR CI

checkout -> JDK 21 -> Maven Cache -> format/static -> unit -> integration -> architecture -> package -> SBOM -> vulnerability -> Docker build -> artifact upload。

## 28.2 Main/Release

额外执行 Compose 冒烟、候选镜像、版本清单和报告。Release 包含 Git Tag、Notes、Migration 列表、镜像 digest、配置变化、已知问题和回滚步骤。

## 28.3 数据库兼容

使用 expand/contract，避免破坏性 Migration；应用回滚时旧版本仍能读取新结构。禁止依赖自动 down migration。

---

# 29. 分阶段实施

## v0.1.0 工程骨架

v0.1.0 是由以下三个有序 Change 组成的里程碑，不要求在单个 Change 中一次生成：

```text
C01 bootstrap-engineering-baseline
C02 add-base-infrastructure
C03 add-resource-service-skeleton
```

C01 完成 Git 友好文件、Maven 多模块/BOM/Wrapper/Enforcer、CI 和 OPSX/HANDOFF/ADR 模板；C02 增加基础 Compose 与 MySQL/Redis/RabbitMQ/Nacos；C03 增加最小 Resource Service 与 Actuator。每个 Change 必须独立验证。

## v0.2.0 业务闭环

不使用 Redis/MQ/ES，完成登录、资源、时段、数据库扣容量、预约、取消和核销。

## v0.3.0 微服务治理

Gateway、Auth/User/Resource/Booking、Nacos、Feign、JWT、两个 Resource 实例和 traceId。停止一个实例后仍可服务。

## v0.4.0 并发

Idempotency-Key、唯一索引、条件扣减、状态机和竞争测试；验证无超卖、无重复。

## v0.5.0 可靠事件

Outbox、Confirm、Manual ACK、ConsumedEvent、死信、超时取消和对账；验证宕机恢复、重复和毒消息。

## v0.6.0 缓存与搜索

Redis Cache Aside、击穿保护、ES、增量同步、全量重建和 Alias；验证 ES 停机恢复收敛。

## v0.7.0 稳定性与观测

Sentinel、超时预算、限流、熔断、Prometheus、Grafana、OTel 和 Dashboard。

## v0.8.0 质量与故障

全链路自动化、安全扫描、故障脚本、回滚和 Runbook。

## v0.9.0 真实试用

邀请 5~20 名真实用户完成任务，记录完成率、问题、修复和复测。

## v1.0.0 简历发布

归档架构、API、ER、压测、故障、监控、回滚、用户验证、简历描述和面试问答。

---

# 30. 必做故障演练

1. Notification Consumer 宕机：积压、恢复、无重复业务；
2. Elasticsearch 宕机：MySQL 可写、事件保留、恢复收敛；
3. Resource 单实例宕机：Nacos 摘除、剩余实例接管；
4. 下游 3 秒延迟：Feign 超时、Sentinel、线程池和 P99；
5. 重复 eventId：业务一次生效；
6. Outbox Publisher 宕机：业务提交、恢复后发布；
7. Redis 失效失败：旧数据窗口与补偿；
8. Hikari 耗尽：慢 SQL、pending、超时和保护；
9. 应用版本回滚：健康失败后恢复旧镜像且 DB 向后兼容；
10. 人为制造 Booking/Resource 不一致：对账发现并修复。

---

# 31. 性能测试

推荐 k6，脚本提交至：

```text
scripts/pressure-test/booking-create.js
scripts/pressure-test/resource-search.js
scripts/pressure-test/mixed-workload.js
```

每次记录 CPU/RAM、VM、Docker、Java、Commit、数据规模、VU、时长、预热、网络、JVM 和数据库参数。

测试类型：阶梯加压、稳态 15~30 分钟、突发、混合读写、故障期间和恢复能力。

指标：throughput、success、P50/P95/P99/max、错误码、CPU、Heap/GC、Hikari、Redis latency、MQ lag、Outbox backlog。

防超卖场景：初始容量 100、并发请求至少 1000；期望成功订单 100、剩余 0、重复 0、负库存 0。最终数据以实测为准。

---

# 32. 真实用户验证

邀请至少 5 人，推荐 10~20 人完成注册、查找、预约、重复点击、冲突预约、取消、核销和历史查询。

记录任务完成率、耗时、错误提示理解度、反馈、缺陷、修复 Commit 和复测结果。禁止伪造 DAU、订单量和用户规模。

---

# 33. 文档与 ADR

必须存在：

```text
README.md
docs/architecture/system-context.md
docs/architecture/container-view.md
docs/architecture/booking-sequence.md
docs/architecture/event-flow.md
docs/architecture/deployment.md
docs/architecture/data-model.md
docs/runbook/
docs/adr/
docs/benchmark/
docs/failure-drills/
docs/security/
docs/resume/
```

ADR 模板：状态、背景、决策、候选、原因、后果、风险、验证、复审日期。

重点 ADR：先单体闭环再拆分；Resource 与 Slot 同服务；Outbox；Seata 只对比；数据库是容量事实；ES 可重建；Redis 非最终幂等事实；选择 Boot 4.0.7；暂不采用 Kubernetes。

---

# 34. Codex 操作协议

## 34.1 开始时

检查：

```bash
git status
git branch --show-current
git log -5 --oneline
```

读取：本文档、HANDOFF、当前 Change、相关 ADR 和模块 README。

Windows PowerShell 若因执行策略无法运行 `openspec.ps1`，使用安装目录或 PATH 中的 `openspec.cmd`；不得为执行项目命令而静默放宽系统级执行策略。

## 34.2 修改前

确认当前 Change/task、文件范围、测试、Migration、契约、风险和是否需要 ADR。

## 34.3 修改后

格式化、模块测试、必要集成测试、检查 diff、更新 task/文档/HANDOFF。除非用户明确要求，不自动提交和合并。

## 34.4 Stop Conditions

遇到以下情况停止扩展并报告：BOM 不兼容、依赖冲突、Migration 失败、核心测试失败、可能丢数据、跨服务数据库访问、需覆盖未提交修改、需破坏性清库、需真实密钥、规格矛盾、无法证明幂等、无限重试、消息失败无落点或回滚不可行。

---

# 35. Codex 首次 Change

只执行 `bootstrap-engineering-baseline`：

- 初始化 Git 友好文件；
- Maven 多模块、BOM、Wrapper、Enforcer；
- 测试、格式、覆盖率框架；
- OPSX、ADR、HANDOFF；
- `.editorconfig`、`.gitattributes`、`.gitignore`、`.env.example`；
- CI Skeleton。

验收：

```bash
./mvnw clean verify
```

第二个 Change `add-base-infrastructure` 才做基础 Compose；第三个 Change `add-resource-service-skeleton` 做 Resource Service Skeleton；第四个 Change 才进入 Booking 最小流程。

---

# 36. 禁止清单

禁止：一次生成全部服务却无测试；复制粘贴六套代码；共享业务 Entity；跨库查询；Redis 锁解决所有并发；用 `@Transactional` 声称解决跨服务事务；改库后直接发 MQ 且无 Outbox；自动 ACK 后处理；延迟消息无条件取消；无限重试；吞异常；所有错误 HTTP 200；Entity 直接作为 API；Controller 调 Mapper；Gateway 写业务；H2 替代全部 MySQL 测试；`latest`；公网暴露中间件；编造压测/用户数据；强行 Kubernetes；无 Change 升级核心版本；修改旧 Migration；自动删卷；提交 secret；自动执行破坏命令。

---

# 37. 面试问答验收

必须能回答：为什么微服务、为何这样拆、为何 Resource/Slot 不拆、为何禁跨库、幂等、Redis 故障、防超卖、Feign 超时但成功、容量已扣但订单失败、Outbox、重复发布、消费者幂等、ACK 前后宕机、死信、延迟取消、MySQL/ES 一致性、索引重建、缓存一致性、分布式锁边界、Sentinel 层次、重试边界、Nacos 故障、MQ 堆积、对账、单点、压测可复现、Seata vs Outbox、为何不用 Kubernetes、重新设计改什么。

统一回答结构：

```text
业务背景 -> 约束 -> 候选方案 -> 选择 -> 实现
-> 失败模式 -> 监控 -> 补偿 -> 验证 -> 局限
```

---

# 38. 简历模板与真实性

```text
VenueFlow 园区共享资源预约平台

基于 Spring Boot、Spring Cloud Alibaba 构建共享资源预约平台，按认证、用户、资源时段、预约履约、搜索和通知等领域划分服务，使用 Nacos 实现服务发现与配置管理，Gateway 与 Spring Security 完成统一入口及 JWT 鉴权。

针对热点时段预约，设计 Idempotency-Key、数据库唯一约束、条件扣减和订单状态机等多层幂等及防超卖机制；在【真实环境】下以【实际并发】请求竞争【实际容量】，实测成功订单【数量】、重复订单 0、超卖 0，P95【实测值】。

基于本地事务 Outbox、RabbitMQ Publisher Confirm、消费者手动 ACK、eventId 去重、死信和定时对账，实现超时取消、异步通知及 MySQL—Elasticsearch 最终一致；通过消费者宕机、重复投递和 Elasticsearch 中断等演练验证恢复后数据收敛。

使用 Sentinel、Actuator、Prometheus、Grafana 和 OpenTelemetry 建立限流、熔断、接口延迟、JVM、连接池、消息堆积和业务指标监控；使用 Testcontainers、ArchUnit 和 CI 覆盖数据库、缓存、消息、架构边界及核心并发场景。
```

占位符只能用真实结果替换。

---

# 39. 最终交付物

```text
[ ] 可运行源代码、Git 历史、Tags/Releases
[ ] OPSX Changes、HANDOFF、ADR
[ ] 架构/部署/时序/ER 图
[ ] API 文档、Flyway、Compose、镜像 digest lock
[ ] 初始化、备份、恢复和冒烟脚本
[ ] 单元、集成、契约、架构、并发测试
[ ] 压测脚本、原始数据、Grafana Dashboard
[ ] 故障演练、Runbook、回滚报告
[ ] 安全扫描、SBOM
[ ] 真实用户验证
[ ] 简历描述与高强度问答
```

---

# 40. 给 Codex 的第一条指令

```text
请先完整阅读仓库根目录的 VENUEFLOW_FULL_CHAIN_ENGINEERING_SPEC.md。

当前只执行第一个 OPSX Change：bootstrap-engineering-baseline。
不要创建全部业务服务，不要引入 Redis 业务逻辑、RabbitMQ 业务逻辑、Elasticsearch、Seata 或 Kubernetes。

先检查 git status、当前目录和已有文件；不得覆盖未提交内容。
创建 proposal.md、design.md、tasks.md，并把任务拆为可逐项验证的小任务。
固定 JDK 21、Spring Boot 4.0.7、Spring Cloud 2025.1.2、Spring Cloud Alibaba 2025.1.0.0、MyBatis-Plus 3.5.17。
初始化 Maven Wrapper、多模块父工程、BOM、Enforcer、基础测试、格式与覆盖率框架、OPSX 目录、ADR 模板、HANDOFF 模板、.editorconfig、.gitattributes、.gitignore、.env.example 和 CI skeleton。

每完成一个任务，运行对应最小测试并更新 tasks.md。
遇到依赖不兼容、测试失败或需要破坏性操作时停止扩展并报告。
不要自动提交、不要自动合并、不要执行清库。
最终输出修改文件、命令、测试结果、未完成项和下一步。
```

---

# 41. 官方参考

- Spring Boot: https://spring.io/projects/spring-boot/
- Spring Cloud: https://spring.io/projects/spring-cloud/
- Spring Cloud Alibaba Releases: https://github.com/alibaba/spring-cloud-alibaba/releases
- Spring Cloud Alibaba Docs: https://sca.aliyun.com/en/docs/2025.x/
- MyBatis-Plus: https://baomidou.com/
- Spring Data Elasticsearch Matrix: https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/versions.html
- Docker Compose: https://docs.docker.com/compose/
- MySQL 8.4: https://dev.mysql.com/doc/refman/8.4/en/
- Redis: https://redis.io/docs/
- RabbitMQ: https://www.rabbitmq.com/docs
- Nacos: https://nacos.io/docs/
- Testcontainers: https://java.testcontainers.org/
- OpenSpec OPSX: https://github.com/Fission-AI/OpenSpec/blob/main/docs/opsx.md

---

# 42. 文档维护

修改本文件必须创建 OPSX Change，说明原因、影响、迁移和回滚，更新文档版本；重大架构变化增加 ADR。会话交接必须在 HANDOFF 记录本文档版本。

一次性 pre-bootstrap 例外：当仓库尚无首次 Git 提交、OpenSpec 已初始化且本文件存在阻断工程启动的已知冲突时，允许通过 `revise-engineering-spec-baseline` Change 修订初始规范。该 Change 必须保留 proposal、spec、design、tasks 与验证结果；首次工程基线建立后，本例外自动失效。

**最终原则：** 技术含量不由服务和中间件数量决定，而由边界是否合理、一致性是否闭环、幂等与并发是否可证明、消息是否有可靠落点、故障是否可检测恢复、数据是否可对账、性能是否有原始证据、变更是否可审计、业务是否被真实用户验证决定。
