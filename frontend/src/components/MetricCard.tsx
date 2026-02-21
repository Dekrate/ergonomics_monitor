import type { ReactNode } from 'react'

interface MetricCardProps {
  title: string
  value: string
  hint: string
  accent?: 'calm' | 'warning' | 'neutral'
  icon: ReactNode
}

export function MetricCard({ title, value, hint, icon, accent = 'neutral' }: MetricCardProps) {
  return (
    <article className={`metric-card metric-card--${accent}`}>
      <header className="metric-card__header">
        <span className="metric-card__icon">{icon}</span>
        <h3>{title}</h3>
      </header>
      <p className="metric-card__value">{value}</p>
      <p className="metric-card__hint">{hint}</p>
    </article>
  )
}
