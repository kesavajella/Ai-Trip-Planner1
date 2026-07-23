import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { esc, cap, money } from '../utils'
import Itinerary from './Itinerary'

export default function Generate() {
  const navigate = useNavigate()
  const [destination, setDestination] = useState('')
  const [days, setDays] = useState(5)
  const [budget, setBudget] = useState('medium')
  const [traveler, setTraveler] = useState('solo')
  const [interests, setInterests] = useState(['culture', 'food'])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [result, setResult] = useState(null)
  const [saved, setSaved] = useState(false)

  const BUDGETS = ['budget friendly', 'medium', 'luxury']
  const TRAVELERS = ['solo', 'couple', 'family', 'friends']
  const INTERESTS = ['culture', 'food', 'adventure', 'nature', 'nightlife', 'relaxation', 'shopping', 'history']

  async function handleGenerate(e) {
    e.preventDefault()
    setError(null)
    setResult(null)
    setSaved(false)
    if (!destination.trim()) {
      setError('Please enter a destination.')
      return
    }
    setLoading(true)
    try {
      const data = await api.post('/generate-trip', {
        city: destination.trim(),
        numberOfDays: days,
        budget,
        travelers: traveler,
        interests,
      })
      setResult(data)
    } catch (err) {
      setError(err.message || 'Failed to generate trip.')
    } finally {
      setLoading(false)
    }
  }

  async function handleSave() {
    if (!result) return
    setLoading(true)
    try {
      await api.post('/trips', result)
      setSaved(true)
      navigate('/trips')
    } catch (err) {
      setError(err.message || 'Failed to save trip.')
      setLoading(false)
    }
  }

  return (
    <div>
      <h1 className="mb-2 text-3xl font-bold">Plan a trip</h1>
      <p className="mb-8 text-muted-foreground">Tell us about your trip and we'll build the itinerary.</p>
      <form onSubmit={handleGenerate} className="card max-w-2xl space-y-5">
        <div>
          <label className="label">Destination</label>
          <input className="input" placeholder="e.g. Tokyo, Paris, Bali" value={destination} onChange={(e) => setDestination(e.target.value)} />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="label">Number of days</label>
            <input type="number" min="1" max="30" className="input" value={days} onChange={(e) => setDays(Number(e.target.value))} />
          </div>
          <div>
            <label className="label">Travelers</label>
            <select className="input" value={traveler} onChange={(e) => setTraveler(e.target.value)}>
              {TRAVELERS.map((t) => (
                <option key={t} value={t}>{cap(t)}</option>
              ))}
            </select>
          </div>
        </div>
        <div>
          <label className="label">Budget</label>
          <div className="flex gap-2">
            {BUDGETS.map((b) => (
              <button
                key={b}
                type="button"
                className={`btn btn-sm ${budget === b ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => setBudget(b)}
              >
                {cap(b)}
              </button>
            ))}
          </div>
        </div>
        <div>
          <label className="label">Interests</label>
          <div className="flex flex-wrap gap-2">
            {INTERESTS.map((i) => (
              <button
                key={i}
                type="button"
                className={`btn btn-sm ${interests.includes(i) ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => setInterests(interests.includes(i) ? interests.filter((x) => x !== i) : [...interests, i])}
              >
                {cap(i)}
              </button>
            ))}
          </div>
        </div>
        <div id="error-slot">
          {error && !loading && !result && <div className="mt-4 error-box">{esc(error)}</div>}
        </div>
        <button type="submit" className="btn btn-primary w-full" disabled={loading}>
          {loading && !result ? 'Generating…' : 'Generate itinerary'}
        </button>
      </form>
      {error && !loading && result && <div className="mt-4 error-box">{esc(error)}</div>}
      {result && (
        <div className="mt-10">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-2xl font-bold">{esc(result.title)}</h2>
            <button className="btn btn-primary" disabled={loading || saved} onClick={handleSave}>
              {saved ? 'Saved' : 'Save trip'}
            </button>
          </div>
          <Itinerary itinerary={result} />
        </div>
      )}
    </div>
  )
}
