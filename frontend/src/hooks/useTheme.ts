import { useEffect, useMemo, useState } from 'react'
import type { ThemeMode } from '../types'

const THEME_STORAGE_KEY = 'ergonomics.theme.mode'

function resolveTheme(mode: ThemeMode): 'light' | 'dark' {
  if (mode !== 'system') {
    return mode
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function useTheme() {
  const [mode, setMode] = useState<ThemeMode>(() => {
    const raw = window.localStorage.getItem(THEME_STORAGE_KEY)
    return raw === 'light' || raw === 'dark' || raw === 'system' ? raw : 'system'
  })

  const resolvedTheme = useMemo(() => resolveTheme(mode), [mode])

  useEffect(() => {
    const root = document.documentElement
    root.dataset.theme = resolvedTheme
    window.localStorage.setItem(THEME_STORAGE_KEY, mode)
  }, [mode, resolvedTheme])

  useEffect(() => {
    if (mode !== 'system') {
      return
    }
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const handleChange = () => {
      document.documentElement.dataset.theme = resolveTheme('system')
    }
    media.addEventListener('change', handleChange)
    return () => media.removeEventListener('change', handleChange)
  }, [mode])

  return { mode, setMode }
}
