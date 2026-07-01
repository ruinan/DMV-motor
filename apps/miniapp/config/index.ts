import path from 'path'
import { defineConfig } from '@tarojs/cli'
import devConfig from './dev'
import prodConfig from './prod'

// Taro 4 build config. See https://docs.taro.zone/docs/config
export default defineConfig(async (merge, { command, mode }) => {
  // Dev-only login bypass (front-end stub). ON for watch/dev builds, OFF for
  // production builds. Force with TARO_APP_DEV_BYPASS=1 (e.g. a one-off preview
  // build) or disable with =0. A real prod build must never enable this.
  const devBypass =
    process.env.TARO_APP_DEV_BYPASS === '1' ||
    (mode === 'development' && process.env.TARO_APP_DEV_BYPASS !== '0')

  const baseConfig: any = {
    projectName: 'dmv-prep-miniapp',
    date: '2026-6-26',
    designWidth: 750,
    deviceRatio: { 640: 2.34 / 2, 750: 1, 375: 2, 828: 1.81 / 2 },
    sourceRoot: 'src',
    outputRoot: 'dist',
    // Match tsconfig paths ("@/*" -> "./src/*") so the vite/rollup build
    // resolves the same aliases the editor/TS does.
    alias: {
      '@': path.resolve(__dirname, '..', 'src')
    },
    plugins: [],
    // The mini-program runtime has no `process` global. Bake TARO_APP_* env
    // reads into string literals at build time so `src/config.ts` never emits a
    // bare `process.env.*` (which crashes the page with "process is not
    // defined"). Values come from the build env / .env files, with safe
    // defaults so a missing .env still builds and runs.
    defineConstants: {
      'process.env.TARO_APP_API_BASE': JSON.stringify(
        process.env.TARO_APP_API_BASE || 'http://localhost:8080'
      ),
      'process.env.TARO_APP_FIREBASE_API_KEY': JSON.stringify(
        process.env.TARO_APP_FIREBASE_API_KEY || ''
      ),
      'process.env.TARO_APP_DEV_BYPASS': JSON.stringify(devBypass ? '1' : '')
    },
    copy: { patterns: [], options: {} },
    framework: 'react',
    compiler: 'vite',
    mini: {
      postcss: {
        pxtransform: { enable: true, config: {} },
        cssModules: { enable: false }
      }
    },
    h5: {
      publicPath: '/',
      staticDirectory: 'static',
      esnextModules: [],
      postcss: {
        autoprefixer: { enable: true, config: {} },
        cssModules: { enable: false }
      }
    }
  }
  if (command === 'build' || mode === 'production') {
    return merge({}, baseConfig, prodConfig)
  }
  return merge({}, baseConfig, devConfig)
})
