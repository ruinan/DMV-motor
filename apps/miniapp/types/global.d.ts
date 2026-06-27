/// <reference types="@tarojs/taro" />

declare namespace NodeJS {
  interface ProcessEnv {
    /** Backend base URL (TARO_APP_API_BASE). */
    TARO_APP_API_BASE: string
    /** Firebase Web API key (TARO_APP_FIREBASE_API_KEY) — public client id. */
    TARO_APP_FIREBASE_API_KEY: string
    TARO_ENV: 'weapp' | 'h5' | string
  }
}

// Taro global config helpers (provided by the toolchain at build time).
declare function defineAppConfig(config: any): any
declare function definePageConfig(config: any): any
