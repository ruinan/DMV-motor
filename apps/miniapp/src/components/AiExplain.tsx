import { View, Text, Button } from '@tarojs/components'
import { useEffect, useState } from 'react'
import { useAiExplain } from '@/lib/useAiExplain'
import { M, fmt } from '@/messages'
import './AiExplain.scss'

/**
 * One self-contained AI-explanation surface — port of the web AiExplainBlock.
 * Click-to-reveal base explanation, then 深入分析 direction chips appending
 * deeper layers (thread persists in storage). Mount with key={questionId} so a
 * new question starts a fresh thread. Cooldowns count down on the button and
 * auto-retry (handled by the hook).
 */

const ASPECTS = [
  { code: 'example', label: M.ai.aspectExample },
  { code: 'mnemonic', label: M.ai.aspectMnemonic },
  { code: 'distractors', label: M.ai.aspectDistractors },
  { code: 'rule', label: M.ai.aspectRule }
] as const

type Props = {
  questionId: string
  variantId?: string
  selectedChoiceKey?: string
}

export function AiExplain({ questionId, variantId, selectedChoiceKey }: Props) {
  const { state, explain, deepen, cooldownUntil } = useAiExplain({
    questionId,
    variantId,
    selectedChoiceKey
  })
  const loading = state.status === 'loading'

  // Display-only countdown; the hook owns the auto-retry.
  const [cooldownSec, setCooldownSec] = useState(0)
  useEffect(() => {
    const tick = () => setCooldownSec(Math.max(0, Math.ceil((cooldownUntil - Date.now()) / 1000)))
    tick()
    if (cooldownUntil <= Date.now()) return
    const id = setInterval(tick, 500)
    return () => clearInterval(id)
  }, [cooldownUntil])
  const cooling = cooldownSec > 0
  const coolingLabel = fmt(M.ai.explainCoolingDown, { n: cooldownSec })
  const isCooldownErr =
    state.errorCode === 'RATE_LIMITED' && /in\s+\d+\s*s/.test(state.errorMessage ?? '')

  function errorMessage(): string {
    if (state.errorCode === 'RATE_LIMITED') return M.ai.explainCooldown
    if (state.errorCode === 'AI_UNAVAILABLE') return M.ai.explainUnavailable
    return M.ai.explainError
  }

  if (state.layers.length === 0) {
    return (
      <View className='ai-explain'>
        {state.status === 'error' && !isCooldownErr && (
          <Text className='ai-error'>{errorMessage()}</Text>
        )}
        <Button
          className='ai-reveal-btn'
          disabled={loading || cooling}
          onClick={explain}
        >
          {cooling ? coolingLabel : loading ? M.ai.explainLoading : `✨ ${M.ai.explainButton}`}
        </Button>
      </View>
    )
  }

  const capReached = state.depthRemaining !== null && state.depthRemaining <= 0

  return (
    <View className='ai-explain'>
      <Text className='ai-heading'>✨ {M.ai.explainHeading}</Text>

      {state.layers.map((layer, i) => (
        <View key={i} className={i > 0 ? 'ai-layer ai-layer--deep' : 'ai-layer'}>
          {layer.aspect && (
            <Text className='ai-aspect-tag'>
              {ASPECTS.find(a => a.code === layer.aspect)?.label || layer.aspect}
            </Text>
          )}
          <Text className='ai-text'>{layer.text}</Text>
          {i === 0 && layer.cached && <Text className='ai-cached'>{M.ai.explainCached}</Text>}
        </View>
      ))}

      {state.status === 'error' && !isCooldownErr && (
        <Text className='ai-error'>{errorMessage()}</Text>
      )}

      {capReached ? (
        <Text className='ai-note'>{M.ai.explainDepthReached}</Text>
      ) : cooling ? (
        <Text className='ai-note'>{coolingLabel}</Text>
      ) : loading ? (
        <Text className='ai-note ai-note--busy'>{M.ai.explainLoading}</Text>
      ) : (
        <View className='ai-aspects'>
          <Text className='ai-deepen-label'>{M.ai.explainDeepen}</Text>
          {ASPECTS.map(a => (
            <View key={a.code} className='ai-aspect-chip' onClick={() => deepen(a.code)}>
              <Text>{a.label}</Text>
            </View>
          ))}
        </View>
      )}
    </View>
  )
}
