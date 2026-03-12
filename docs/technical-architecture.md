# 技术架构设计

## 状态

- 当前状态：进行中
- 推进方式：问答式逐步定稿
- 当前问题：系统整体形态是单体应用，还是拆成多个服务

## 已确认结论

- 系统整体形态采用“模块化单体”
- 当前不从一开始拆成多服务
- 主要原因是初期 QPS 不高，优先降低复杂度，同时保留未来按模块拆分的边界
- 前后端分离
- 后端 API 需要同时服务 Web 和 App
- 后端应作为统一业务能力入口，避免 Web 和 App 各自维护不同业务规则
- 后端按以下主模块划分：
- `auth-access`
- `content`
- `practice`
- `mistake-review`
- `mock-exam`
- `progress-readiness`
- `reminder`
- `ai-support`
- `memory-export`
- 同步执行的主流程包括：
- 登录 / 鉴权
- 拉题
- 提交单题答案并即时判分
- 记录本次作答
- mock exam 配额消耗与 attempt 创建
- mock exam 交卷并返回真实分数
- 查询当前 review pack
- 查询当前 summary / readiness 快照
- 异步执行的主流程包括：
- review task / review pack 刷新
- progress snapshot / readiness snapshot 刷新
- reminder task 生成
- AI 简短解释与 AI 推荐生成
- memory export 生成与加密
- 聚合统计刷新
- 长期主状态实时写入，包括：
- 用户 / access pass / mock 配额
- topic / question / question variant
- practice session / attempt
- mistake record
- review task / review pack
- mock attempt 与每题结果
- memory export 记录
- 快照包括：
- progress snapshot
- readiness snapshot
- summary 页所需的聚合结果
- mock 后的薄弱点总结
- 实时计算或按需重算的内容包括：
- topic 覆盖率细节
- slow / ok / fast 节奏分层
- 某些页面临时排序结果
- AI 文案缓存命中状态
- `reminder` 作为独立内部子系统设计
- `ai-support` 作为独立内部子系统设计
- 两者都需要独立考虑第三方依赖、成本控制、失败降级和优化策略
- 判题采用“单题提交一次 request，同步返回结果”的模式
- 判题只依赖题目正确答案等内容事实，不依赖用户完整历史
- 用户历史状态在判题完成后更新，而不是在判题前拼装整段历史
- 错题记录属于学习历史的一部分，但它是沉淀后的薄弱点状态，不等于原始作答流水

## 待确认问题

- 当前轮次已完成，下一步进入 `review-and-readiness-engine`

## 决策记录

- 决策：系统整体形态选择模块化单体
- 原因：初期 QPS 不高，不值得过早引入多服务带来的部署、调用、观测和数据一致性复杂度
- 约束：模块边界需要提前定义清楚，后续可以按模块拆分
- 决策：前后端分离
- 原因：后端 API 需要同时服务 Web 和 App，统一承载账号、练习、复习、mock、readiness、reminder、AI 等业务能力
- 约束：客户端只负责展示和交互，不应各自实现一套核心业务规则
- 决策：后端按业务能力切分为 `auth-access`、`content`、`practice`、`mistake-review`、`mock-exam`、`progress-readiness`、`reminder`、`ai-support`、`memory-export`
- 原因：这些模块基本对应已确认的产品状态边界，后续如果拆服务，也有自然切分点
- 决策：把必须立刻影响当前用户交互结果的链路做成同步流程
- 决策：把可延迟收敛的任务、快照、提醒、AI、导出做成异步流程
- 原因：既保证练习与 mock 的即时反馈，又避免把 review/readiness/reminder/AI 的复杂计算压进核心请求链路
- 决策：把权限、题目、作答、错题、复习任务、mock 结果等作为长期主状态实时写入
- 决策：把 progress / readiness / summary / mock 后薄弱点总结作为快照
- 决策：把覆盖率细节、节奏分层、临时排序等作为可实时计算或重建的衍生状态
- 原因：区分事实、阶段结论和可推导细节，避免状态重复维护和结果冲突
- 决策：`reminder` 与 `ai-support` 作为模块化单体中的独立内部子系统
- 原因：两者都依赖独立第三方服务，且直接涉及成本，需要单独处理缓存、配额、重试、降级与优化
- 决策：判题按单题提交独立 request 的方式处理
- 原因：判题必须快、稳定、低成本，不能依赖拼装用户完整历史
- 决策：判题后同步写 attempt，后续异步更新 mistake/review/progress/readiness/reminder
- 原因：把核心交互链路和复杂学习状态收敛链路解耦

## 客户端职责边界

- Web 与 App 负责登录态持有、页面展示、交互流程、本地轻量缓存、失败提示和重试
- 后端统一负责判题、分数计算、review task / review pack 生成、readiness / completion 判断、mock 配额校验、reminder task 生成、AI 调用与导出约束
- 客户端不维护独立 readiness 规则，不自行决定 review pack，不自行扣减 mock 配额，不把 AI 作为主流程判定器
