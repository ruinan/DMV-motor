// Per-exam theme mapping — mirrors apps/web/src/styles/theme.css:
// motorcycle (M1/M2) gets the warm amber, Class C / unmapped keep the
// highway-sign blue. Pure functions only (unit-tested); the Taro side effect
// (setTabBarStyle) lives in useExamTheme.ts.

export const DEFAULT_PRIMARY = '#1b5e9b'
export const M_PRIMARY = '#b45309'

/** Root class enabling the amber variable overrides in app.scss ('' = default). */
export function examThemeClass(licenseClass?: string | null): string {
  return licenseClass === 'M1' || licenseClass === 'M2' ? 'theme-m' : ''
}

/** Accent hex for the active exam — used to tint the native tabBar. */
export function examPrimary(licenseClass?: string | null): string {
  return examThemeClass(licenseClass) ? M_PRIMARY : DEFAULT_PRIMARY
}
