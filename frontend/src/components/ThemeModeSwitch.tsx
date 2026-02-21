import type { ThemeMode } from '../types'

interface ThemeModeSwitchProps {
  mode: ThemeMode
  onChange: (mode: ThemeMode) => void
}

const OPTIONS: ThemeMode[] = ['light', 'dark', 'system']

export function ThemeModeSwitch({ mode, onChange }: ThemeModeSwitchProps) {
  return (
    <div className="theme-switch" role="radiogroup" aria-label="Theme mode">
      {OPTIONS.map((option) => (
        <button
          key={option}
          type="button"
          role="radio"
          aria-checked={mode === option}
          className={`theme-switch__item ${mode === option ? 'is-active' : ''}`}
          onClick={() => onChange(option)}
        >
          {option}
        </button>
      ))}
    </div>
  )
}
