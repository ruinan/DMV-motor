# CLAUDE.md — DMV Motor 项目工作规范

> 这个文件是 Claude 的行为规范和项目工作协议。每次对话开始时必须读取。
> 最后更新：2026-06-04（**多考试扩展：第二考试 CA-C（加州 C 类/小汽车）已落地 V27 + 多考试解耦审计与 2 修；本地全部 push origin/master `58d00e3`（一次 15 commits）**。仅支持 CA（CA-M1+CA-C）。prod 仍 revision `00050-nhk`/Flyway V22，Cloud SQL STOPPED。进行中：aiqgen 版权安全改造 + 生成 CA-C 题库使其本地可测。详见 memory progress §39）

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

## 7.5 多 agent 工作流

**主 agent 协调，subagent 执行。** 自定义 subagent 定义放在 `.claude/agents/<name>.md`（YAML frontmatter + system prompt）。

**核心原则**：

- **单一职责**：每个 subagent 只做一件事，描述里写清楚什么时候用。Subagent 越泛 → 越像第二个主 agent，杠杆消失。
- **冷启动隔离**：subagent 不继承主对话的 context，每次 spawn 重新读必要文件。所以**任务粒度要够大**才划算（启动开销 ~几千 token）。小任务（编一个函数、改一个文案）不值得 spawn。
- **文件即共享内存**：subagent 之间不直接通信。共享通过文件——`memory/`（持久学习）、`docs/`（架构 / 决策）、本仓库代码本身。Subagent 完成后**返回结构化文字摘要**给主 agent，主 agent 决定哪些进 memory。
- **并行化 only when independent**：真独立的活（"前端 lint + 后端 verify"）一条消息里发多个 Agent 调用并行跑。有依赖（"先看 review 结果再写代码"）必须串行。
- **写权限收紧**：review / observation 类 subagent 只给 Read/Grep/Glob/Bash，不给 Edit/Write，物理上做不到产生副作用。

**主 agent 的责任**：
- 写清晰的 brief（goal + context + 已验证的事实 + 预期产出格式）
- 决定何时 spawn vs 自己做
- 把 subagent 的报告综合进 memory / 决策 / 下一步

**Subagent 的责任**：
- 严格按 brief 执行，不超出范围
- 报告要结构化（findings / actions / blockers / 下一步建议）
- 不擅自修改 memory（由主 agent 集中管理）

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

**当前阶段：** **Phase B Study Hub + 计时/复盘批 + 部署前审计 + 验证期 bug 修复 全部完成并已上 prod（2026-05-30，revision `00050-nhk`，Flyway prod schema=V22）。验证后停 Cloud SQL 省钱。** **【2026-05-30 dev-audit 修复批，6 commit 已 push origin/master `b3eec79`，未部署】** 用户跑的代码质量审计 6 项全部闭环：#1 topic-filtered practice total 偏大（正确性 bug，已修）、bug5 切语言侧栏消失（前端"很严重"，已修）、#5 controller inline Map→typed DTO record、#4a 抽 `MockScoringPolicy`、#3a 抽 `PracticeQuestionSelector`、#3b 抽 history/stats 读 DAO（`PracticeHistoryDao`/`MockHistoryDao`）。362 tests 绿、JaCoCo branch 90.7%。**Deferred（含理由）**：#4b（service 再拆 lifecycle/read=对 ~340 行 service 过度抽象）、#2（前端拆 hook → 并入 enhance1 新会话）、#6（mastery 第 3 闸门 → 等 confusion schema）。**⚠️ 此次 push 触发的 CI deploy 会因 Cloud SQL STOPPED 而 fail（无害，旧 revision `00050-nhk` 续命）；真上线需启 SQL + cost approval + workflow_dispatch 重跑。** 验证期（本地 dev 连 prod）修了 4 个 bug + 冷却：bug1 刷新练习闪引导页、bug2 AI 冷却 120s→3s、bug3 dev 后门 prod 404（预期，改用 Cloud SQL Studio 发 pass）、bug4 模考记录点击进只读逐题复盘（`?review=1` 不 resume）。**下一步（2026-06-04 起，详见 memory progress §39）：多考试扩展 — 第二考试 CA-C（加州 C 类/小汽车）已落地（V27 纯数据，mirror M1 的 8×2=16 sub-topic，参数同 M1 85%/30q）；多考试解耦审计（scoping/阈值/mock 模板全可复用，耦合只在 AI 文本层）+ 2 修已完成（`f1af66f` review-plan 读 exam 阈值不写死 85 / `58d00e3` AI explain+review-plan prompt exam-aware）。正在做 aiqgen 版权安全改造（临时抓 handbook + 章节锚点 + exam-aware prompt + 删 vendored M1 handbook，handbook 不入仓库避版权）→ 生成 CA-C 题库 ~120 题（DeepSeek ~$2-5，跑前批成本）→ CA-C mock 模板 → 用户本地测试。仅支持 CA。enhance1（AI 深入分析）已于更早会话完成。** 另"答对题 AI 聚合混淆点"同属 AI 增强一起做。**注意 study 页点击偶现 `chrome.runtime message channel` error 是浏览器扩展噪音，非应用 bug（代码零处用 chrome.runtime，无痕模式可证）。** Phase B 内容：sub-topic schema（V13-V15）+ AI 出题 pipeline（V16）+ 后端 sub-topic mastery / practice & mock history endpoints + 前端 Study Hub UI（重写 /dashboard、删 /review）+ AI explain 按钮（点击式，free/paid 分层）+ mock exam linear 重写（逐题只标对错、不可回退、答错超 15% 自动 terminate）+ Phase E 考后 AI 复习计划 + practice topic_filter + Mistakes "Practice these"。本会话续做：mock 复习计划改**自动异步生成**、错题页逐题复盘+AI 按钮、**practice session 限题（免费 15 / 付费 30，按 entry_type）**、**mock 倒计时+到点自动交卷（V21/V22，ended_by_timeout）**、mock 逐题复盘视图（?review=1 保留侧栏）、Study Hub history 各留 3 条。部署前两轮审计（spec/security/backend/web）全过：抓的 2 个 HIGH（免费题数 30→拆成 15/30、AI 解释分层文档过时）已闭环。**注意**：付费购买流程仍是 stub（用户钉定"先不做付费"，临时用 dev grant-pass 后门，已 `@Profile("!prod")` 双保险）；reminder 模块、AI 主动推荐 endpoint 仍未做（见 backlog，部署后最高优先级）。**部署 smoke：health UP + 公开端点真实 seed 不泄答案 + Flyway=V22 已确认；认证 smoke（free/paid practice 15/30、mock 计时）待用户用真实 Firebase token 或浏览器验证。验证完即停 Cloud SQL 省钱。**

**基础设施：**
- 后端：Java 21 + Spring Boot 3.4 + jOOQ + Flyway
- DB：PostgreSQL 16（本地 Docker 容器 `dmv-motor-postgres`，volume `dmv-motor-pgdata`）
- 测试：Testcontainers（静态容器，`IntegrationTestBase` + `TestFixtures`）
- 部署：GCP Cloud Run + Cloud SQL（Terraform，`infra/terraform/`）
- CI/CD：GitHub Actions（`.github/workflows/deploy.yml`）

**已完成（后端 MVP）：**
- [x] V1–V10 migrations（基础表 + 所有功能扩展）
- [x] jOOQ codegen 改为 `-Pjooq-gen` profile，生成源码归档到 `src/main/java/.../jooq/generated/`
- [x] Docker 本地 PostgreSQL（持久化 volume）
- [x] 测试基础设施（`IntegrationTestBase` + `TestFixtures` + `E2ETestBase`）
- [x] 统一响应格式 `ApiResponse<T>` + 全局异常处理（snake_case JSON）
- [x] 全部 MVP API 端点（117 单测，JaCoCo branch ≥90%；7 个 E2E IT 测试全绿）
- [x] V10：53 条真实 CA M1 题目（EN+ZH）+ CA_M1_30Q mock exam
- [x] GCP 基础设施上线（Terraform apply 成功，36 resources）
- [x] 学习周期隔离（soft reset，reset_count）
- [x] Free trial 隔离（allow_in_free_trial 字段）
- [x] **Round 2 纠偏**：ReadinessProperties 参数化；readiness 公式按 docs/parameters.md §7-§8 重写（2-mock avg 85%/key cov 90%/review 80%/持续薄弱点四道硬门槛；40/25/20/15 权重）；/summary 免费/付费分层；/mock-exams/access 401；review task 端点 canUseReview 纵深防御
- [x] **线上验证**：revision `dmv-motor-api-00004-cjt`（2026-04-21），`/actuator/health=UP`，`/api/v1/questions/1` 返真实 seed
- [x] Cloud SQL 已暂停（activation-policy=NEVER）省钱中
- [x] **MASTERY 判定升级 + wire-up 完成**（commits 3d52861 + b13c972 组件；2026-05-11 commit 5a79e07 真 wire-up）：`MasteryEvaluator` + `MasteryProperties`（`app.mastery.*`）+ `PracticeHistoryRepository`（双闸门：topic 正确率 ≥80% & 近 8 条 ≥6 道正确）。早先审计标 ORPHAN 部分有误——`isMastered()` 被 `ReviewService.completeTask` 调用，但原本只做 per-question 失活；`5a79e07` 用 `MistakeListRepository.deactivateForTopic` jOOQ bulk UPDATE 接通 topic-level 失活。Phase B 又加了 `SubTopicMasteryEvaluator`（`app.mastery.subtopic.*`，window=4/correct=3/rate≥0.80）驱动 Study Hub donut
- [x] **Firebase Auth 迁移 code-complete**（2026-04-23，commits 23e5223 + 323754f + 3e71694）：`FirebaseAuthVerifier` 接口 + `StubFirebaseVerifier`（dev/test 默认，`Bearer <numericUserId>` 走它）+ `FirebaseIdTokenVerifier`（prod，`app.auth.firebase.enabled=true` 开启）+ `UserProvisioner`（首次登录按 `firebase_uid` JIT 建号）+ V11 migration；`UserIdResolver` 注入 verifier+provisioner；Terraform 在 Cloud Run 注入 `APP_AUTH_FIREBASE_ENABLED=true`
- [x] **Firebase Auth 部署生产**（2026-04-25，revision `dmv-motor-api-00009-hf4`，commits ad2ac85 + 75c351f）：`terraform apply` 推 env 后两轮纠偏：`E2ETestBase.createTestUser` 套用 `TestFixtures` stamp 模式（`firebase_uid="test-<id>"`，原本 IT 没盖导致 JIT 新建 user 与断言不符）；`FirebaseConfig` 加 `@Value` + `setProjectId()` + `application-prod.yml` 默认 `${GOOGLE_CLOUD_PROJECT:dmv-motor-prod}`（Cloud Run ADC `ComputeEngineCredentials` 不带 projectId 且 `GOOGLE_CLOUD_PROJECT` env 不自动注入）。Smoke test 用真实 Firebase ID token 调 `/api/v1/me` 双调 200 + 同 user_id（id=1，幂等 ✅，email/uid 从 token 解码正确）。141 tests 全绿。
- [x] Cloud SQL 已重新暂停（activation-policy=NEVER）省钱中
- [x] **AI explain Phase A**（2026-05-08，commit cb33a1f）：V12 migration + `/api/v1/ai/explain` 端点 + `AiExplanationService`（cache 命中早返回 / 免费用户付费题 404 防 ID 枚举 / 思考时间 cooldown / 每日 50 cap）+ `StubAiExplanationProvider`（默认 dev/test 用）+ 15 IT；188 unit + 7 IT 全绿
- [x] **AI explain Phase B1**（2026-05-08 → 2026-05-11，commits 0bd0a37 + f4954ba + 3ea2f6e）：DeepSeek key 进 Secret Manager + Cloud Run env；`DeepSeekAiExplanationProvider`（双语 system prompt / RestClient + 30s timeout / 隐私契约严格不传 user_id / 失败统一 503 AI_PROVIDER_ERROR）+ 11 MockWebServer 测；`terraform apply` 推 `APP_AI_PROVIDER=deepseek` 到 prod；新 revision `dmv-motor-api-00018-2f2`；smoke test prod + 本地双绿（DeepSeek 真返 explanation + cache 命中 cached:true + cross-language 触发 180s cooldown 429）。**注意：仅后端，前端没有 AI 按钮**

**已完成（前端 MVP 第一波）：**
- [x] **Next.js 16 脚手架**（2026-04-26+，5 个 commit：ad83c2a → 597f060）：App Router + `[lang]/(marketing)/(app)` 分组、shadcn/ui、Tailwind、TanStack Query、`auth-context.tsx`（Firebase Web SDK + `getIdToken()` 注入）、`api-client.ts`（自动带 `Bearer <token>` + ApiError 解构 snake_case error envelope）、`messages/{en,zh}.json` i18n。⚠️ **Next.js 16 与训练数据有差异**：写代码前查阅 `node_modules/next/dist/docs/`（见 `apps/web/AGENTS.md`）
- [x] **Practice / Mistakes / Review / Mock 流程接通后端**（commits 40c903d / 619dc4a / 8397ed5 / 597f060）：登录 → 选 topic → 答题 → 看错题 → 走复习包 → 模考全链路可用；语言切换实时生效
- [x] **Lint/Build 基线**（2026-04-28，commit 7ee5606）：React 19 `react-hooks/set-state-in-effect` 规则下，effect 内部 setState 改用「render 期间用 tracked key 比较 + 一次性 setState」官方推荐 pattern；141 后端 tests + `npm run lint` + `npm run build`（19 静态页 + 2 动态路由）全绿

**已完成（Phase B Study Hub，2026-05-28，详见 memory/progress §31-§32）：**
- [x] **B0-B3 数据层**：vendor DMV M1 handbook（B0）+ 冻结 16 sub-topics（B1）+ V13/V14 sub_topics 表 + seed（B2）+ V15 retag 99 题 + NOT NULL（B3）
- [x] **B4 AI 出题 pipeline**：`aiqgen/` 模块（FormatValidator + DeepSeekChatClient + QuestionGenerator + 3 LLM judges + GenerationOrchestrator + CLI）；V16 灌 24 道 AI 题补 3 个薄弱 sub-topic；DeepSeek 成本 ~$0.15
- [x] **B5 后端 mastery + history endpoints**：`SubTopicMasteryEvaluator` + `GET /topics/mastery` + `/practice/sessions/{history,stats}` + `/mock-exams/attempts/{history,stats}` + `/me` 加 `in_progress_practice`
- [x] **B6 Study Hub UI**：重写 `/dashboard`（CoverageDonut 双弧 + ReadinessRing + Resume/Start card + practice/mock history + 手写 Sparkline）+ **删 `/review` 路由树**（后端 ReviewController/Service/Repo 标 `@Deprecated(forRemoval=true)` 保留）+ AI explain 按钮（点击式 / free 与 paid 分层 / AI 关闭返 `AI_UNAVAILABLE`）
- [x] **Mock exam linear 重写**（用户新规则）：逐题只标对错不解释、不可回退、手动 Next、答错超 `ceil(total*0.15)` 自动 `ended_by_failure`（V18）、refresh 可续考、focus mode 隐藏侧栏、V17 deactivate legacy 46-q mock
- [x] **Phase E 考后 AI 复习计划**：`POST /api/v1/ai/review-plan`（V19 缓存列 + Stub/DeepSeek provider + 归属/状态/enabled 闸门，点击式防 hijack）
- [x] **审计纠偏**（commit 67b1ad4）：practice `topic_filter`（V20，server cap 8）+ Mistakes "Practice these" CTA + `@Deprecated` review 模块 + design doc 决策 #2 rubric 更新
- [x] **基线**：335 unit + 7 IT 全绿，JaCoCo branch 90.47%；security clean（1 LOW）

**Backlog — Phase B 期间识别但未做（按 severity）：**

| Sev | 项 | 来源 | 状态 |
|---|---|---|---|
| HIGH | 付费 access pass 真实购买流程 | 用户钉定"先不做付费" | /me 全 Coming soon stub；临时用 dev grant-pass 后门 |
| ~~HIGH~~ ✅ | ~~`reminder/` 模块~~ | `features.md §2` + `reminder-and-readiness.md` MVP 必备 | **完成（2026-05-30，commit `fbf00da`，本地）**：V24 `reminder_tasks` + domain/repo/service/controller。后端按学习状态生成（resume>weak-points>mock 优先序）+ 每天≤1 + 3 次未响应暂停该类回退；`GET /reminders`(站内 list) + `POST /reminders/generate`(幂等) + `POST /reminders/{id}/respond`。12 IT，379 tests 绿。Scope：**后端 only**（前端站内 UI 待做）；无 cron（generate 显式/lazy）；同类冷却由 1/day 包含 |
| ~~HIGH~~ ✅ | ~~AI 主动推荐 endpoint~~ | `mvp.md §5 功能 10`："推荐下一轮强化题/复习方向" | **完成（2026-05-31，commit `d1182cc`，本地）**：`GET /api/v1/ai/recommendations` 确定性排序（active mistakes topic 计数优先 > 未覆盖 key topic）+ reason_code + topic_filter 一键开 practice。`RecommendationRepository/Service/Controller`。7 IT，386 tests 绿。LLM 文案层留 seam 后补（§34-B）；free/paid 分层未做（暂全登录用户）；前端未接 |
| MED | mastery 第 3 闸门（混淆点） | `parameters.md §6` 第 3 条 | 等 questions 加 confusion_tag schema；`TODO(FUTURE_CONFUSION_SCHEMA)` |
| MED | `api-contract.md §16` mock UX 过期 | 本会话用户改了 mock 规则 | 文档还写旧"逐题对错统计"，需更新为 linear/auto-terminate/考后 AI |
| MED | `/api/v1/access` 路径不一致 | `api-contract.md §12` 写 `/access` | 实际 `/me/access` |
| LOW | aiqgen stem 去重 | audit #4 | Q-gen 没查重复题干 |
| LOW | DevController `@Profile("!prod")` 双保险 | security audit LOW | 当前只靠 `app.dev.endpoints` flag |
| LOW | `/summary` `pace` 字段 + `next_action` 具体文案 | `parameters.md §9` / `reminder-and-readiness §13` | |
| LOW | `memoryexport/` 模块 | `features.md §2` | 空目录，MVP 价值低，暂搁 |
| LOW | `mvnw` wrapper | — | 目前用本地 mvn |

**NOT-DO（用户曾期望但 spec 没要求 → 不做）：**
- `/study` 独立页面（`mvp.md §6` 没列；Study Hub 即 `/dashboard`）
- 显式 "study plan" 实体（spec 用 pace + next_action + review pack 替代）

**【2026-06-01 会话3 — 多考试地基 ✅ 本地完成，2 commit 未 push】** 新需求：不强调加州，扩展到别的州 + 别的驾照类型。本批只搭地基（schema + 后端 scoping + 前端选择器），CA-M1 仍唯一 seed，行为不变；落地页文案 held。后端 `ea525b4`：V26 `exams`(state×license, 自带 pass_threshold) + 给 topics/questions/mock_exams/practice_sessions/mock_attempts 加 exam_id(NOT NULL backfill) + users.current_exam_id(nullable)，全 dynamic jOOQ ref 不 regen；`ExamContext` 统一解析当前考试；practice/mock snapshot exam_id + pool/模板按它 scope（跨考试隔离）；`GET /exams`、`PUT /me/exam`、`/me.current_exam`；`/topics`/donut/recommendations 按考试 scope；`MockScoringPolicy` 阈值从 exam 读（不再硬编码 0.85）。前端 `ad490db`：useExams + useMe.current_exam + `ExamPicker`（PUT /me/exam→全 invalidate）+ MeView Exam section + Dashboard onboarding 卡 + sidebar 显示考试名 + de-brand authed shell（appBrand→"DMV Prep" / appTagline→"DMV written-exam prep" / metadata 中性，落地页 home.* 未动）。409 tests 绿、JaCoCo≥0.90、lint+build clean。本地 backend 已重启到 V26（`/exams` 返 CA-M1 en+zh）。Plan `~/.claude/plans/cosmic-bouncing-mochi.md`，详见 progress §38。

**下一阶段：** ①把累积的本地改动一次性部署 prod（启 Cloud SQL + cost approval + smoke test）——含 dev-audit / enhance1 / reminder（已做）/ AI 推荐 endpoint（已做）/ 多考试地基（已做）。②等用户定扩张优先级：别的州/驾照类型的真题内容 + 落地页重定位文案（held）。

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
