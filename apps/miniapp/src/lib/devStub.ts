// Canned responses for the dev-bypass front-end preview (no backend, no
// Firebase). Keyed by API path — extend as pages are ported (T5/T6). These are
// only ever read behind DEV_BYPASS/isDevSession(), which is false in a real
// prod build, so this data never reaches production users.
export const devStub: Record<string, any> = {
  '/api/v1/me': {
    email: 'dev@local.test',
    current_exam: { id: 'CA-C', name_en: 'California Class C', name_zh: '加州 C 类' }
  }
}

/** Stub payload for a path (empty object if none defined yet). */
export function stubFor(path: string): any {
  return devStub[path] ?? {}
}
