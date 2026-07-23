import { useState, useEffect } from 'react'
import { api } from '../api'
import { esc, cap } from '../utils'

function Stat({ label, value }) {
  return (
    <div className="card">
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className="mt-1 text-2xl font-bold">{value}</p>
    </div>
  )
}

export default function Admin() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.get('/admin/analytics')
      .then((d) => { setData(d); setLoading(false) })
      .catch((err) => { setError(err.message); setLoading(false) })
  }, [])

  if (loading) return <p className="text-muted-foreground">Loading…</p>
  if (error) return <div className="card border-border text-destructive">{esc(error)}</div>
  if (!data) return null

  return (
    <div>
      <h1 className="mb-6 text-3xl font-bold">Admin dashboard</h1>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Total users" value={(data.totalUsers || 0).toLocaleString()} />
        <Stat label="Total trips" value={(data.totalTrips || 0).toLocaleString()} />
        <Stat label="Avg trip days" value={String(data.avgTripDays || 0)} />
        <Stat label="Generated this week" value={String(data.generatedThisWeek || 0)} />
      </div>
      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <div className="card">
          <h2 className="mb-4 text-lg font-semibold">Weekly signups</h2>
          <BarChart data={data.weeklySignups || []} />
        </div>
        <div className="card">
          <h2 className="mb-4 text-lg font-semibold">Trips by type</h2>
          <PieChart data={data.tripsByType || []} />
        </div>
      </div>
    </div>
  )
}

function BarChart({ data }) {
  const max = Math.max(1, ...data.map((d) => d.users || 0))
  return (
    <div className="chart">
      {data.map((d) => (
        <div key={d.day} className="bar-wrap">
          <span className="bar-value">{d.users}</span>
          <div className="bar" style={{ height: `${(d.users / max) * 100}%` }} />
          <span className="bar-label">{esc(d.day)}</span>
        </div>
      ))}
    </div>
  )
}

function PieChart({ data }) {
  const colors = ['#0f172a', '#64748b', '#0ea5e9', '#f59e0b']
  const total = data.reduce((s, d) => s + (d.count || 0), 0) || 1
  return (
    <>
      <div style={{ display: 'flex', height: '200px', gap: '2px', alignItems: 'flex-end' }}>
        {data.map((d, i) => {
          const pct = ((d.count || 0) / total) * 100
          return <div key={d.type} style={{ height: `${pct}%`, background: colors[i % colors.length], borderRadius: '4px 4px 0 0' }} />
        })}
      </div>
      <div className="legend">
        {data.map((d, i) => (
          <span key={d.type} className="item"><span className="dot" style={{ background: colors[i % colors.length] }} />{esc(d.type)} ({d.count})</span>
        ))}
      </div>
    </>
  )
}
