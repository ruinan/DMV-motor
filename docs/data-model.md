# 数据模型

## 1. 目标

这份文档定义产品层的数据模型，回答这些问题：

- 系统里有哪些核心实体
- 这些实体之间是什么关系
- 哪些状态必须被长期保存
- 哪些状态可以按需计算

这份文档不定义：

- 具体数据库类型
- 具体表结构
- 索引设计
- ORM 设计
- 接口字段命名

## 2. 数据模型的边界

当前阶段的数据模型目标不是“把数据库设计完”，而是验证前面的产品规则能否被一套稳定对象承载。

因此当前只做三件事：

1. 定义核心实体
2. 定义实体关系
3. 定义关键状态流转

## 3. 建模原则

### 原则 1：同一条产品规则只能有一套主状态来源

例如：

- 真实分数来自作答结果
- readiness 来自独立判断结果
- 访问权限来自 access pass

不能让同一个产品规则在多处各自维护一份主状态。

### 原则 2：双语是表达层，不是两套内容系统

中英双语必须建模为同一套学习对象的不同语言表达。  
不能做成两套互不相干的题库和学习记录。

### 原则 3：题目、学习记录、复习任务要分开

题目是内容对象。  
学习记录是用户行为对象。  
复习任务是系统组织出来的行动对象。

这三者不能混成一个实体。

### 原则 4：快照和实时计算要分开

有些内容适合长期保存快照，例如：

- mock exam 结果
- 某一时刻的 readiness summary

有些内容适合按需重算，例如：

- 当前某个 topic 的实时覆盖率
- 当前推荐速度分层

## 4. 核心实体总览

当前产品至少需要以下核心实体：

1. User
2. AccessPass
3. LanguagePreference
4. Topic
5. Question
6. QuestionVariant
7. PracticeSession
8. PracticeAttempt
9. MistakeRecord
10. ReviewTask
11. ReviewPack
12. MockExam
13. MockAttempt
14. ProgressSnapshot
15. ReadinessSnapshot
16. MemoryExport

## 5. 用户与访问相关实体

### User

表示一个学习用户。

至少承载这些长期状态：

- 账户身份
- 当前学习主状态
- 当前语言偏好

说明：

- 不在 User 上存储访问期状态快照；是否处于有效访问期通过实时查询 `AccessPass` 判断，避免冗余字段带来的状态不一致

### AccessPass

表示一次付费访问资格。

它至少需要表达：

- 属于哪个用户
- 生效时间
- 失效时间
- 对应的 mock exam 配额
- 当前剩余 mock exam 次数
- 当前状态

当前状态至少应包括：

- `inactive`
- `active`
- `expired`
- `consumed_out` 或等价状态

### LanguagePreference

表示用户当前语言偏好设置。

当前产品层需要支持：

- 默认语言来源
- 用户手动切换后的最终选择

这个实体也可以在后续物理设计里并入 User。  
但在产品模型里需要单独明确这类状态存在。

## 6. 内容相关实体

### Topic

表示稳定的知识范围。

至少需要表达：

- topic 标识
- topic 名称
- 是否为关键 topic
- topic 的层级关系
- topic 的风险等级或关键性标记

### Question

表示一道题的“学习对象本体”。

它不直接等于某种语言下展示出来的题面。  
它至少需要表达：

- 主 topic
- 相关 topic 关系入口
- 正确答案
- 难度层级
- 学习层次
- 风险标记
- 是否可用于 mock exam
- 是否可用于基础练习或复习

### QuestionVariant

表示同一道题在某个语言下的展示版本。

例如：

- 英文版本
- 中文版本

它至少需要表达：

- 属于哪个 Question
- 语言类型
- 题干
- 选项文本
- 解释文本

当前产品原则是：

- 一个 Question 可以有多个 QuestionVariant
- 不同语言版本共享同一个正确答案和同一个学习归属

## 7. 练习与作答相关实体

### PracticeSession

表示用户发起的一轮普通练习。

它至少需要表达：

- 属于哪个用户
- 开始时间
- 当前状态
- 当前进入的是哪一类练习入口
- 是否允许恢复

当前状态至少应包括：

- `in_progress`
- `completed`
- `abandoned`

### PracticeAttempt

表示用户对单题的一次作答行为。

它至少需要表达：

- 属于哪个用户
- 属于哪个 session
- 对应哪个 Question
- 使用哪个语言版本展示
- 用户选择了什么
- 是否答对
- 提交时间
- 来源入口

来源入口至少应能区分：

- 普通练习
- 复习任务
- mock exam

## 8. 错题与复习相关实体

### MistakeRecord

表示某个用户在某道题或某类知识范围上的持续错误沉淀。

它的作用不是简单复制作答记录。  
它要承载“这道题或这个点仍然需要补”的状态。

至少需要表达：

- 属于哪个用户
- 对应哪个 Question
- 主 topic 是什么
- 首次出错时间
- 最近出错时间
- 累计错误次数
- 当前是否仍为活跃薄弱点

### ReviewTask

表示系统为用户生成的一项可执行复习任务。

它不等于单道题。  
它更像一个“为什么现在要补这一块”的任务单元。

至少需要表达：

- 属于哪个用户
- 对应哪个主 topic 或薄弱点
- 任务类型
- 目标学习层次
- 优先级
- 当前状态

任务类型至少包括：

- 错题重看
- 同 topic 再练
- 基础补齐
- 混淆点强化
- 场景判断强化

当前状态至少包括：

- `todo`
- `in_progress`
- `completed`
- `expired`
- `replaced`

### ReviewPack

表示某一天系统收敛出来的一组复习任务和题目集合。

它至少需要表达：

- 属于哪个用户
- 对应哪一天
- 使用的语言（生成时继承用户当前语言偏好，决定投放的 QuestionVariant 语言版本）
- 包含哪些 ReviewTask
- 目标题量
- 已完成数量
- 当前状态

当前状态至少包括：

- `ready`
- `in_progress`
- `completed`
- `stale`

## 9. mock exam 相关实体

### MockExam

表示一套可被投放的 mock exam 模板或试卷定义。

它至少需要表达：

- 包含哪些 Question
- 适用版本
- 题目覆盖范围
- 是否可投放

### MockAttempt

表示用户参加一次 mock exam 的完整过程和结果。

它和 PracticeSession 不能混用，因为规则不同：

- mock exam 退出即结束
- mock exam 有配额消耗
- mock exam 结果要形成完整快照

它至少需要表达：

- 属于哪个用户
- 关联哪个 AccessPass
- 使用哪套 MockExam
- 开始时间
- 交卷或结束时间
- 当前状态
- 最终得分
- 是否中途退出

当前状态至少包括：

- `in_progress`
- `submitted`
- `ended_by_exit`
- `expired`

### MockAttemptQuestionResult

当前产品层建议显式存在这一层，即 mock exam 中每道题的结果明细。

至少需要表达：

- 属于哪个 MockAttempt
- 对应哪个 Question
- 用户答案
- 是否正确
- 所属 topic

这个实体在后续物理设计里可以和 PracticeAttempt 共享部分结构，  
但在产品模型里要把 mock exam 明细单独看待。

## 10. 进度与状态快照实体

### ProgressSnapshot

表示某一时刻的学习进度汇总。

它至少需要表达：

- 属于哪个用户
- 生成时间
- topic 覆盖概览
- completion 结果
- 当前主要薄弱点
- 推荐下一步动作

它的作用是：

- 为 summary 页提供稳定视图
- 避免每次都把所有历史行为重新拼装成用户可读状态

### ReadinessSnapshot

表示某一时刻的 readiness 判断结果。

它至少需要表达：

- 属于哪个用户
- 生成时间
- readiness 分值或分层
- 是否达到 ready 候选
- 未满足的硬门槛
- 当前建议动作

它和 ProgressSnapshot 可以共用底层数据来源，  
但在产品层应视为两个不同表达对象。

## 11. 导出相关实体

### MemoryExport

表示一次学习记忆导出行为。

它至少需要表达：

- 属于哪个用户
- 导出时间
- 导出状态
- 是否加密
- 是否设备绑定
- 可读取范围

当前状态至少包括：

- `pending`
- `ready`
- `failed`
- `expired`

## 12. 关键实体关系

当前核心关系如下：

- 一个 User 可以有多个 AccessPass
- 一个 User 对多个 Topic 形成持续学习状态
- 一个 Topic 可以关联多道 Question
- 一道 Question 至少属于一个主 Topic
- 一道 Question 可以有多个 QuestionVariant
- 一个 User 可以有多个 PracticeSession
- 一个 PracticeSession 可以有多个 PracticeAttempt
- 一个 User 可以有多个 MistakeRecord
- 一个 User 可以有多个 ReviewTask
- 一个 ReviewPack 可以包含多个 ReviewTask
- 一个 User 可以有多个 MockAttempt
- 一个 MockAttempt 可以包含多条题目结果明细
- 一个 User 可以有多个 ProgressSnapshot 和 ReadinessSnapshot

## 13. 关键状态流转

### 普通练习流转

当前状态流转应为：

1. 用户创建 PracticeSession
2. 用户产生多条 PracticeAttempt
3. 错误作答更新 MistakeRecord
4. 系统据此生成或更新 ReviewTask
5. 系统更新 ProgressSnapshot / ReadinessSnapshot

### 复习流转

当前状态流转应为：

1. 系统生成 ReviewTask
2. 系统将任务组织进 ReviewPack
3. 用户完成复习作答
4. 任务状态从 `todo/in_progress` 进入 `completed` 或被替换
5. 系统更新 completion 与 readiness 相关快照

### mock exam 流转

当前状态流转应为：

1. 用户消耗一次 mock 配额创建 MockAttempt
2. 用户完成或中途退出
3. MockAttempt 进入 `submitted` 或 `ended_by_exit`
4. 系统生成分数、薄弱点总结和 readiness 更新

## 14. 哪些状态应该长期保存

当前建议长期保存：

- 用户账户与访问资格
- 题目与双语版本
- 每次作答结果
- 活跃错题与薄弱点记录
- 复习任务与复习包状态
- mock exam 历史
- readiness / progress 快照
- 导出记录

## 15. 哪些状态可以按需计算

当前建议按需计算或可重建：

- 当前 topic 覆盖率细节
- 当前速度分层
- 当前某个 topic 的局部统计
- 某些 summary 页上的实时排序结果

这些内容可以依赖历史作答和快照重建，  
不一定都需要作为主存储状态长期维护。

## 16. 当前不在这份文档里定义的内容

这份文档暂时不定义：

- 关系型还是文档型数据库
- 表拆分方式
- 多对多关系的落库方式
- 软删除策略
- 审计字段完整集合
- 索引与查询优化
- 缓存策略
- 事件流或消息队列设计

这些内容要等这份产品数据模型稳定后，再进入数据库设计文档。
