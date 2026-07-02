import Taro from '@tarojs/taro'
import { API_BASE } from '../config'
import { getIdToken, isDevSession } from './auth'
import { stubRequest, stubMetaFor } from './devStub'

/**
 * Authenticated API calls: attach a fresh Firebase ID token, unwrap the
 * backend ApiResponse envelope, throw { code, status } on error — the same
 * backend contract the web client uses. In a dev-bypass session everything is
 * served by the local stub (with a small delay so loading states render).
 */

type Options = { method?: keyof Taro.request.Method; data?: any }

const STUB_DELAY_MS = 200
const sleep = (ms: number) => new Promise(res => setTimeout(res, ms))

async function realFetch(path: string, options: Options): Promise<{ data: any; meta: any }> {
  const token = await getIdToken()
  const res = await Taro.request({
    url: `${API_BASE}${path}`,
    method: (options.method as any) || 'GET',
    data: options.data,
    header: { 'content-type': 'application/json', Authorization: `Bearer ${token}` }
  })
  const body: any = res.data
  if (res.statusCode >= 200 && res.statusCode < 300) {
    return { data: body?.data, meta: body?.meta ?? {} }
  }
  const err: any = new Error(body?.error?.message || `Request failed (${res.statusCode})`)
  err.code = body?.error?.code
  err.status = res.statusCode
  throw err
}

/** Returns just the `data` field of the envelope. Most callers use this. */
export async function api<T = any>(path: string, options: Options = {}): Promise<T> {
  if (isDevSession()) {
    await sleep(STUB_DELAY_MS)
    return stubRequest(path, options.method || 'GET', options.data) as T
  }
  return (await realFetch(path, options)).data as T
}

/** Returns data + meta — for endpoints that page via `meta` (e.g. /mistakes). */
export async function apiEnvelope<T = any>(
  path: string,
  options: Options = {}
): Promise<{ data: T; meta: any }> {
  if (isDevSession()) {
    await sleep(STUB_DELAY_MS)
    return {
      data: stubRequest(path, options.method || 'GET', options.data) as T,
      meta: stubMetaFor(path)
    }
  }
  return realFetch(path, options) as Promise<{ data: T; meta: any }>
}
