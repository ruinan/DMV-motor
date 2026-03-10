# 数据模型设计

本文默认使用 Firestore。为了支持查询效率，可以把部分聚合字段冗余到文档中，而不是追求完全范式化。

## 设计原则

- `question` 是内容主表，尽量稳定
- `user_attempt` 是最核心行为数据
- `weak_topic_profile` 是规则引擎和 AI 联合产物
- `review_queue` 是复习调度核心
- `mock_exam_session` 用于判断用户整体稳定性
- `ai_feedback` 只保存有价值的结构化输出，不保存无用长文本

## 1. `question`

集合：`questions`

| 字段 | 类型 | 含义 | 为什么需要 |
| --- | --- | --- | --- |
| `id` | string | 题目 ID | 供答题、复习、错题引用 |
| `status` | string | `draft` / `active` / `retired` | 控制题目是否可投放 |
| `topic_code` | string | 主知识点，如 `right_of_way` | 用于弱点聚类 |
| `subtopic_code` | string | 子知识点，如 `4_way_stop` | 支持更细粒度强化 |
| `question_type` | string | `rule_fact` / `scenario_best_action` / `legality` / `sign` | 判断用户错在何种题型 |
| `difficulty` | int | 1-5 难度 | 便于递进练习 |
| `stem` | string | 题干 | 用户看到的问题主体 |
| `option_ids` | string[] | 选项 ID 列表 | 适配独立选项结构 |
| `correct_option_id` | string | 正确选项 ID | 判分需要 |
| `explanation_short` | string | 简短规则解释 | 即时反馈需要 |
| `explanation_deep` | string | 详细解析 | 错题复盘和 AI 输入需要 |
| `trap_type` | string | `similar_rule` / `common_misread` / `plausible_but_wrong` 等 | 识别干扰项模式 |
| `source_refs` | string[] | 来源主题引用，如 handbook section code | 用于内容追溯，而不是前台展示 |
| `tags` | string[] | 补充标签 | 支持筛选和编排 |
| `version` | int | 内容版本 | 后续修订题目时追踪 |
| `created_at` | timestamp | 创建时间 | 内容管理需要 |
| `updated_at` | timestamp | 更新时间 | 内容管理需要 |

## 2. `question_option`

集合：`question_options`

| 字段 | 类型 | 含义 | 为什么需要 |
| --- | --- | --- | --- |
| `id` | string | 选项 ID | 唯一标识 |
| `question_id` | string | 所属题目 | 关联题目 |
| `label` | string | `A` / `B` / `C` / `D` | 前端显示 |
| `text` | string | 选项文案 | 用户作答内容 |
| `is_correct` | boolean | 是否正确 | 允许内容检查和回放 |
| `option_role` | string | `correct` / `trap` | 识别干扰项特征 |
| `trap_reason` | string | 干扰项设计原因 | AI 和内容校验需要 |

为什么拆表：便于后续做干扰项分析、相似选项统计、内容编辑。  
如果 MVP 想先简单，也可以把 options 直接内嵌进 `question` 文档。

## 3. `user_profile`

集合：`user_profiles`

| 字段 | 类型 | 含义 | 为什么需要 |
| --- | --- | --- | --- |
| `user_id` | string | 用户 ID，对应 Firebase UID | 主键 |
| `display_name` | string | 用户昵称 | 基础信息 |
| `email` | string | 登录邮箱 | 基础信息 |
| `exam_goal_date` | date | 计划考试日期 | 决定复习强度 |
| `target_state` | string | `CA` | 暂时固定为 California |
| `license_goal` | string | `M1` | 明确备考目标 |
| `onboarding_level` | string | `new` / `some_practice` / `retake` | 初始编排依据 |
| `study_streak_days` | int | 连续学习天数 | 激励和节奏 |
| `last_active_at` | timestamp | 最近活跃时间 | Dashboard 展示 |
| `mastery_score` | map<string, number> | 每个 topic 的掌握度 0-100 | 首页和推荐需要 |
| `readiness_score` | int | 综合上考场准备度 0-100 | 用户最关心指标 |
| `created_at` | timestamp | 创建时间 | 审计需要 |
| `updated_at` | timestamp | 更新时间 | 审计需要 |

## 4. `user_attempt`

集合：`user_attempts`

| 字段 | 类型 | 含义 | 为什么需要 |
| --- | --- | --- | --- |
| `id` | string | 作答记录 ID | 主键 |
| `user_id` | string | 用户 ID | 归属用户 |
| `question_id` | string | 题目 ID | 关联题目 |
| `selected_option_id` | string | 用户所选答案 | 判分和分析需要 |
| `is_correct` | boolean | 是否答对 | 核心行为结果 |
| `attempt_mode` | string | `practice` / `review` / `mock_exam` | 区分学习场景 |
| `topic_code` | string | 题目主知识点冗余 | 快速查询 |
| `subtopic_code` | string | 子知识点冗余 | 快速查询 |
| `question_type` | string | 题型冗余 | 分析错因 |
| `trap_type` | string | 干扰项类型冗余 | 分析被什么误导 |
| `response_time_ms` | int | 作答耗时 | 识别是否“不确定但蒙对” |
| `confidence_level` | int | 用户自评信心 1-3，可选 | 辅助判断真实掌握度 |
| `session_id` | string | 所属练习或考试会话 | 聚合需要 |
| `answered_at` | timestamp | 作答时间 | 复习调度需要 |

## 5. `weak_topic_profile`

集合：`weak_topic_profiles`

建议主键：`{user_id}_{topic_code}`

| 字段 | 类型 | 含义 | 为什么需要 |
| --- | --- | --- | --- |
| `id` | string | 文档 ID | 主键 |
| `user_id` | string | 用户 ID | 归属用户 |
| `topic_code` | string | 主知识点 | 聚合维度 |
| `subtopic_code` | string | 可选子知识点 | 更精细定位 |
| `weak_score` | int | 薄弱程度 0-100 | 排序和优先级 |
| `mastery_score` | int | 当前掌握度 0-100 | 用户展示和推荐 |
| `recent_attempts` | int | 最近窗口内作答数 | 判断样本量 |
| `recent_wrong_count` | int | 最近窗口内错误数 | 识别持续问题 |
| `consecutive_wrong_count` | int | 连续错误次数 | 高优先级强化信号 |
| `mistake_pattern` | string[] | 如 `rule_memory`, `similar_rule_confusion`, `question_intent_misread` | 系统需要知道“怎么错” |
| `high_risk_reason` | string | 简短说明 | 前台展示 |
| `last_wrong_at` | timestamp | 最近错误时间 | 复习编排 |
| `last_reviewed_at` | timestamp | 最近复习时间 | 避免过度重复 |
| `recommended_action` | string | `repeat_rules`, `contrast_drill`, `scenario_drill`, `mixed_review` | 驱动复习策略 |
| `updated_at` | timestamp | 更新时间 | 审计需要 |

## 6. `review_queue`

集合：`review_queue`

| 字段 | 类型 | 含义 | 为什么需要 |
| --- | --- | --- | --- |
| `id` | string | 队列项 ID | 主键 |
| `user_id` | string | 用户 ID | 归属用户 |
| `queue_type` | string | `wrong_question`, `weak_topic_drill`, `contrast_set`, `mock_followup` | 区分复习来源 |
| `question_id` | string | 单题复习时使用 | 错题重做 |
| `topic_code` | string | topic 复习时使用 | 题组生成 |
| `subtopic_code` | string | 子 topic 复习时使用 | 题组生成 |
| `priority` | int | 1-100 | 决定今日包排序 |
| `scheduled_for` | timestamp | 计划复习时间 | 每日复习包生成 |
| `status` | string | `pending` / `done` / `skipped` / `expired` | 队列生命周期 |
| `source_attempt_id` | string | 来源作答 | 回溯原因 |
| `strategy_code` | string | `same_question_retry`, `same_topic_new_question`, `contrast_pair`, `timed_drill` | 执行复习策略 |
| `reason_code` | string | `two_consecutive_wrong`, `mock_exam_weakness`, `high_confusion_topic` | 解释为什么进入队列 |
| `created_at` | timestamp | 创建时间 | 审计需要 |
| `completed_at` | timestamp | 完成时间 | 统计需要 |

## 7. `mock_exam_session`

集合：`mock_exam_sessions`

| 字段 | 类型 | 含义 | 为什么需要 |
| --- | --- | --- | --- |
| `id` | string | 模拟考试 ID | 主键 |
| `user_id` | string | 用户 ID | 归属用户 |
| `status` | string | `in_progress` / `submitted` | 会话状态 |
| `question_ids` | string[] | 本次题目列表 | 保证考试稳定 |
| `total_questions` | int | 总题数 | 前台展示 |
| `correct_count` | int | 正确数 | 成绩核心 |
| `wrong_count` | int | 错误数 | 成绩核心 |
| `score_percent` | int | 百分制分数 | 用户易理解 |
| `pass_estimate` | boolean | 按规则估算是否达标 | 给出考试 readiness |
| `weak_topics` | string[] | 本次暴露的薄弱点 | 结果总结 |
| `started_at` | timestamp | 开始时间 | 记录考试行为 |
| `submitted_at` | timestamp | 提交时间 | 记录考试行为 |

## 8. `ai_feedback`

集合：`ai_feedback`

| 字段 | 类型 | 含义 | 为什么需要 |
| --- | --- | --- | --- |
| `id` | string | AI 输出 ID | 主键 |
| `user_id` | string | 用户 ID | 归属用户 |
| `feedback_type` | string | `attempt_explanation`, `topic_summary`, `mock_exam_summary`, `next_drill_recommendation` | 控制使用场景 |
| `related_attempt_id` | string | 相关作答记录 | 单题解释需要 |
| `related_session_id` | string | 相关练习或模拟考试 | 总结需要 |
| `related_topic_code` | string | 相关知识点 | 主题总结需要 |
| `mistake_reason_code` | string | `rule_not_memorized`, `similar_rule_confusion`, `question_intent_misread`, `distractor_bias` | 结构化错因 |
| `explanation_short` | string | 1-3 句短解释 | 前台直接展示 |
| `recommended_strategy_code` | string | `repeat_rule`, `contrast_questions`, `scenario_repeat`, `timed_retry` | 驱动下一步行为 |
| `confidence_score` | number | 0-1 | 控制是否直接展示 |
| `model_name` | string | 使用的模型名 | 追踪质量和成本 |
| `prompt_version` | string | Prompt 版本 | 调试需要 |
| `created_at` | timestamp | 创建时间 | 审计需要 |

## 9. 推荐索引与查询

Firestore 常见查询建议：

- `user_attempts`: `user_id + answered_at desc`
- `user_attempts`: `user_id + topic_code + answered_at desc`
- `review_queue`: `user_id + status + scheduled_for asc`
- `weak_topic_profiles`: `user_id + weak_score desc`
- `mock_exam_sessions`: `user_id + submitted_at desc`

## 10. 为什么这套模型够用

这套数据模型已经覆盖 MVP 的关键闭环：

- `question` 提供内容
- `user_attempt` 记录行为
- `weak_topic_profile` 识别问题
- `review_queue` 安排复习
- `mock_exam_session` 验证 readiness
- `ai_feedback` 做可解释增强

它不追求复杂 BI 建模，但已经足够支撑第一版上线和后续迭代。
