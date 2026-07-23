import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { esc, cap, usd } from '../utils'

const STATUSES = ['UPCOMING', 'DRAFT', 'COMPLETED']

export default function TripDetail() {
  const { id } = useParams()
  const [trip, setTrip] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    api.get(`/trips/${id}`)
      .then((data) => { setTrip(data); setLoading(false) })
      .catch((err) => { setError(err.message); setLoading(false) })
  }, [id])

  async function handleStatusChange(e) {
    setBusy(true)
    try {
      await api.patch(`/trips/${id}/status`, { status: e.target.value })
      loadTrip()
    } finally {
      setBusy(false)
    }
  }

  async function handleDelete() {
    if (!confirm('Delete this trip?')) return
    setBusy(true)
    try {
      await api.del(`/trips/${id}`)
      navigate('/trips')
    } finally {
      setBusy(false)
    }
  }

  function loadTrip() {
    api.get(`/trips/${id}`).then((data) => setTrip(data)).catch((err) => setError(err.message))
  }

  if (loading) return <p className="text-muted-foreground">Loading…</p>
  if (error) return <div className="card border-border text-destructive">{esc(error)}</div>
  if (!trip) return null

  return (
    <div>
      <div className="mb-6 flex items-start justify-between">
        <div>
          <h1 className="text-3xl font-bold">{esc(trip.destination)}</h1>
          <p className="text-muted-foreground">{trip.days} days · {cap(trip.travelType)} · {esc(trip.country || '—')}</p>
        </div>
        <div className="flex gap-2">
          <select className="input w-auto" value={trip.status?.toUpperCase() || 'DRAFT'} onChange={handleStatusChange} disabled={busy}>
            {STATUSES.map((s) => (
              <option key={s} value={s}>{cap(s.toLowerCase())}</option>
            ))}
          </select>
          <button className="btn btn-outline" onClick={handleDelete} disabled={busy}>Delete</button>
        </div>
      </div>
      <div className="grid gap-4 sm:grid-cols-3">
        <div className="card"><p className="text-sm text-muted-foreground">Budget</p><p className="mt-1 font-semibold capitalize">{esc((trip.budget || '').toLowerCase())}</p></div>
        <div className="card"><p className="text-sm text-muted-foreground">Budget (USD)</p><p className="mt-1 font-semibold">{usd(trip.budgetUsd)}</p></div>
        <div className="card"><p className="text-sm text-muted-foreground">Start date</p><p className="mt-1 font-semibold">{esc(trip.startDate || '—')}</p></div>
      </div>
      {trip.interests && trip.interests.length > 0 && (
        <div className="mt-6 flex flex-wrap gap-2">
          {trip.interests.map((i) => (
            <span key={i} className="badge">{esc(i)}</span>
          ))}
        </div>
      )}
    </div>
  )
}
