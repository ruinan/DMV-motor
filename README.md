# DMV Motor

一个只为通过 California Motorcycle Written Test / M1 permit written test 服务的学习工具。

**当前阶段：产品与技术设计已基本收口，进入 MVP 代码实现阶段。**

## 文档结构

### 产品设计文档（`docs/`）

| 文档 | 说明 |
|---|---|
| [MVP 设计](./docs/mvp.md) | MVP 范围、验收标准、不做什么 |
| [功能拆解](./docs/features.md) | 各功能模块职责 |
| [题目系统](./docs/question-system.md) | 题目组织方式与分层 |
| [题目 Topic 体系](./docs/question-topics.md) | Topic 树与分类 |
| [复习系统](./docs/review-system.md) | 复习系统职责与输入输出 |
| [模拟考试](./docs/mock-exam.md) | Mock exam 规则与退出机制 |
| [提醒与 Readiness](./docs/reminder-and-readiness.md) | 提醒触发场景与 readiness 定位 |
| [账户与访问](./docs/account-and-access.md) | 账户状态与访问边界 |
| [商业模式](./docs/business-model.md) | 付费模式与访问期设计 |
| [语言支持](./docs/language-support.md) | 双语支持规则 |
| [产品参数](./docs/parameters.md) | 所有可配置产品参数的**唯一定义**（免费体验、review、mastery、readiness、提醒等） |
| [数据模型](./docs/data-model.md) | 核心实体与关系 |
| [API 设计](./docs/api.md) | 产品层 API 边界（上游）|
| [AI 架构](./docs/ai-architecture.md) | AI 接入策略 |
| [数据库 Schema](./docs/db-schema.md) | 逻辑 Schema 与字段方向 |
| [Access Pass 生命周期](./docs/access-pass-and-quota-lifecycle.md) | 付费访问期与配额规则 |
| [复习与 Readiness 引擎](./docs/review-and-readiness-engine.md) | 引擎设计决策 |
| [运行时参数](./docs/runtime-parameters.md) | 运行时参数与成本控制策略 |
| [技术架构](./docs/technical-architecture.md) | 模块划分与同步/异步边界 |

### 开发实现文档（`docs/development/`）

| 文档 | 说明 |
|---|---|
| [技术选型决策](./docs/development/tech-stack-decision.md) | 技术栈选型理由与仓库结构 |
| [后端结构](./docs/development/backend-structure.md) | Spring Boot 包结构与分层规范 |
| [API 合同](./docs/development/api-contract.md) | HTTP 接口字段级定义（下游，开发以此为准） |
| [实现推进顺序](./docs/development/implementation-roadmap.md) | MVP 6 阶段实现路线 |

## 当前决策原则

- 所有设计都必须服务”提高 M1 笔试通过率”
- 不把产品扩展成安全教育平台或大而全学习系统
- 同一规则只有一份权威定义；开发遇到多处定义冲突时，以 `docs/development/` 下的文档为准，并反馈修正上游
