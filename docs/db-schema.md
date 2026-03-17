# 数据库 Schema

## 1. 目标

这份文档定义 MVP 阶段建议采用的数据库逻辑 schema，回答这些问题：

- 哪些核心对象应该落成独立表
- 表之间如何关联
- 哪些字段是主状态
- 哪些内容适合快照存储

这份文档仍然不定义：

- 最终数据库产品选型
- 索引优化细节
- 分库分表
- 读写分离
- 迁移脚本

## 2. 当前数据库层设计原则

### 原则 1：业务表统一关联内部 `user_id`

认证建议交给第三方服务。  
数据库里所有学习、访问、复习、mock exam 相关数据都只关联内部用户主键。

这意味着：

- 不把业务表直接绑在第三方 provider id 上
- 后续更换认证服务不会破坏学习数据

### 原则 2：双语题目建成“题目本体 + 语言版本”

不能为中英文各建一套独立题库。  
应拆成：

- `questions`
- `question_variants`

### 原则 3：行为记录与汇总状态分开

例如：

- 原始作答记录单独存
- 错题汇总单独存
- readiness / progress 快照单独存

这样更利于审计、重算和回放。

### 原则 4：mock exam 单独建模

mock exam 不和普通练习共用同一条会话主表。  
因为它有更强状态约束和配额逻辑。

## 3. 建议的数据库实体总览

当前建议至少包含以下表：

1. `users`
2. `user_auth_identities`
3. `access_passes`
4. `topics`
5. `questions`
6. `question_variants`
7. `question_related_topics`
8. `practice_sessions`
9. `practice_attempts`
10. `mistake_records`
11. `review_tasks`
12. `review_packs`
13. `review_pack_tasks`
14. `mock_exams`
15. `mock_exam_questions`
16. `mock_attempts`
17. `mock_attempt_results`
18. `progress_snapshots`
19. `readiness_snapshots`
20. `memory_exports`

## 4. 用户与认证相关表

### `users`

作用：

- 保存产品内用户主身份

建议字段方向：

- `id`
- `email` 或可选联系字段
- `language_preference`
- `current_access_state`
- `created_at`
- `updated_at`

说明：

- `language_preference` 可以先放在 `users`
- 更复杂的偏好设置后续再拆独立表
- `current_access_state` 是 `access_passes` 的冗余快照，用于快速鉴权；实现时必须在 access pass 激活、到期、续费时同步更新此字段，否则会产生鉴权漏洞

### `user_auth_identities`

作用：

- 记录用户与第三方认证身份的映射

建议字段方向：

- `id`
- `user_id`
- `provider`
- `provider_user_id`
- `provider_email`
- `created_at`
- `updated_at`

约束建议：

- `provider + provider_user_id` 唯一
- 一个用户可绑定一个或多个外部身份

## 5. 访问权限相关表

### `access_passes`

作用：

- 记录 30 天访问期和 mock exam 配额

建议字段方向：

- `id`
- `user_id`
- `status`
- `starts_at`
- `expires_at`
- `mock_exam_total_count`
- `mock_exam_used_count`
- `created_at`
- `updated_at`

状态建议：

- `inactive`
- `active`
- `expired`
- `consumed_out`

说明：

- 当前阶段不需要把价格、订单等支付细节混进这张表
- 支付表可以在后续商业实现层再引入

## 6. 内容相关表

### `topics`

作用：

- 保存 topic 树和关键标记

建议字段方向：

- `id`
- `parent_topic_id`
- `code`
- `name_en`
- `name_zh`
- `is_key_topic`
- `risk_level`
- `sort_order`
- `created_at`
- `updated_at`

### `questions`

作用：

- 保存题目本体和规则属性

建议字段方向：

- `id`
- `primary_topic_id`
- `correct_choice_key`
- `learning_level`
- `difficulty_level`
- `risk_flag`
- `is_key_coverage`
- `allow_in_practice`
- `allow_in_review`
- `allow_in_mock_exam`
- `status`
- `created_at`
- `updated_at`

说明：

- `correct_choice_key` 应属于题目本体，不属于某个语言版本
- 中英文共享同一正确答案和同一学习归属

### `question_variants`

作用：

- 保存题目在不同语言下的文本版本

建议字段方向：

- `id`
- `question_id`
- `language_code`
- `stem_text`
- `choice_a_text`
- `choice_b_text`
- `choice_c_text`
- `choice_d_text`
- `explanation_text`
- `status`
- `created_at`
- `updated_at`

约束建议：

- `question_id + language_code` 唯一

### `question_related_topics`

作用：

- 保存题目与相关 topic 的非主归属关系

建议字段方向：

- `id`
- `question_id`
- `topic_id`
- `relation_type`

说明：

- 这张表解决“主 topic 之外的相关 topic”
- 不影响 `questions.primary_topic_id` 的主归属

## 7. 普通练习相关表

### `practice_sessions`

作用：

- 保存一轮普通练习的会话状态

建议字段方向：

- `id`
- `user_id`
- `status`
- `entry_type`
- `language_code`
- `started_at`
- `completed_at`
- `last_active_at`
- `resume_token` 或等价恢复标记
- `created_at`
- `updated_at`

状态建议：

- `in_progress`
- `completed`
- `abandoned`

### `practice_attempts`

作用：

- 保存单题作答记录

建议字段方向：

- `id`
- `user_id`
- `practice_session_id`
- `question_id`
- `question_variant_id`
- `entry_source`
- `selected_choice_key`
- `is_correct`
- `submitted_at`
- `created_at`

说明：

- 这里可承载普通练习和复习作答，只要 `entry_source` 可区分
- 但 mock exam 仍建议单独落表

## 8. 错题与复习相关表

### `mistake_records`

作用：

- 保存用户在某道题上的错误沉淀状态

建议字段方向：

- `id`
- `user_id`
- `question_id`
- `primary_topic_id`
- `first_wrong_at`
- `last_wrong_at`
- `wrong_count`
- `last_entry_source`
- `is_active`
- `created_at`
- `updated_at`

约束建议：

- `user_id + question_id` 唯一

### `review_tasks`

作用：

- 保存系统生成的复习任务

建议字段方向：

- `id`
- `user_id`
- `primary_topic_id`
- `task_type`
- `target_learning_level`
- `priority_score`
- `status`
- `source_reason`
- `generated_at`
- `started_at`
- `completed_at`
- `created_at`
- `updated_at`

状态建议：

- `todo`
- `in_progress`
- `completed`
- `expired`
- `replaced`

### `review_packs`

作用：

- 保存每日复习包

建议字段方向：

- `id`
- `user_id`
- `pack_date`
- `language_code`
- `target_question_count`
- `completed_question_count`
- `status`
- `generated_at`
- `started_at`
- `completed_at`
- `created_at`
- `updated_at`

说明：

- `language_code` 在 pack 生成时继承用户当前语言偏好，后续投放题目的语言版本（`question_variants`）以此为准
- 用户中途切换语言偏好不影响已生成 pack 的语言，下一轮生成时才会使用新偏好

### `review_pack_tasks`

作用：

- 建立复习包和复习任务之间的关联

建议字段方向：

- `id`
- `review_pack_id`
- `review_task_id`
- `sort_order`

约束建议：

- `review_pack_id + review_task_id` 唯一

## 9. mock exam 相关表

### `mock_exams`

作用：

- 保存可投放的 mock exam 模板

建议字段方向：

- `id`
- `code`
- `version`
- `status`
- `question_count`
- `created_at`
- `updated_at`

### `mock_exam_questions`

作用：

- 保存试卷和题目之间的关联

建议字段方向：

- `id`
- `mock_exam_id`
- `question_id`
- `sort_order`

### `mock_attempts`

作用：

- 保存用户参加一次 mock exam 的整体结果

建议字段方向：

- `id`
- `user_id`
- `access_pass_id`
- `mock_exam_id`
- `status`
- `language_code`
- `started_at`
- `ended_at`
- `ended_by_exit`
- `score_percent`
- `correct_count`
- `wrong_count`
- `created_at`
- `updated_at`

状态建议：

- `in_progress`
- `submitted`
- `ended_by_exit`
- `expired`

### `mock_attempt_results`

作用：

- 保存 mock exam 中每道题的作答明细

建议字段方向：

- `id`
- `mock_attempt_id`
- `question_id`
- `question_variant_id`
- `selected_choice_key`
- `is_correct`
- `topic_id`
- `submitted_at`

说明：

- 这张表可支持赛后错题分析和 topic 薄弱点总结

## 10. 快照相关表

### `progress_snapshots`

作用：

- 保存某一时刻的学习进度总结

建议字段方向：

- `id`
- `user_id`
- `completion_score`
- `key_topic_coverage_score`
- `review_completion_score`
- `next_action_type`
- `summary_payload`
- `created_at`

说明：

- `summary_payload` 可以先承载结构化摘要
- 不必在 MVP 阶段把所有 summary 细项都拆列

### `readiness_snapshots`

作用：

- 保存某一时刻的 readiness 结果

建议字段方向：

- `id`
- `user_id`
- `readiness_score`
- `is_ready_candidate`
- `missing_gate_codes`
- `explanation_payload`
- `created_at`

说明：

- `missing_gate_codes` 可先用轻量结构存储
- readiness 的判定来源仍应来自规则计算，而不是手工编辑

## 11. 导出相关表

### `memory_exports`

作用：

- 保存学习记忆导出任务

建议字段方向：

- `id`
- `user_id`
- `status`
- `is_encrypted`
- `is_device_bound`
- `export_scope`
- `file_ref`
- `created_at`
- `ready_at`
- `expired_at`

状态建议：

- `pending`
- `ready`
- `failed`
- `expired`

## 12. 推荐的主外键关系

当前建议主关系如下：

- `user_auth_identities.user_id -> users.id`
- `access_passes.user_id -> users.id`
- `topics.parent_topic_id -> topics.id`
- `questions.primary_topic_id -> topics.id`
- `question_variants.question_id -> questions.id`
- `question_related_topics.question_id -> questions.id`
- `question_related_topics.topic_id -> topics.id`
- `practice_sessions.user_id -> users.id`
- `practice_attempts.user_id -> users.id`
- `practice_attempts.practice_session_id -> practice_sessions.id`
- `practice_attempts.question_id -> questions.id`
- `practice_attempts.question_variant_id -> question_variants.id`
- `mistake_records.user_id -> users.id`
- `mistake_records.question_id -> questions.id`
- `mistake_records.primary_topic_id -> topics.id`
- `review_tasks.user_id -> users.id`
- `review_tasks.primary_topic_id -> topics.id`
- `review_packs.user_id -> users.id`
- `review_pack_tasks.review_pack_id -> review_packs.id`
- `review_pack_tasks.review_task_id -> review_tasks.id`
- `mock_exam_questions.mock_exam_id -> mock_exams.id`
- `mock_exam_questions.question_id -> questions.id`
- `mock_attempts.user_id -> users.id`
- `mock_attempts.access_pass_id -> access_passes.id`
- `mock_attempts.mock_exam_id -> mock_exams.id`
- `mock_attempt_results.mock_attempt_id -> mock_attempts.id`
- `mock_attempt_results.question_id -> questions.id`
- `mock_attempt_results.question_variant_id -> question_variants.id`
- `progress_snapshots.user_id -> users.id`
- `readiness_snapshots.user_id -> users.id`
- `memory_exports.user_id -> users.id`

## 13. 哪些字段适合结构化 JSON / payload

为了避免 MVP 阶段过度拆表，当前有些内容可以先用结构化 payload 存：

- `progress_snapshots.summary_payload`
- `readiness_snapshots.explanation_payload`
- `readiness_snapshots.missing_gate_codes`

但以下内容不建议偷懒塞进 JSON：

- 用户主身份
- 题目主归属
- 正确答案
- mock exam 配额
- 复习任务状态
- 会话状态

这些都属于强业务字段，应保持显式列。

## 14. 哪些内容先不进入数据库主 schema

当前建议先不进入主 schema 的内容包括：

- 支付订单明细
- 发票或退款表
- 复杂的审计日志系统
- AI prompt / response 原文归档
- 通知发送明细全量表

原因是：

- 这些内容当前不是 MVP 主闭环的阻塞项
- 过早引入会拉高 schema 复杂度

## 15. 当前数据库建议结论

当前最适合的数据库层方向是：

- 以关系型数据库为主
- 用清晰主外键承载核心业务对象
- 少量 summary / explanation 内容可先用 payload

原因是这套产品的核心问题本质上是：

- 用户
- 访问期
- 题目
- 作答
- 复习任务
- mock exam
- 快照

它们天然更适合关系型建模。

## 16. 当前不在这份文档里定义的内容

这份文档暂时不定义：

- PostgreSQL 还是 MySQL
- 索引清单
- 分页查询 SQL
- 分区策略
- 冷热数据拆分
- 归档策略
- 读模型和写模型是否拆开

这些内容要放到后续技术架构文档里再定。
