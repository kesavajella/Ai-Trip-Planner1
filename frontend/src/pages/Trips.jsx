import { useState, useEffect } from 'react'
import { api } from '../api'
import { esc, cap, usd } from '../utils'

export default function Trips() {
  const [trips, setTrips] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.get('/trips')
      .then((data) => setTrips(data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return <h1 className="mb-6 text-3xl font-bold">My trips</h1>
  }

  if (error) {
    return (
      <>
        <h1 className="mb-6 text-3xl font-bold">My trips</h1>
        <div className="card border-border text-destructive">{esc(error)}</div>
      </>
    )
  }

  if (trips.length === 0) {
    return (
      <>
        <h1 className="mb-6 text-3xl font-bold">My trips</h1>
        <div className="card text-muted-foreground">You haven't created any trips yet.</div>
      </>
    )
  }

  return (
    <>
      <h1 className="mb-6 text-3xl font-bold">My trips</h1>
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {trips.map((trip) => (
          <TripCard key={trip.id} trip={trip} />
        ))}
      </div>
    </>
  )
}

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
