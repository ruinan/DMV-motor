import Taro from '@tarojs/taro'
import { API_BASE } from '../config'
import { getIdToken, isDevSession } from './auth'
import { stubFor } from './devStub'

/**
 * Authenticated API call: attaches a fresh Firebase ID token, unwraps the
 * backend ApiResponse envelope, and throws { code, status } on error — the same
 * backend contract the web client uses.
 */
export async function api<T = any>(
  path: string,
  options: { method?: keyof Taro.request.Method; data?: any } = {}
): Promise<T> {
  // Dev bypass: serve canned data, skip the network entirely.
  if (isDevSession()) return stubFor(path) as T
  const token = await getIdToken()
  const res = await Taro.request({
    url: `${API_BASE}${path}`,
    method: (options.method as any) || 'GET',
    data: options.data,
    header: { 'content-type': 'application/json', Authorization: `Bearer ${token}` }
  })
  const body: any = res.data
  if (res.statusCode >= 200 && res.statusCode < 300) {
    return body?.data as T
  }
  const err: any = new Error(body?.error?.message || `Request failed (${res.statusCode})`)
  err.code = body?.error?.code
  err.status = res.statusCode
  throw err
}
