import { View, Text, Button, Input } from '@tarojs/components'
import Taro, { useLoad } from '@tarojs/taro'
import { useState } from 'react'
import { ensureAuthed, signOut } from '@/lib/auth'
import { hideHomeCapsule } from '@/lib/nav'
import { api } from '@/lib/request'
import { invalidate } from '@/lib/bus'
import { useApi } from '@/lib/useApi'
import { useExamTheme } from '@/lib/useExamTheme'
import { TabBar } from '@/components/TabBar'
import { M, fmt } from '@/messages'
import './index.scss'

/** 我的 (tab 4) — port of the MeView essentials: profile + access state, the
 * three-state exam catalog (paid/free/locked) with switch & open-free,
 * activation-code redeem, backup status, sign out. Password/2FA/Stripe stay
 * web-only (pointer shown). */

type Exam = { id: string; state_code: string; license_class: string; name: string }
type Entitlement = { exam_id: string; subscribed: boolean; opened: boolean }

const ACCESS_LABEL: Record<string, string> = {
  free_trial: M.me.stateFreeTrial,
  active: M.me.stateActive,
  expired: M.me.stateExpired
}

export default function Me() {
  const { themeClass, me, loading } = useExamTheme()
  const { data: examsData } = useApi<{ exams: Exam[] }>('/api/v1/exams?language=zh')
  const { data: entData, refresh: refreshEnts } = useApi<{ entitlements: Entitlement[] }>(
    '/api/v1/exams/entitlements'
  )
  const [switching, setSwitching] = useState(false)

  useLoad(() => {
    ensureAuthed()
    hideHomeCapsule()
  })

  const hasPass = me?.access?.has_active_pass ?? false
  const entMap = new Map((entData?.entitlements ?? []).map(e => [e.exam_id, e]))
  const currentId = me?.current_exam?.id ?? null

  /** Three-state rule (web useExamLock parity): paid > free (opened/current) > locked. */
  function examStatus(examId: string): 'paid' | 'free' | 'locked' {
    const ent = entMap.get(examId)
    if (ent?.subscribed) return 'paid'
    if (examId === currentId || !entData) return 'free'
    return ent?.opened ? 'free' : 'locked'
  }

  async function switchTo(exam: Exam) {
    await api('/api/v1/me/exam', { method: 'PUT', data: { exam_id: exam.id } })
    // Re-scope everything; land on the study hub like web useSetExam.
    for (const p of ['/api/v1/']) invalidate(p)
    Taro.redirectTo({ url: '/pages/dashboard/index' })
  }

  async function onExamTap(exam: Exam) {
    if (switching || exam.id === currentId) return
    const status = examStatus(exam.id)
    setSwitching(true)
    try {
      if (status === 'locked') {
        const res = await Taro.showModal({
          title: fmt(M.me.examOpenTitle, { exam: exam.name }),
          content: M.me.examOpenBody,
          confirmText: M.me.examOpenFree,
          cancelText: M.me.examSwitchCancel
        })
        if (res.confirm) {
          await api(`/api/v1/exams/${exam.id}/open-free`, { method: 'POST' })
          await switchTo(exam)
        }
      } else {
        const res = await Taro.showModal({
          title: fmt(M.me.examSwitchTitle, { exam: exam.name }),
          content: M.me.examSwitchBody,
          confirmText: M.me.examSwitchYes,
          cancelText: M.me.examSwitchCancel
        })
        if (res.confirm) await switchTo(exam)
      }
    } catch (err: any) {
      Taro.showToast({ title: err?.message || M.app.error, icon: 'none' })
    } finally {
      setSwitching(false)
      refreshEnts()
    }
  }

  const out = () => {
    signOut()
    Taro.redirectTo({ url: '/pages/login/index' })
  }

  return (
    <View className={`page me ${themeClass}`}>
      <View className='head'>
        <Text className='head-title'>{M.me.title}</Text>
        <Text className='head-sub'>{M.me.subtitle}</Text>
      </View>

      {loading ? (
        <Text className='muted'>{M.app.loading}</Text>
      ) : (
        <>
          <View className='card'>
            <Text className='card-title'>{M.me.account}</Text>
            <View className='kv'><Text className='k'>{M.me.email}</Text><Text className='v'>{me?.email || '—'}</Text></View>
            <View className='kv'>
              <Text className='k'>{M.me.accessState}</Text>
              <Text className={`v ${hasPass ? 'ok' : ''}`}>
                {ACCESS_LABEL[me?.access?.state || ''] || me?.access?.state || '—'}
              </Text>
            </View>
            {me?.access?.expires_at && hasPass && (
              <View className='kv'><Text className='k'>{M.me.expiresAt}</Text><Text className='v'>{me.access.expires_at.slice(0, 10)}</Text></View>
            )}
            <View className='kv'><Text className='k'>{M.me.mockRemaining}</Text><Text className='v'>{me?.access?.mock_remaining ?? 0}</Text></View>
          </View>

          <View className='card'>
            <Text className='card-title'>{M.me.sectionExam}</Text>
            <Text className='section-body'>{M.me.sectionExamBody}</Text>
            {(examsData?.exams ?? []).map(exam => {
              const status = examStatus(exam.id)
              const isCurrent = exam.id === currentId
              return (
                <View
                  key={exam.id}
                  className={`exam-row ${isCurrent ? 'exam-row--current' : ''}`}
                  onClick={() => onExamTap(exam)}
                >
                  <View className='exam-main'>
                    <Text className='exam-name'>{exam.name}</Text>
                    {isCurrent && <Text className='exam-current-tag'>{M.me.examCurrent}</Text>}
                  </View>
                  <Text className={`exam-status exam-status--${status}`}>
                    {status === 'paid' ? M.me.examSubscribed : status === 'free' ? M.me.examFree : M.me.examLocked}
                  </Text>
                </View>
              )
            })}
          </View>

          <RedeemCard />

          <BackupCard hasPass={hasPass} />

          <Text className='web-note'>{M.me.moreOnWeb}</Text>

          <Button className='btn-out' onClick={out}>{M.me.signOut}</Button>
        </>
      )}

      <TabBar current='me' />
    </View>
  )
}

function RedeemCard() {
  const [code, setCode] = useState('')
  const [busy, setBusy] = useState(false)

  async function redeem() {
    if (busy || !code.trim()) return
    setBusy(true)
    try {
      await api(`/api/v1/access/redeem?code=${encodeURIComponent(code.trim())}`, { method: 'POST' })
      Taro.showToast({ title: M.me.redeemSuccess, icon: 'success' })
      setCode('')
      invalidate('/api/v1/')
    } catch (err: any) {
      const map: Record<string, string> = {
        CODE_ALREADY_REDEEMED: M.me.redeemAlready,
        CODE_EXHAUSTED: M.me.redeemExhausted,
        CODE_EXPIRED: M.me.redeemExpired,
        CODE_INVALID: M.me.redeemInvalid
      }
      Taro.showToast({ title: map[err?.code] || M.me.redeemError, icon: 'none' })
    } finally {
      setBusy(false)
    }
  }

  return (
    <View className='card'>
      <Text className='card-title'>{M.me.redeemTitle}</Text>
      <Text className='section-body'>{M.me.redeemBody}</Text>
      <View className='redeem-row'>
        <Input
          className='redeem-input'
          value={code}
          placeholder={M.me.redeemPlaceholder}
          onInput={e => setCode(e.detail.value)}
        />
        <Button
          className='btn-primary redeem-btn'
          size='mini'
          loading={busy}
          disabled={busy || !code.trim()}
          onClick={redeem}
        >
          {M.me.redeemSubmit}
        </Button>
      </View>
    </View>
  )
}

function BackupCard({ hasPass }: { hasPass: boolean }) {
  const { data, refresh } = useApi<{ backed_up_at: string | null }>(
    hasPass ? '/api/v1/backup/latest' : null
  )
  const [busy, setBusy] = useState(false)

  async function backupNow() {
    if (busy) return
    setBusy(true)
    const startedAt = Date.now()
    try {
      await api('/api/v1/backup/sync', { method: 'POST' })
      Taro.showToast({ title: M.me.backupSynced, icon: 'success' })
      refresh()
    } catch (err: any) {
      Taro.showToast({ title: err?.message || M.app.error, icon: 'none' })
    } finally {
      const elapsed = Date.now() - startedAt
      if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
      setBusy(false)
    }
  }

  return (
    <View className='card'>
      <Text className='card-title'>{M.me.sectionBackup}</Text>
      <Text className='section-body'>{M.me.sectionBackupBody}</Text>
      {hasPass ? (
        <>
          <Text className='backup-status'>
            {data?.backed_up_at
              ? fmt(M.me.backupLastSynced, { time: data.backed_up_at.replace('T', ' ').slice(0, 16) })
              : M.me.backupNone}
          </Text>
          <Button
            className='btn-primary backup-btn'
            loading={busy}
            disabled={busy}
            onClick={backupNow}
          >
            {busy ? M.me.backupSaving : M.me.backupNow}
          </Button>
        </>
      ) : (
        <Text className='muted'>{M.me.backupPaidOnly}</Text>
      )}
    </View>
  )
}
