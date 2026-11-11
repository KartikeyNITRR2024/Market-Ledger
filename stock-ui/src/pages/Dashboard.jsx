import { useEffect, useState } from 'react'
import { Bar, BarChart, CartesianGrid, Line, LineChart, ReferenceDot, ResponsiveContainer, Tooltip, XAxis, YAxis, Legend } from 'recharts'
import { Stocks, errMsg } from '../api.js'
import { Alert, PageTitle, Spinner } from '../components/ui.jsx'
import { fmt, useDebounce } from '../hooks.js'

const money = (n) => (n == null ? '–' : '$' + fmt.compact(n))
const pct = (n) => (n == null ? '–' : `${n > 0 ? '+' : ''}${fmt.num(n)}%`)

const ANOMALY = {
  PRICE_AND_VOLUME: { label: 'Price + volume', cls: 'bg-red-100 text-red-800' },
  PRICE_MOVE: { label: 'Price move', cls: 'bg-orange-100 text-orange-800' },
  VOLUME_SPIKE: { label: 'Volume spike', cls: 'bg-amber-100 text-amber-800' },
}

const Kpi = ({ label, value, sub, tone }) => (
  <div className="panel p-4">
    <div className="text-xs text-slate-600">{label}</div>
    <div className={`mt-1 text-2xl font-semibold ${tone || ''}`}>{value}</div>
    {sub && <div className="mt-1 text-xs text-slate-500">{sub}</div>}
  </div>
)

const Card = ({ title, note, children, className = '' }) => (
  <section className={`panel p-4 ${className}`}>
    <h2 className="text-sm font-semibold">{title}</h2>
    {note && <p className="mb-3 mt-0.5 text-xs text-slate-500">{note}</p>}
    {!note && <div className="mb-3" />}
    {children}
  </section>
)

const Empty = ({ children }) => <p className="py-6 text-center text-sm text-slate-500">{children}</p>

function Performers({ title, rows, up, onPick }) {
  return (
    <Card title={title} note="Change in adjusted close over the selected range">
      {rows.length === 0 ? <Empty>Not enough data.</Empty> : (
        <ul className="divide-y divide-line">
          {rows.map((r) => (
            <li key={r.ticker}>
              <button onClick={() => onPick(r.ticker)} className="flex w-full items-center justify-between gap-3 py-2 text-left hover:bg-slate-50">
                <span className="font-medium">{r.ticker}</span>
                <span className="text-xs text-slate-500">{fmt.num(r.firstClose)} → {fmt.num(r.lastClose)}</span>
                <span className={`w-20 text-right font-medium ${up ? 'text-gain' : 'text-loss'}`}>{pct(r.changePct)}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}

export default function Dashboard() {
  const [f, setF] = useState({ ticker: '', startDate: '', endDate: '' })
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const dTicker = useDebounce(f.ticker)

  useEffect(() => {
    let live = true
    setLoading(true)
    Stocks.analytics({ ticker: dTicker, startDate: f.startDate, endDate: f.endDate })
      .then((d) => live && (setData(d), setError('')))
      .catch((e) => live && setError(errMsg(e)))
      .finally(() => live && setLoading(false))
    return () => { live = false }
  }, [dTicker, f.startDate, f.endDate])

  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })
  const pick = (ticker) => setF({ ...f, ticker })
  const reset = () => setF({ ticker: '', startDate: '', endDate: '' })

  const s = data?.summary
  const series = data?.priceSeries || []
  const dates = new Set(series.map((p) => p.date))
  // Markers can only be placed on the line when each point is a single day
  const markers = data?.granularity === 'day'
    ? (data.anomalies || []).filter((a) => a.ticker === data.priceTicker && dates.has(a.date))
    : []
  const closeByDate = Object.fromEntries(series.map((p) => [p.date, p.close]))
  const unitLabel = { day: 'daily', week: 'weekly', month: 'monthly' }[data?.granularity] || ''

  return (
    <>
      <PageTitle title="Dashboard" sub="Trend, activity, winners and unusual days. Everything recalculates when you change a filter.">
        <button className="btn-ghost" onClick={reset}>Reset filters</button>
      </PageTitle>

      <div className="panel mb-6 grid grid-cols-1 gap-3 p-4 sm:grid-cols-3">
        <div><label className="label">Ticker</label><input className="input" placeholder="All tickers" value={f.ticker} onChange={set('ticker')} /></div>
        <div><label className="label">From</label><input type="date" className="input" value={f.startDate} onChange={set('startDate')} /></div>
        <div><label className="label">To</label><input type="date" className="input" value={f.endDate} onChange={set('endDate')} /></div>
      </div>

      <Alert>{error}</Alert>
      {loading && !data ? <Spinner /> : data && (s.totalRows === 0 ? (
        <div className="panel"><Empty>No data for these filters. Clear the ticker or widen the date range.</Empty></div>
      ) : (
        <div className={`space-y-4 ${loading ? 'opacity-60' : ''}`}>
          {/* Summary cards */}
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-5">
            <Kpi label="Rows" value={fmt.compact(s.totalRows)} sub={`${fmt.int(s.totalRows)} price records`} />
            <Kpi label="Tickers" value={fmt.int(s.tickerCount)} />
            <Kpi label="Average close" value={'$' + fmt.num(s.averageClose)} />
            <Kpi label="Dollar volume" value={money(s.totalDollarVolume)} sub={`${fmt.compact(s.totalVolume)} shares`} />
            <Kpi label="Anomaly days" value={fmt.int(s.anomalyCount)} sub="Beyond 3 std. deviations" tone={s.anomalyCount > 0 ? 'text-loss' : ''} />
          </div>

          {/* Price trend */}
          <Card
            title={`${data.priceTicker || 'Price'} — close price with moving averages`}
            note={`${unitLabel[0]?.toUpperCase() || ''}${unitLabel.slice(1)} average close, 20 and 50 period moving averages${!f.ticker ? '. Showing the most traded ticker; pick one below or type it above.' : '.'}${markers.length ? ' Red dots mark anomalies.' : ''}`}
          >
            <div className="h-64 md:h-80">
              <ResponsiveContainer>
                <LineChart data={series}>
                  <CartesianGrid stroke="#e5e9f0" vertical={false} />
                  <XAxis dataKey="date" tick={{ fontSize: 11 }} minTickGap={50} />
                  <YAxis tick={{ fontSize: 11 }} width={50} domain={['auto', 'auto']} />
                  <Tooltip formatter={(v) => (v == null ? '–' : fmt.num(v))} />
                  <Legend />
                  <Line type="monotone" dataKey="close" name="Close" stroke="#12213a" dot={false} strokeWidth={2} />
                  <Line type="monotone" dataKey="ma20" name="MA 20" stroke="#b7791f" dot={false} strokeWidth={1.5} connectNulls={false} />
                  <Line type="monotone" dataKey="ma50" name="MA 50" stroke="#0f8a5f" dot={false} strokeWidth={1.5} connectNulls={false} />
                  {markers.map((a) => (
                    <ReferenceDot key={a.date + a.type} x={a.date} y={closeByDate[a.date]} r={5} fill="#c2410c" stroke="#fff" />
                  ))}
                </LineChart>
              </ResponsiveContainer>
            </div>
          </Card>

          {/* Money flow + winners / losers */}
          <div className="grid gap-4 lg:grid-cols-2">
            <Card title="Top tickers by dollar volume" note="Close price × volume. Click a bar to focus that ticker.">
              <div className="h-72">
                <ResponsiveContainer>
                  <BarChart data={data.topTickers} layout="vertical" margin={{ left: 8 }}>
                    <CartesianGrid stroke="#e5e9f0" horizontal={false} />
                    <XAxis type="number" tick={{ fontSize: 11 }} tickFormatter={money} />
                    <YAxis type="category" dataKey="ticker" tick={{ fontSize: 11 }} width={48} />
                    <Tooltip formatter={(v) => money(v)} />
                    <Bar dataKey="dollarVolume" name="Dollar volume" fill="#12213a" radius={[0, 3, 3, 0]} cursor="pointer" onClick={(d) => pick(d.ticker)} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </Card>

            {f.ticker ? (
              <Card title="Gainers and losers" note="Available when no ticker is selected.">
                <Empty>Clear the ticker filter to compare stocks against each other.</Empty>
              </Card>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">
                <Performers title="Top gainers" rows={data.gainers} up onPick={pick} />
                <Performers title="Top losers" rows={data.losers} onPick={pick} />
              </div>
            )}
          </div>

          {/* Anomalies */}
          <Card title="Unusual days" note="Daily return or volume far outside that ticker's normal range. Most extreme first.">
            {data.anomalies.length === 0 ? <Empty>No anomalies in this range.</Empty> : (
              <>
                {/* Desktop table */}
                <div className="hidden overflow-x-auto md:block">
                  <table className="w-full text-sm">
                    <thead className="text-left text-xs text-slate-500">
                      <tr><th className="py-2 font-medium">Date</th><th className="font-medium">Ticker</th><th className="font-medium">Type</th>
                        <th className="text-right font-medium">Daily return</th><th className="text-right font-medium">Volume</th></tr>
                    </thead>
                    <tbody>
                      {data.anomalies.map((a) => (
                        <tr key={a.date + a.ticker} className="border-t border-line">
                          <td className="py-2">{a.date}</td>
                          <td className="font-medium">{a.ticker}</td>
                          <td><span className={`rounded-full px-2 py-0.5 text-xs font-medium ${ANOMALY[a.type]?.cls}`}>{ANOMALY[a.type]?.label || a.type}</span></td>
                          <td className={`text-right ${a.returnPct > 0 ? 'text-gain' : a.returnPct < 0 ? 'text-loss' : ''}`}>{pct(a.returnPct)}</td>
                          <td className="text-right">{fmt.int(a.volume)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {/* Mobile list */}
                <ul className="divide-y divide-line md:hidden">
                  {data.anomalies.map((a) => (
                    <li key={a.date + a.ticker} className="space-y-1 py-3">
                      <div className="flex items-center justify-between">
                        <span><b>{a.ticker}</b> <span className="text-sm text-slate-600">{a.date}</span></span>
                        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${ANOMALY[a.type]?.cls}`}>{ANOMALY[a.type]?.label || a.type}</span>
                      </div>
                      <div className="flex justify-between text-xs text-slate-600">
                        <span>Return <span className={a.returnPct > 0 ? 'text-gain' : a.returnPct < 0 ? 'text-loss' : ''}>{pct(a.returnPct)}</span></span>
                        <span>Volume {fmt.int(a.volume)}</span>
                      </div>
                    </li>
                  ))}
                </ul>
                {s.anomalyCount > data.anomalies.length && (
                  <p className="mt-3 text-xs text-slate-500">Showing the {data.anomalies.length} most extreme of {fmt.int(s.anomalyCount)}.</p>
                )}
              </>
            )}
          </Card>
        </div>
      ))}
    </>
  )
}