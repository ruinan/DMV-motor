// Build-time config (Taro exposes TARO_APP_* to process.env). Not secrets — the
// Firebase Web API key is a public client identifier, as on the web app.
export const API_BASE: string =
  process.env.TARO_APP_API_BASE || 'http://localhost:8080'

export const FIREBASE_API_KEY: string =
  process.env.TARO_APP_FIREBASE_API_KEY || ''
