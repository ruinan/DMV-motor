import { defineConfig } from 'vitest/config'
import path from 'path'

// Unit tests cover the Taro-free pure logic in src/lib (bus, theme mapping,
// dev stub). Anything importing @tarojs/taro stays out of unit scope and is
// verified in WeChat DevTools instead.
export default defineConfig({
  resolve: {
    alias: { '@': path.resolve(__dirname, 'src') }
  },
  test: {
    include: ['src/**/*.test.ts'],
    environment: 'node'
  }
})
