import type { AiLanguage } from '../types'

interface AiLanguagePanelProps {
  language: AiLanguage | null
  supportedLanguages: AiLanguage[]
  updating: boolean
  onChange: (language: AiLanguage) => void
}

export function AiLanguagePanel({
  language,
  supportedLanguages,
  updating,
  onChange,
}: AiLanguagePanelProps) {
  return (
    <section className="panel">
      <header className="panel__header">
        <h3>AI Language</h3>
        <p>Choose the language used for AI prompting and recommendation phrasing.</p>
      </header>

      <div className="language-grid">
        {supportedLanguages.map((item) => (
          <button
            key={item}
            type="button"
            className={`language-pill ${language === item ? 'is-active' : ''}`}
            onClick={() => onChange(item)}
            disabled={updating}
          >
            {item}
          </button>
        ))}
      </div>
    </section>
  )
}
