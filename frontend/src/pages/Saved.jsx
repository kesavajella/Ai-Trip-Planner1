import { useState, useEffect } from 'react'
import { api } from '../api'
import { esc, cap } from '../utils'

const demo = [
  { name: 'Eiffel Tower', location: 'Paris, France', rating: 4.7, category: 'Landmark' },
  { name: 'Fushimi Inari', location: 'Kyoto, Japan', rating: 4.8, category: 'Temple' },
  { name: 'Sagrada Família', location: 'Barcelona, Spain', rating: 4.9, category: 'Architecture' },
]

export default function Saved() {
  return (
    <div>
      <h1 className="mb-6 text-3xl font-bold">Saved places</h1>
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {demo.map((p, idx) => (
          <div key={idx} className="card">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold">{esc(p.name)}</h3>
              <span className="badge">★ {p.rating}</span>
            </div>
            <p className="text-sm text-muted-foreground">{esc(p.location)}</p>
            <span className="badge mt-3">{esc(p.category)}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
