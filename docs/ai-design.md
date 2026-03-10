# AI 设计

## 1. AI 在这个产品里的定位

AI 不是主流程控制器。  
主流程必须由规则引擎控制，AI 负责做三件事：

- 把错因解释得更清楚
- 把错误归类得更细
- 帮助推荐下一轮更合适的强化策略

原因很直接：判分、掌握度更新、复习调度这些环节必须稳定、可控、低成本，不能依赖模型自由发挥。

## 2. 哪些环节 AI 介入

### 环节 A：单题错因解释

输入：

- 题目
- 正确答案
- 用户错误答案
- topic / question_type / trap_type
- 用户最近同 topic 错误摘要

输出：

- 简短错因解释
- 错误归因标签
- 推荐强化策略

为什么有帮助：用户不只要知道答案错了，还要知道自己是怎么被带偏的。

### 环节 B：薄弱点总结

输入：

- 最近 20-50 条 `user_attempt`
- 每个 topic 的错误统计
- 连续错误模式

输出：

- 2 到 4 个高风险薄弱点
- 每个薄弱点的风险描述
- 下一轮优先复习顺序

为什么有帮助：把数据变成用户能执行的行动项。

### 环节 C：模拟考试总结

输入：

- 模拟考试答题结果
- 错题 topic 分布
- 历史 mastery 和 weak score

输出：

- 是否建议继续冲刺考试
- 当前最影响通过率的 2 到 3 个问题
- 具体下一步训练建议

为什么有帮助：用户最需要的是“我现在能不能去考”和“再练什么最值”。

## 3. 哪些环节必须规则优先

以下环节不能交给 AI 主导：

- 判分
- 正确答案确定
- 题目选择范围控制
- `weak_score` 计算
- `review_queue` 生成
- 模拟考试得分和 pass estimate

原因：

- 这些逻辑必须可复现
- 必须便于调参
- 必须便于排查错误
- 成本必须稳定

## 4. 错因识别框架

先由规则层做一轮粗分类，再由 AI 做细化解释。

### 规则层可直接识别的错因

- `rule_not_memorized`
  - 同一 topic 的事实题连续错误
- `similar_rule_confusion`
  - 同一组相似规则题反复互错
- `question_intent_misread`
  - 在 `legality`、`must do`、`best action` 类型题上频繁选到“合理但不符合口径”的答案
- `distractor_bias`
  - 高频选中某类干扰项
- `unstable_mastery`
  - 练习里能做对，但模拟考试里明显掉分

### AI 负责补充的内容

- 用自然语言说明“这类错法是什么意思”
- 判断更像“记忆问题”还是“理解题意问题”
- 生成面向用户的简短解释

## 5. AI 输出结构化字段

建议所有模型输出都落成 JSON，不直接信任自由文本。

### 单题解释输出

```json
{
  "mistake_reason_code": "question_intent_misread",
  "explanation_short": "你把题目当成了‘哪种做法看起来更稳妥’，但这题实际考的是 DMV 规则下谁有优先权。",
  "recommended_strategy_code": "contrast_questions",
  "focus_topic_code": "right_of_way",
  "confidence_score": 0.88
}
```

### Topic 总结输出

```json
{
  "topic_code": "lane_splitting",
  "pattern_summary": "你对 lane splitting 的基本方向知道，但在 legality 与 safest practice 的边界上容易混淆。",
  "mistake_pattern": [
    "similar_rule_confusion",
    "distractor_bias"
  ],
  "recommended_strategy_code": "contrast_drill",
  "priority_score": 91,
  "confidence_score": 0.84
}
```

### 模拟考试总结输出

```json
{
  "overall_readiness_label": "not_ready",
  "top_risks": [
    "lane_splitting",
    "right_of_way"
  ],
  "next_step_text": "先完成 10 题相似规则辨析，再做一次 25 题模拟考试。",
  "confidence_score": 0.86
}
```

## 6. 如何根据错因决定强化策略

规则引擎根据 `mistake_reason_code` 映射到训练策略：

| 错因 | 强化策略 | 为什么有效 |
| --- | --- | --- |
| `rule_not_memorized` | 同 topic 新题 + 关键规则卡片 | 先补事实记忆 |
| `similar_rule_confusion` | 对比题组 | 让用户看到相似规则边界 |
| `question_intent_misread` | 同 topic 不同题型混合训练 | 训练识别题目到底在考 legality、must、best action |
| `distractor_bias` | 干扰项拆解题 | 训练识别“看起来合理但不是 DMV 标准答案” |
| `unstable_mastery` | 限时混合训练 + 模拟考试回放 | 提升考试稳定性 |

## 7. 成本与稳定性控制

### 原则

- 能用规则解决的，不调用模型
- 能复用历史 AI 输出的，不重复生成
- 能异步生成的，不阻塞主作答流程

### 具体措施

- 单题答题后先返回规则解释，AI 解释异步补齐
- 仅对错题调用 AI，不对所有正确题调用
- 对同一 `question_id + selected_option_id + mistake_reason_code` 做缓存模板
- 长总结只在模拟考试提交后生成，不在每次普通练习后生成
- 设置 `confidence_score` 下限，过低时回退到规则模板文案

## 8. Prompt 设计原则

- 严格限制在 California motorcycle handbook topics 范围内
- 不生成法律建议扩展内容
- 不输出“官方 DMV 原文”
- 不解释未提供的规则背景
- 强制输出 JSON
- 强调简短、可执行、面向考试

## 9. 建议系统架构中的 AI 调用方式

- `Practice Attempt Submitted`
  - 同步：规则判分 + 弱点更新 + 是否入复习队列
  - 异步：发消息到任务队列生成 AI explanation

- `Mock Exam Submitted`
  - 同步：算分 + 薄弱点统计
  - 异步：生成 AI mock summary

这样设计的原因是：主流程必须稳定快速，AI 延迟不应影响用户继续学习。

## 10. 为什么这套 AI 设计适合 MVP

它没有把产品做成“智能体平台”，而是只在真正能提分的地方介入：

- 解释为什么错
- 归纳错在哪类问题
- 推荐下一轮最合适训练

这三点已经足够体现 AI 的价值，同时不会破坏系统稳定性和成本控制。
