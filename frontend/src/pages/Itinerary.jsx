import { money } from '../utils'

export default function Itinerary({ itinerary }) {
  const totalCost = itinerary.costBreakdown != null
    ? Object.values(itinerary.costBreakdown).reduce((a, b) => a + (b || 0), 0)
    : itinerary.totalBudget != null
      ? itinerary.totalBudget
      : null

  return (
    <div className="space-y-6">
      {itinerary.overview && <p className="text-muted-foreground">{itinerary.overview}</p>}
      {(itinerary.days || []).map((day) => (
        <div key={day.day} className="card">
          <h3 className="mb-3 text-lg font-semibold">Day {day.day}: {day.title}</h3>
          {day.theme && <p className="mb-3 text-sm text-muted-foreground">{day.theme}</p>}
          <ol className="space-y-3">
            {(day.activities || []).map((a, idx) => (
              <li key={idx} className="flex gap-4">
                <span className="shrink-0 rounded-md bg-muted px-2 py-1 text-xs font-medium text-muted-foreground">{a.time}</span>
                <div>
                  <p className="font-medium">{a.activity}</p>
                  <p className="text-sm text-muted-foreground">{a.description}</p>
                  {a.estimatedCost != null && <p className="text-xs text-muted-foreground">~{money(a.estimatedCost)}</p>}
                </div>
              </li>
            ))}
          </ol>
        </div>
      ))}
      {itinerary.accommodation && itinerary.accommodation.length > 0 && (
        <div className="card">
          <h3 className="mb-3 text-lg font-semibold">Where to stay</h3>
          <div className="grid gap-3 sm:grid-cols-2">
            {itinerary.accommodation.map((h, idx) => (
              <div key={idx} className="rounded-lg border border-border p-3">
                <div className="flex items-center justify-between">
                  <p className="font-medium">{h.name}</p>
                  <span className="text-sm text-muted-foreground">{h.rating}</span>
                </div>
                <p className="text-sm text-muted-foreground">{h.type} · {money(h.price)}/night</p>
                <p className="mt-1 text-xs text-muted-foreground">{h.description}</p>
              </div>
            ))}
          </div>
        </div>
      )}
      {itinerary.costBreakdown && (
        <div className="card">
          <h3 className="mb-3 text-lg font-semibold">Estimated cost breakdown</h3>
          <div className="space-y-2 text-sm">
            {row('Accommodation', itinerary.costBreakdown.accommodation)}
            {row('Food', itinerary.costBreakdown.food)}
            {row('Activities', itinerary.costBreakdown.activities)}
            {row('Transportation', itinerary.costBreakdown.transportation)}
            {row('Other', itinerary.costBreakdown.other)}
            {totalCost != null && (
              <div className="mt-2 flex justify-between border-t pt-2 font-semibold"><span>Total</span><span>{money(totalCost)}</span></div>
            )}
          </div>
        </div>
      )}
      {itinerary.tips && itinerary.tips.length > 0 && (
        <div className="card">
          <h3 className="mb-3 text-lg font-semibold">Tips</h3>
          <ul className="list-inside list-disc space-y-1 text-sm text-muted-foreground">
            {itinerary.tips.map((t, idx) => <li key={idx}>{t}</li>)}
          </ul>
        </div>
      )}
    </div>
  )
}

function row(label, value) {
  return (
    <div className="flex justify-between">
      <span className="text-muted-foreground">{label}</span>
      <span>{money(value || 0)}</span>
    </div>
  )
}
