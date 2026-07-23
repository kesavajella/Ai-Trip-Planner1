import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { esc } from '../utils'

export default function Landing() {
  const features = [
    { title: 'Instant Itineraries', body: 'Get a full multi-day plan with activities, dining, and transport in one click.' },
    { title: 'Budget Aware', body: 'Pick a budget tier and we balance accommodation, food, and activities for you.' },
    { title: 'Save & Track', body: 'Keep all your trips organized and track upcoming days and spend at a glance.' },
  ]

  return (
    <div>
      <section className="relative overflow-hidden">
        <div className="mx-auto max-w-6xl px-4 py-24 text-center">
          <span className="badge mb-4">AI-Powered Travel Planning</span>
          <h1 className="mx-auto max-w-3xl text-4xl font-bold leading-tight sm:text-6xl">
            Plan smarter trips with <span className="text-primary-60">IntelliTrip</span>
          </h1>
          <p className="mx-auto mt-6 max-w-2xl text-lg text-muted-foreground">
            Generate personalized day-by-day itineraries in seconds. Tailor every trip to
            your budget, interests, and travel style.
          </p>
          <div className="mt-8 flex items-center justify-center gap-4">
            <a href="#/signup" className="btn btn-primary btn-lg">Get started</a>
            <a href="#/login" className="btn btn-outline btn-lg">Sign in</a>
          </div>
        </div>
      </section>
      <section className="mx-auto grid max-w-6xl gap-6 px-4 pb-24 md:grid-cols-3">
        {features.map((f, idx) => (
          <div key={idx} className="card">
            <h3 className="text-lg font-semibold">{f.title}</h3>
            <p className="mt-2 text-sm text-muted-foreground">{f.body}</p>
          </div>
        ))}
      </section>
    </div>
  )
}
