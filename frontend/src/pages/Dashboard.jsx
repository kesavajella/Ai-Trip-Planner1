import { useState, useEffect } from 'react'
import { api } from '../api'

export default function Dashboard() {
  const [trips, setTrips] = useState([])
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [user, setUser] = useState(() => {
    try {
      const raw = localStorage.getItem('intellitrip_user')
      return raw ? JSON.parse(raw) : null
    } catch (e) {
      return null
    }
  })

  useEffect(() => {
    Promise.all([
      api.get('/trips').catch(() => []),
      api.get('/notifications').catch(() => []),
    ]).then(([tripsData, notificationsData]) => {
      setTrips(tripsData || [])
      setNotifications(notificationsData || [])
    }).finally(() => {
      setLoading(false)
    })
  }, [])

  const upcoming = trips.filter((t) => t.status === 'upcoming')
  const drafts = trips.filter((t) => t.status === 'draft')
  const upcomingDays = upcoming.reduce((s, t) => s + (t.days || 0), 0)
  const budget = upcoming.reduce((s, t) => s + (t.budgetUsd || 0), 0)

  function Stat({ label, value }) {
    return (
      <div className="card">
        <p className="text-sm text-muted-foreground">{label}</p>
        <p className="mt-1 text-2xl font-bold">{value}</p>
      </div>
    )
  }

  if (loading) {
    return <p className="text-muted-foreground">Loading dashboard…</p>
  }

  return (
    <div>
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Welcome, {(user?.name || '').split(' ')[0]}</h1>
          <p className="text-muted-foreground">Here's what's coming up on your trips.</p>
        </div>
        <a href="#/generate" className="btn btn-primary">Plan a trip</a>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Total trips" value={String(trips.length)} />
        <Stat label="Upcoming days" value={String(upcomingDays)} />
        <Stat label="Budget tracked" value={budget ? '$' + Math.round(budget).toLocaleString() : '$0'} />
        <Stat label="Notifications" value={String(notifications.length)} />
      </div>
      {notifications.length > 0 && (
        <div className="card mt-6">
          <h2 className="mb-3 text-lg font-semibold">Notifications</h2>
          <ul className="space-y-2">
            {notifications.map((n, idx) => (
              <li key={idx} className="rounded-lg bg-muted px-4 py-3 text-sm">{n.message}</li>
            ))}
          </ul>
        </div>
      )}
      <section className="mt-8">
        <h2 className="mb-4 text-xl font-semibold">Upcoming trips</h2>
        {upcoming.length === 0 ? (
          <div className="card text-muted-foreground">No upcoming trips yet. <a className="text-primary" href="#/generate">Plan one now</a>.</div>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {upcoming.map(TripCard)}
          </div>
        )}
      </section>
      {drafts.length > 0 && (
        <section className="mt-8">
          <h2 className="mb-4 text-xl font-semibold">Drafts</h2>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {drafts.map(TripCard)}
          </div>
        </section>
      )}
    </div>
  )
}

import { esc, cap, usd } from '../utils'

function TripCard({ trip }) {
  const status = trip.status || 'draft'
  const statusLabel = cap(status)
  const statusClass = status === 'upcoming' ? 'status-upcoming' : status === 'draft' ? 'status-draft' : 'status-completed'

  return (
    <a href={`#/trips/${trip.id}`} className="card trip-card">
      <div className="flex items-start justify-between">
        <div>
          <h3 className="font-semibold">{esc(trip.destination)}</h3>
          <p className="text-sm text-muted-foreground">{trip.days} days · {cap(trip.travelType || 'Solo')}</p>
        </div>
        <span className={`badge ${statusClass}`}>{statusLabel}</span>
      </div>
      <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
        <span className="capitalize">{esc(trip.budget || '—')} budget</span>
        {trip.budgetUsd != null && <span>{usd(trip.budgetUsd)}</span>}
      </div>
    </a>
  )
}
