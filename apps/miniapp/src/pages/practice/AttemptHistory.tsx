import { View, Text } from '@tarojs/components'
import { useEffect, useState } from 'react'
import { api } from '@/lib/request'
import { AiExplain } from '@/components/AiExplain'
import { M } from '@/messages'
import { LANG } from '@/lib/i18n'

/** Read-only history of the session's submitted answers (回看已答题). */

export type AttemptItem = {
  question_id: string
  variant_id: string
  topic_id: string
  stem: string
  choices: { key: string; text: string }[]
  correct_choice_key: string
  selected_choice_key: string
  explanation: string
  is_correct: boolean
  submitted_at: string
}

export function AttemptHistory({ sessionId, onBack }: { sessionId: string; onBack: () => void }) {
  const [items, setItems] = useState<AttemptItem[] | null>(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    let stale = false
    api<{ items: AttemptItem[] }>(
      `/api/v1/practice/sessions/${sessionId}/attempts?language=${LANG}`
    )
      .then(d => { if (!stale) setItems(d.items) })
      .catch(() => { if (!stale) setError(true) })
    return () => { stale = true }
  }, [sessionId])

  return (
    <View className='history'>
      <View className='history-head'>
        <Text className='history-title'>{M.practice.reviewHistoryTitle}</Text>
        <Text className='history-back' onClick={onBack}>‹ {M.practice.reviewHistoryBack}</Text>
      </View>

      {items === null && !error && <Text className='muted'>{M.app.loading}</Text>}
      {error && <Text className='muted'>{M.app.error}</Text>}
      {items !== null && items.length === 0 && (
        <Text className='muted'>{M.practice.reviewHistoryEmpty}</Text>
      )}

      {(items ?? []).map((a, idx) => (
        <View key={`${a.question_id}-${idx}`} className='card attempt'>
          <View className='attempt-head'>
            <Text className='attempt-index'>{idx + 1}</Text>
            <Text className={a.is_correct ? 'attempt-verdict ok' : 'attempt-verdict bad'}>
              {a.is_correct ? M.practice.correct : M.practice.incorrect}
            </Text>
          </View>
          <Text className='attempt-stem'>{a.stem}</Text>

          {a.choices.map(c => {
            const isCorrect = c.key === a.correct_choice_key
            const wrongPick = c.key === a.selected_choice_key && !isCorrect
            return (
              <View
                key={c.key}
                className={`choice choice--small ${isCorrect ? 'choice--correct' : ''} ${wrongPick ? 'choice--wrong' : ''}`}
              >
                <Text className='choice-key'>{c.key}</Text>
                <Text className='choice-text'>{c.text}</Text>
              </View>
            )
          })}

          <View className='attempt-meta'>
            <Text>{M.practice.reviewHistoryYourPick}：{a.selected_choice_key}</Text>
            <Text>{M.practice.reviewHistoryCorrect}：{a.correct_choice_key}</Text>
          </View>

          {a.explanation && (
            <View className='attempt-expl'>
              <Text>{M.practice.explanation}：{a.explanation}</Text>
            </View>
          )}

          {!a.is_correct && (
            <AiExplain
              questionId={a.question_id}
              variantId={a.variant_id}
              selectedChoiceKey={a.selected_choice_key}
            />
          )}
        </View>
      ))}
    </View>
  )
}
