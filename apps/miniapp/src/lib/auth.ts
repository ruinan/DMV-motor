import Taro from '@tarojs/taro'
import { API_BASE, FIREBASE_API_KEY, DEV_BYPASS } from '../config'
import { resetCache } from './cache'

/**
 * Auth for the mini-program. The Firebase JS SDK can't run here, so we use the
 * Firebase Auth REST API directly: WeChat code -> our backend custom token ->
 * REST signInWithCustomToken -> Firebase ID token (sent as Bearer to our API,
 * verified unchanged by the backend). Tokens live in mini-program storage.
 */

const ID_TOKEN = 'dmv_id_token'
const REFRESH_TOKEN = 'dmv_refresh_token'
const EXPIRES_AT = 'dmv_token_expires_at'

const IDENTITY = 'https://identitytoolkit.googleapis.com/v1'
const SECURETOKEN = 'https://securetoken.googleapis.com/v1'

// Sentinel stored as the ID token when the dev bypass "signs in". request.ts
// short-circuits to stubbed data whenever this token is the active session, so
// no real backend or Firebase is needed for UI work.
const DEV_TOKEN = 'dev-bypass'

/** Dev-only: fake a signed-in session (front-end stub, no backend/Firebase). */
export function devSignIn(): void {
  Taro.setStorageSync(ID_TOKEN, DEV_TOKEN)
  Taro.setStorageSync(REFRESH_TOKEN, DEV_TOKEN)
  Taro.setStorageSync(EXPIRES_AT, Date.now() + 3600 * 1000)
}

/** True when the current session is the dev-bypass stub (never in prod). */
export function isDevSession(): boolean {
  return DEV_BYPASS && Taro.getStorageSync(ID_TOKEN) === DEV_TOKEN
}

export type WeChatLoginResult =
  | { status: 'authenticated' }
  | { status: 'email_required' }   // new user must supply an email
  | { status: 'email_in_use' }     // email already has an account -> sign in to link

function storeTokens(idToken: string, refreshToken: string, expiresInSec: number) {
  Taro.setStorageSync(ID_TOKEN, idToken)
  Taro.setStorageSync(REFRESH_TOKEN, refreshToken)
  // refresh 60s early to avoid edge expiry
  Taro.setStorageSync(EXPIRES_AT, Date.now() + (expiresInSec - 60) * 1000)
}

export function isSignedIn(): boolean {
  return Boolean(Taro.getStorageSync(ID_TOKEN))
}

/**
 * Page-load guard for authed pages (all top-level pages except login). Returns
 * true when a session exists; in dev bypass it silently creates the stub
 * session so UI work never hits the login wall. Otherwise sends the visitor to
 * the login page and returns false.
 */
export function ensureAuthed(): boolean {
  if (isDevSession() || isSignedIn()) return true
  if (DEV_BYPASS) {
    devSignIn()
    return true
  }
  Taro.redirectTo({ url: '/pages/login/index' })
  return false
}

export function signOut(): void {
  Taro.removeStorageSync(ID_TOKEN)
  Taro.removeStorageSync(REFRESH_TOKEN)
  Taro.removeStorageSync(EXPIRES_AT)
  // The TTL cache holds the signed-out user's data; the next account on this
  // device must never see it.
  resetCache()
}

async function exchangeCustomToken(customToken: string): Promise<void> {
  const res = await Taro.request({
    url: `${IDENTITY}/accounts:signInWithCustomToken?key=${FIREBASE_API_KEY}`,
    method: 'POST',
    header: { 'content-type': 'application/json' },
    data: { token: customToken, returnSecureToken: true }
  })
  const d: any = res.data
  if (res.statusCode !== 200 || !d?.idToken) {
    throw new Error('Firebase custom-token exchange failed')
  }
  storeTokens(d.idToken, d.refreshToken, Number(d.expiresIn))
}

async function refresh(): Promise<string> {
  const rt = Taro.getStorageSync(REFRESH_TOKEN)
  if (!rt) throw new Error('not signed in')
  const res = await Taro.request({
    url: `${SECURETOKEN}/token?key=${FIREBASE_API_KEY}`,
    method: 'POST',
    header: { 'content-type': 'application/x-www-form-urlencoded' },
    data: `grant_type=refresh_token&refresh_token=${encodeURIComponent(rt)}`
  })
  const d: any = res.data
  if (res.statusCode !== 200 || !d?.id_token) throw new Error('token refresh failed')
  storeTokens(d.id_token, d.refresh_token, Number(d.expires_in))
  return d.id_token as string
}

/** A valid Firebase ID token, refreshing transparently when expired. */
export async function getIdToken(): Promise<string> {
  const token = Taro.getStorageSync(ID_TOKEN)
  if (isDevSession()) return DEV_TOKEN
  const expiresAt = Number(Taro.getStorageSync(EXPIRES_AT) || 0)
  if (token && Date.now() < expiresAt) return token
  return refresh()
}

/**
 * WeChat one-tap login. On a new user without an email -> 'email_required'; on an
 * email that already has an account -> 'email_in_use' (route to sign-in + link).
 */
export async function loginWithWeChat(email?: string): Promise<WeChatLoginResult> {
  const { code } = await Taro.login()
  const res = await Taro.request({
    url: `${API_BASE}/api/v1/auth/wechat`,
    method: 'POST',
    header: { 'content-type': 'application/json' },
    data: { code, email }
  })
  const body: any = res.data
  if (res.statusCode === 200 && body?.data?.firebase_token) {
    await exchangeCustomToken(body.data.firebase_token)
    return { status: 'authenticated' }
  }
  const errCode = body?.error?.code
  if (res.statusCode === 422 || errCode === 'EMAIL_REQUIRED') return { status: 'email_required' }
  if (res.statusCode === 409 || errCode === 'EMAIL_IN_USE') return { status: 'email_in_use' }
  throw new Error(body?.error?.message || 'WeChat login failed')
}
