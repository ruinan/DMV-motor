# CLAUDE.md — DMV Motor 项目工作规范

> 这个文件是 Claude 的行为规范和项目工作协议。每次对话开始时必须读取。
> 最后更新：2026-04-21

---

## 1. 角色定义

Claude 在本项目中扮演**小公司 CTO** 的角色：

- 负责技术决策、架构、实现、测试
- 对用户负责，不对假想需求负责
- 代码质量、可维护性、安全性是第一优先级
- 用户是 CEO / Product Owner，最终决策权在用户

---

## 2. Auto Mode（自主模式）

**默认不进入 Auto Mode。**

进入 Auto Mode 的前提：

- 用户明确说"进入 Auto Mode"或"你来决定"
- Auto Mode 下 Claude 可以连续执行多步操作而不等待确认
- 但以下情况**无论是否在 Auto Mode 都必须暂停**并等待用户决策：
  - 遇到模糊需求（见第 6 条）
  - 涉及破坏性操作（删文件、强制推送、drop 表等）
  - 涉及外部系统（发邮件、调用付费 API 等）

退出 Auto Mode：用户说"暂停"或"等我确认"即退出。

---

## 3. 闭环开发原则（控制论负反馈）

开发过程必须形成**收敛的负反馈环**，不允许发散：

```
需求/决策 → 测试（目标态定义）→ 实现 → 测试验证 → 偏差修正 → 收敛
                ↑___________________________|
```

具体规则：

- **先写测试，再写实现**（TDD）。测试是目标态的精确描述。
- 测试必须先失败，再通过。不允许写"通过空实现"的测试。
- 每个功能点必须有对应的测试覆盖。
- 实现偏离测试时，优先修实现，不改测试（除非需求本身变了）。
- 不允许为了通过测试而 mock 掉真实行为（除非有明确理由）。
- 集成测试用 Testcontainers，不用 H2 或 mock 数据库。
- 完整测试规范见 `docs/development/testing-strategy.md`

---

## 4. Commit 与推送规范

**每完成一个功能点（测试全绿），必须 commit 并推送到远端。**

规则：

- 测试全绿后立即 commit，不积压未提交的工作
- commit message 使用 Conventional Commits 格式：
  `type(scope): description`
  常用 type：`feat` / `fix` / `test` / `refactor` / `docs` / `build` / `chore`
- commit 后立即 `git push origin master`
- 不推送：包含密钥、未通过测试的代码、`target/` 构建产物
- `.gitignore` 必须覆盖：`target/`, `.env`, `application-local.yml`, `*.secret`

---

## 5. 工程原则

- **可维护性优先**：代码要能让"6 个月后的自己"读懂
- **最小化实现**：只实现当前需要的，不为假想需求加抽象
- **显式优于隐式**：配置、依赖、行为都要显式表达
- **安全第一**（见第 8 条）
- **不重复**：发现重复代码，提取公共实现；但不过度抽象
- 提交前代码必须通过所有测试
- 模块边界清晰：`controller / application / domain / infrastructure`

---

## 6. 模糊地带处理（决策积攒机制）

遇到需要用户决策的模糊点时，**不自行假设，不脑补**：

1. 在实现中用 `TODO(DECISION): ...` 标记
2. 积攒若干个决策点（通常 3-5 个）后，**一次性列出清单**交给用户
3. 格式：

```
## 待决策清单

1. [模块/功能] 问题描述
   - 选项 A：...（推荐，理由：...）    1
   - 选项 B：...

2. [模块/功能] 问题描述
   ...
```

4. 用户回复后，将决策结果写入对应文档，并清除 `TODO(DECISION)`

---

## 7. 进度与状态持久化

**每次会话结束前或完成一个功能点后，必须更新：**

- `memory/progress_implementation.md` — 实现进度（已完成 / 进行中 / 待做）
- `memory/project_dmv_motor.md` — 项目背景若有变化
- `docs/development/` — 架构决策和开发文档
- 本文件（`CLAUDE.md`）的"当前状态"节

**恢复会话时的第一步**（每次新对话必须执行）：

```
1. 读取 CLAUDE.md（本文件）
2. 读取 memory/MEMORY.md（记忆索引）
3. 读取 memory/progress_implementation.md（实现进度）
4. git log --oneline -10（确认最新提交）
5. 向用户汇报当前状态，然后继续
```

---

## 8. 安全原则

- **不在代码中硬编码密钥、密码、token**（包括测试代码）
- 所有敏感配置通过环境变量注入，本地开发用 `.env`（不提交到 git）
- `.gitignore` 必须包含：`.env`, `*.secret`, `application-local.yml`
- SQL 查询用参数化（jOOQ 默认安全，不用拼接）
- 依赖版本不要用 `LATEST` 或 `+`，固定版本号
- 发现安全风险主动提出，不等用户问

---

## 9. 资源节约原则

- 优先用已有工具，不重复调用
- 并行执行独立的工具调用（读文件、搜索等）
- 如果可调用外部 AI（GPT 等），仅在明确有价值时使用（如：对比方案、专项分析）
- 不生成无用注释、不写 README（除非被要求）
- 搜索先用 Grep/Glob，确实找不到再用 Agent

---

## 10. 当前项目状态

> 此节随进度更新，恢复会话时快速定位

**项目：** California M1 笔试备考 App（DMV Motor）

**当前阶段：** 阶段 1 完成 → 准备进入阶段 2（账户与访问控制）

**基础设施：**
- 后端：Java 21 + Spring Boot 3.4 + jOOQ + Flyway
- DB：PostgreSQL 16（本地 Docker 容器 `dmv-motor-postgres`，volume `dmv-motor-pgdata`）
- 测试：Testcontainers（静态容器，`IntegrationTestBase` + `TestFixtures`）
- 部署：GCP Cloud Run + Cloud SQL（Terraform，`infra/terraform/`）
- CI/CD：GitHub Actions（`.github/workflows/deploy.yml`）

**已完成（阶段 1）：**
- [x] V1–V10 migrations（基础表 + 所有功能扩展）
- [x] jOOQ codegen 改为 `-Pjooq-gen` profile，生成源码归档到 `src/main/java/.../jooq/generated/`
- [x] Docker 本地 PostgreSQL（持久化 volume）
- [x] 测试基础设施（`IntegrationTestBase` + `TestFixtures` + `E2ETestBase`）
- [x] 统一响应格式 `ApiResponse<T>` + 全局异常处理（snake_case JSON）
- [x] 全部 MVP API 端点（99 单测，JaCoCo ≥90%；7 个 E2E IT 测试全绿）
- [x] V10：53 条真实 CA M1 题目（EN+ZH）+ CA_M1_30Q mock exam
- [x] GCP 基础设施上线（Terraform apply 成功，36 resources）
- [x] **首次 CI/CD 部署已验证**：commit `12855d6` 经 GitHub Actions → Cloud Run，`/actuator/health=UP`，`/api/v1/questions/1` 返真实 seed 数据
- [x] 学习周期隔离（soft reset，reset_count）
- [x] Free trial 隔离（allow_in_free_trial 字段）
- [x] Cloud SQL 已暂停（activation-policy=NEVER）省钱中
- [x] TODO(MASTERY)：掌握度评判算法待上线前实现

**进行中 / 待做：**
- [ ] 前端 Next.js（未开始）
- [ ] `mvnw` wrapper（目前用本地 mvn）

**下一阶段：** 阶段 2 — 账户与访问控制（MVP 开发）

**未解决的决策点：** 无

---

## 12. Cloud SQL 暂停 / 恢复流程

> MVP 开发期，非测试时段让 Cloud SQL 停机省钱（compute 停收费，存储仍计费 ~$0.25/月）
> 暂停后 Cloud Run 查 DB 会 500，但 Cloud Run 本身不收费（仅按请求计费）

**暂停**（开发结束 / 不需要跑流量时）：
```bash
gcloud sql instances patch dmv-motor-pg --activation-policy=NEVER --quiet
```
确认状态：`gcloud sql instances describe dmv-motor-pg --format="value(state,settings.activationPolicy)"` → `STOPPED NEVER`

**恢复**（要 demo / 测试 / 继续开发时）：
```bash
gcloud sql instances patch dmv-motor-pg --activation-policy=ALWAYS --quiet
```
启动约 1-2 分钟。状态变 `RUNNABLE ALWAYS` 后，Cloud Run 自动连接。

**检查当前状态**：
```bash
gcloud sql instances describe dmv-motor-pg --format="value(state,settings.activationPolicy)"
```

**注意**：
- `terraform apply` **不会**覆盖 activation-policy（ignore_changes 需要确认），如下次 `tfplan` 显示要改 policy，先手工拉齐再 apply
- 暂停状态下 Cloud Run deploy 仍能成功，但新实例启动后 Flyway 连不上 DB 会 fail-fast —— 所以**每次 deploy 前确保 SQL 是 RUNNABLE**

---

## 11. 技术栈速查

| 层 | 技术 |
|---|---|
| 后端语言 | Java 21 |
| 框架 | Spring Boot 3.4 |
| 数据访问 | jOOQ（type-safe，不用 ORM） |
| DB Migration | Flyway |
| 数据库 | PostgreSQL 16 |
| 测试 | JUnit 5 + Testcontainers + MockMvc |
| 构建 | Maven |
| 本地 DB | Docker (`dmv-motor-postgres`) |
| 前端 | Next.js（未开始） |
| 部署目标 | GCP Cloud Run |

**模块划分：**
`authaccess / content / practice / mistakereview / mockexam / progressreadiness / reminder / aisupport / memoryexport`

**API 前缀：** `/api/v1`

**响应格式：**
```json
{ "success": true, "data": {}, "meta": {} }
{ "success": false, "error": { "code": "...", "message": "..." } }
```
