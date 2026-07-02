import Taro from '@tarojs/taro'
import { useEffect, useRef, useState } from 'react'
import { api } from './request'
import { LANG } from './i18n'

/**
 * AI explanation thread for one question — port of the web's use-ai-explain.
 * Layer 0 is the base "why was I wrong" explanation (server DB-cached); layers
 * 1..N are 深入分析 deep-dives the server does NOT persist, so the whole thread
 * lives here + in mini-program storage (revisits hydrate instantly, no server
 * hit). RATE_LIMITED cooldowns auto-retry the same request when they elapse —
 * the user already clicked once and is owed that answer.
 */

export type AiLayer = { depth: number; text: string; cached: boolean; aspect?: string }

export type AiExplainState = {
  status: 'idle' | 'loading' | 'error'
  layers: AiLayer[]
  depthRemaining: number | null
  errorCode?: string
  errorMessage?: string
}

type AiExplainResponse = {
  explanation: string
  cached: boolean
  model: string
  language: string
  depth: number
  depth_remaining: number
}

type StoredThread = { layers: AiLayer[]; depthRemaining: number | null }

const STORAGE_PREFIX = 'ai-explain:v1:'
const EMPTY: AiExplainState = { status: 'idle', layers: [], depthRemaining: null }

function readThread(key: string): StoredThread | null {
  try {
    const raw = Taro.getStorageSync(key)
    if (!raw) return null
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(parsed?.layers) ? parsed : null
  } catch {
    return null
  }
}

function writeThread(key: string, thread: StoredThread): void {
  try {
    Taro.setStorageSync(key, JSON.stringify(thread))
  } catch {
    // Storage full — the thread just won't persist.
  }
}

export function useAiExplain(identity: {
  questionId: string
  variantId?: string
  selectedChoiceKey?: string
}) {
  const { questionId, variantId, selectedChoiceKey } = identity
  const key = `${STORAGE_PREFIX}${questionId}:${LANG}`
  const [state, setState] = useState<AiExplainState>(EMPTY)
  const [cooldownUntil, setCooldownUntil] = useState(0)
  const retryTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    const saved = readThread(key)
    setState(saved ? { status: 'idle', layers: saved.layers, depthRemaining: saved.depthRemaining } : EMPTY)
    setCooldownUntil(0)
    return () => {
      if (retryTimer.current) {
        clearTimeout(retryTimer.current)
        retryTimer.current = null
      }
    }
  }, [key])

  async function call(depth: number, aspect?: string) {
    const priorContext = depth > 0 ? state.layers.map(l => l.text).join('\n\n') : ''
    if (retryTimer.current) {
      clearTimeout(retryTimer.current)
      retryTimer.current = null
    }
    setState(s => ({ ...s, status: 'loading', errorCode: undefined, errorMessage: undefined }))
    try {
      const res = await api<AiExplainResponse>('/api/v1/ai/explain', {
        method: 'POST',
        data: {
          question_id: questionId,
          variant_id: variantId,
          selected_choice_key: selectedChoiceKey,
          language: LANG,
          depth,
          ...(aspect ? { aspect } : {}),
          ...(priorContext ? { prior_context: priorContext } : {})
        }
      })
      setState(s => {
        const layers = [...s.layers, { depth: res.depth, text: res.explanation, cached: res.cached, aspect }]
        writeThread(key, { layers, depthRemaining: res.depth_remaining })
        return { status: 'idle', layers, depthRemaining: res.depth_remaining }
      })
      setCooldownUntil(0)
    } catch (err: any) {
      const code = err?.code as string | undefined
      const message = (err?.message as string) || 'Network error'
      setState(s => ({ ...s, status: 'error', errorCode: code, errorMessage: message }))
      const m = code === 'RATE_LIMITED' ? message.match(/in\s+(\d+)\s*s/) : null
      if (m) {
        const secs = Math.min(parseInt(m[1], 10), 120)
        setCooldownUntil(Date.now() + secs * 1000)
        retryTimer.current = setTimeout(() => {
          retryTimer.current = null
          void call(depth, aspect)
        }, secs * 1000)
      }
    }
  }

  /** Reveal the base explanation (depth 0). No-op if we already have it. */
  function explain() {
    if (state.layers.some(l => l.depth === 0)) return
    void call(0)
  }

  /** Request a deeper layer in a chosen direction (深入分析). */
  function deepen(aspect: string) {
    void call(state.layers.length, aspect)
  }

  return { state, explain, deepen, cooldownUntil }
}
