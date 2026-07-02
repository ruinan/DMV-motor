import { View, Text, Button } from '@tarojs/components'
import Taro, { useLoad } from '@tarojs/taro'
import { useEffect, useState } from 'react'
import { ensureAuthed } from '@/lib/auth'
import { api, apiEnvelope } from '@/lib/request'
import { cacheGet, cachedFetch, isFresh } from '@/lib/cache'
import { invalidate } from '@/lib/bus'
import { useApi } from '@/lib/useApi'
import { useExamTheme } from '@/lib/useExamTheme'
import { AiExplain } from '@/components/AiExplain'
import { M, fmt } from '@/messages'
import { LANG } from '@/lib/i18n'
import './index.scss'

/** Active mistakes — port of web MistakesView: paged list, expandable rows
 * with a lazily-loaded review (correct answer + explanation + AI 解析), and a
 * "针对性练习" CTA that starts a topic-filtered session and jumps to practice
 * (which auto-resumes it). Reached from the dashboard. */

type MistakeItem = {
  mistake_id: string
  question_id: string
  topic_id: string
  wrong_count: number
  last_wrong_at: string
  source: string
}

type Topic = { id: string; name_zh: string }

const PAGE_SIZE = 20

const SOURCE_LABEL: Record<string, string> = {
  practice: M.mistakes.fromPractice,
  review: M.mistakes.fromReview,
  mock: M.mistakes.fromMock
}

export default function Mistakes() {
  const { themeClass, me } = useExamTheme()
  const { data: topicsData } = useApi<{ items: Topic[] }>('/api/v1/topics')
  const [page, setPage] = useState(1)
  const [items, setItems] = useState<MistakeItem[] | null>(null)
  const [total, setTotal] = useState(0)
  const [loadError, setLoadError] = useState(false)
  const [starting, setStarting] = useState(false)

  useLoad(() => { ensureAuthed() })

  useEffect(() => {
    const path = `/api/v1/mistakes?page=${page}&page_size=${PAGE_SIZE}`
    let stale = false
    const apply = (data: any, meta: any) => {
      setItems(data?.items ?? [])
      setTotal(Number(meta?.total ?? 0))
    }
    // Fresh cache (30s TTL, evicted by invalidate('/api/v1/mistakes')) serves
    // instantly — re-entering the page or flipping back a page skips the fetch.
    const cached = cacheGet(path)
    if (cached && isFresh(path)) {
      apply(cached.data, cached.meta)
      setLoadError(false)
      return
    }
    setItems(null)
    setLoadError(false)
    cachedFetch(path, () => apiEnvelope<{ items: MistakeItem[] }>(path))
      .then(env => { if (!stale) apply(env.data, env.meta) })
      .catch(() => { if (!stale) setLoadError(true) })
    return () => { stale = true }
  }, [page])

  const topicName = (id: string) =>
    topicsData?.items.find(t => t.id === id)?.name_zh ?? `知识点 ${id}`

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE))
  const mistakeTopicIds = Array.from(new Set((items ?? []).map(m => m.topic_id))).slice(0, 8)

  async function practiceThese() {
    if (starting || mistakeTopicIds.length === 0) return
    setStarting(true)
    try {
      await api('/api/v1/practice/sessions', {
        method: 'POST',
        data: {
          entry_type: me?.access?.has_active_pass ? 'full' : 'free_trial',
          language: LANG,
          // Real topic ids are numeric; the stub uses string ids — send what parses.
          topic_filter: mistakeTopicIds.map(id => (isNaN(Number(id)) ? id : Number(id)))
        }
      })
      invalidate('/api/v1/me')
      Taro.redirectTo({ url: '/pages/practice/index' })
    } catch (err: any) {
      Taro.showToast({ title: err?.message || M.app.error, icon: 'none' })
      setStarting(false)
    }
  }

  return (
    <View className={`page mistakes ${themeClass}`}>
      <View className='head'>
        <Text className='head-title'>{M.mistakes.title}</Text>
        <Text className='head-sub'>{M.mistakes.subtitle}</Text>
      </View>

      {items && items.length > 0 && (
        <View className='practice-cta card'>
          <Text className='cta-hint'>{fmt(M.mistakes.practiceTheseHint, { n: mistakeTopicIds.length })}</Text>
          <Button className='btn-primary' loading={starting} disabled={starting} onClick={practiceThese}>
            {starting ? M.mistakes.practiceTheseStarting : M.mistakes.practiceThese}
          </Button>
        </View>
      )}

      {items === null && !loadError && <Text className='muted'>{M.app.loading}</Text>}
      {loadError && <View className='error-box'><Text>{M.mistakes.loadError}</Text></View>}

      {items && items.length === 0 && (
        <View className='card empty-card'>
          <Text className='empty-icon'>🔖</Text>
          <Text className='muted'>{M.mistakes.empty}</Text>
        </View>
      )}

      {(items ?? []).map(m => (
        <MistakeRow key={m.mistake_id} item={m} topicName={topicName(m.topic_id)} />
      ))}

      {totalPages > 1 && (
        <View className='pager'>
          <Text
            className={`pager-btn ${page <= 1 ? 'pager-btn--off' : ''}`}
            onClick={() => page > 1 && setPage(p => p - 1)}
          >
            ‹ {M.mistakes.prevPage}
          </Text>
          <Text className='muted'>{fmt(M.mistakes.pageOf, { page, total: totalPages })}</Text>
          <Text
            className={`pager-btn ${page >= totalPages ? 'pager-btn--off' : ''}`}
            onClick={() => page < totalPages && setPage(p => p + 1)}
          >
            {M.mistakes.nextPage} ›
          </Text>
        </View>
      )}
    </View>
  )
}

function MistakeRow({ item, topicName }: { item: MistakeItem; topicName: string }) {
  const [expanded, setExpanded] = useState(false)

  return (
    <View className='card mistake-row'>
      <View className='row-head' onClick={() => setExpanded(e => !e)}>
        <View className='row-main'>
          <View className='row-chips'>
            <Text className='chip chip--topic'>{topicName}</Text>
            <Text className='chip chip--source'>{SOURCE_LABEL[item.source] || item.source}</Text>
          </View>
          <Text className='row-meta'>
            {fmt(M.mistakes.wrongCount, { count: item.wrong_count })} · {M.mistakes.lastWrong}：{item.last_wrong_at.slice(0, 10)}
          </Text>
        </View>
        <Text className={`row-arrow ${expanded ? 'row-arrow--up' : ''}`}>⌄</Text>
      </View>
      {expanded && <MistakeReviewPanel questionId={item.question_id} />}
    </View>
  )
}

function MistakeReviewPanel({ questionId }: { questionId: string }) {
  type Review = {
    question_id: string
    variant_id: string
    stem: string
    choices: { key: string; text: string }[]
    correct_choice_key: string
    explanation: string
  }
  const { data, error, loading } = useApi<Review>(
    `/api/v1/mistakes/${questionId}/review?language=${LANG}`
  )

  if (loading) return <Text className='muted panel-pad'>{M.app.loading}</Text>
  if (error || !data) return <Text className='bad panel-pad'>{M.mistakes.loadError}</Text>

  return (
    <View className='review-panel'>
      <Text className='panel-stem'>{data.stem}</Text>
      {data.choices.map(c => {
        const isCorrect = c.key === data.correct_choice_key
        return (
          <View key={c.key} className={`choice choice--small ${isCorrect ? 'choice--correct' : 'choice--dim'}`}>
            <Text className='choice-key'>{c.key}</Text>
            <Text className='choice-text'>{c.text}</Text>
            {isCorrect && <Text className='choice-mark ok'>✓</Text>}
          </View>
        )
      })}
      {data.explanation && (
        <View className='panel-expl'>
          <Text>{M.mistakes.explanation}：{data.explanation}</Text>
        </View>
      )}
      <AiExplain questionId={data.question_id} variantId={data.variant_id} />
    </View>
  )
}
