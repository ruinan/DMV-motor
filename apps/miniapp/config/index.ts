import { defineConfig } from '@tarojs/cli'
import devConfig from './dev'
import prodConfig from './prod'

// Taro 4 build config. See https://docs.taro.zone/docs/config
export default defineConfig(async (merge, { command, mode }) => {
  const baseConfig: any = {
    projectName: 'dmv-prep-miniapp',
    date: '2026-6-26',
    designWidth: 750,
    deviceRatio: { 640: 2.34 / 2, 750: 1, 375: 2, 828: 1.81 / 2 },
    sourceRoot: 'src',
    outputRoot: 'dist',
    plugins: [],
    defineConstants: {},
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
