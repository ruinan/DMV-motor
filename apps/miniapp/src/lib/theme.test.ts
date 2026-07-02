import { describe, it, expect } from 'vitest'
import { examThemeClass, examPrimary, DEFAULT_PRIMARY, M_PRIMARY } from './theme'

// Mirrors apps/web/src/styles/theme.css: motorcycle (M1/M2) gets the amber
// accent; Class C and anything unmapped keep the highway-sign blue.
describe('per-exam theme mapping', () => {
  it('M1/M2 map to the amber theme class', () => {
    expect(examThemeClass('M1')).toBe('theme-m')
    expect(examThemeClass('M2')).toBe('theme-m')
  })

  it('Class C and unknown/missing map to the default (no extra class)', () => {
    expect(examThemeClass('C')).toBe('')
    expect(examThemeClass('X9')).toBe('')
    expect(examThemeClass(null)).toBe('')
    expect(examThemeClass(undefined)).toBe('')
  })

  it('primary color follows the same mapping (for setTabBarStyle)', () => {
    expect(examPrimary('M1')).toBe(M_PRIMARY)
    expect(examPrimary('M2')).toBe(M_PRIMARY)
    expect(examPrimary('C')).toBe(DEFAULT_PRIMARY)
    expect(examPrimary(null)).toBe(DEFAULT_PRIMARY)
  })

  it('brand hexes match the web theme.css', () => {
    expect(DEFAULT_PRIMARY).toBe('#1b5e9b')
    expect(M_PRIMARY).toBe('#b45309')
  })
})
