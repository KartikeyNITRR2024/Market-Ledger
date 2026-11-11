import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Stocks, errMsg } from '../api.js'
import { Alert, PageTitle, Spinner } from '../components/ui.jsx'
import { fmt, useDebounce } from '../hooks.js'

const COLS = [
  { key: 'ticker', label: 'Ticker' },
  { key: 'tradeDate', label: 'Date' },
  { key: 'open', label: 'Open', num: true },
  { key: 'high', label: 'High', num: true },
  { key: 'low', label: 'Low', num: true },
  { key: 'close', label: 'Close', num: true },
  { key: 'adjClose', label: 'Adj close', num: true },
  { key: 'volume', label: 'Volume', num: true },
  { key: 'dividends', label: 'Dividends', num: true },
  { key: 'stockSplits', label: 'Splits', num: true },
]

const EMPTY = {
  ticker: '', startDate: '', endDate: '',
  minClosePrice: '', maxClosePrice: '',
  minAdjClosePrice: '', maxAdjClosePrice: '',
  minVolume: '', maxVolume: '',
  minDividends: '', minStockSplits: '',
  hasDividends: false, hasStockSplits: false,
}

const NumField = ({ label, value, onChange, step = 'any' }) => (
  <div>
    <label className="label">{label}</label>
    <input type="number" min="0" step={step} className="input" value={value} onChange={onChange} />
  </div>
)

export default function StockList() {
  const [sp] = useSearchParams()
  const taskId = sp.get('task') || ''
  const [f, setF] = useState(EMPTY)
  const [sorts, setSorts] = useState([{ key: 'tradeDate', dir: 'desc' }])
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showFilters, setShowFilters] = useState(false)

  // f only changes when the user edits a filter, so debouncing it is safe
  const df = useDebounce(f)

  useEffect(() => {
    let live = true
    setLoading(true)
    Stocks.list({
      ticker: df.ticker, startDate: df.startDate, endDate: df.endDate,
      minClosePrice: df.minClosePrice, maxClosePrice: df.maxClosePrice,
      minAdjClosePrice: df.minAdjClosePrice, maxAdjClosePrice: df.maxAdjClosePrice,
      minVolume: df.minVolume, maxVolume: df.maxVolume,
      minDividends: df.minDividends, minStockSplits: df.minStockSplits,
      hasDividends: df.hasDividends || '', hasStockSplits: df.hasStockSplits || '',
      uploadTaskId: taskId,
      page, size, sort: sorts.map((s) => `${s.key},${s.dir}`),
    })
      .then((d) => live && (setData(d), setError('')))
      .catch((e) => live && setError(errMsg(e)))
      .finally(() => live && setLoading(false))
    return () => { live = false }
  }, [df, taskId, page, size, sorts])

  const set = (k) => (e) => { setF({ ...f, [k]: e.target.value }); setPage(0) }
  const setBool = (k) => (e) => { setF({ ...f, [k]: e.target.checked }); setPage(0) }
  const reset = () => { setF(EMPTY); setPage(0) }

  const activeCount = Object.entries(f).filter(([k, v]) => k !== 'ticker' && v !== '' && v !== false).length

  // Click = sort by this column (asc -> desc -> off). Shift/Ctrl-click = add as extra sort column.
  const toggleSort = (key, multi) => {
    const cur = sorts.find((s) => s.key === key)
    const next = !cur ? { key, dir: 'asc' } : cur.dir === 'asc' ? { key, dir: 'desc' } : null
    if (multi) setSorts(next ? (cur ? sorts.map((s) => (s.key === key ? next : s)) : [...sorts, next]) : sorts.filter((s) => s.key !== key))
    else setSorts(next ? [next] : [])
    setPage(0)
  }
  const arrow = (key) => {
    const i = sorts.findIndex((s) => s.key === key)
    return i < 0 ? '' : (sorts[i].dir === 'asc' ? '▲' : '▼') + (sorts.length > 1 ? i + 1 : '')
  }

  const rows = data?.content || []
  const totalPages = data?.totalPages || 0
  const num4 = (v) => fmt.num(v, 4)

  return (
    <>
      <PageTitle title="Stock data" sub={data ? `${fmt.int(data.totalElements)} matching rows` : 'Imported price history'} />

      <div className="panel mb-4 p-4">
        <div className="flex gap-2">
          <input className="input" placeholder="Search ticker, e.g. AAPL" aria-label="Search ticker" value={f.ticker} onChange={set('ticker')} />
          <button className="btn-ghost" aria-expanded={showFilters} onClick={() => setShowFilters(!showFilters)}>
            Filters{activeCount > 0 && <span className="rounded-full bg-brass px-1.5 text-xs text-white">{activeCount}</span>}
          </button>
          <button className="btn-ghost hidden sm:inline-flex" onClick={reset}>Reset</button>
        </div>

        {showFilters && (
          <div className="mt-4 space-y-4">
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              <div><label className="label">From</label><input type="date" className="input" value={f.startDate} onChange={set('startDate')} /></div>
              <div><label className="label">To</label><input type="date" className="input" value={f.endDate} onChange={set('endDate')} /></div>
            </div>

            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              <NumField label="Min close" value={f.minClosePrice} onChange={set('minClosePrice')} />
              <NumField label="Max close" value={f.maxClosePrice} onChange={set('maxClosePrice')} />
              <NumField label="Min adj close" value={f.minAdjClosePrice} onChange={set('minAdjClosePrice')} />
              <NumField label="Max adj close" value={f.maxAdjClosePrice} onChange={set('maxAdjClosePrice')} />
              <NumField label="Min volume" value={f.minVolume} onChange={set('minVolume')} step="1" />
              <NumField label="Max volume" value={f.maxVolume} onChange={set('maxVolume')} step="1" />
              <NumField label="Min dividends" value={f.minDividends} onChange={set('minDividends')} />
              <NumField label="Min stock splits" value={f.minStockSplits} onChange={set('minStockSplits')} />
            </div>

            <div className="flex flex-wrap gap-x-6 gap-y-2 text-sm">
              <label className="flex items-center gap-2"><input type="checkbox" checked={f.hasDividends} onChange={setBool('hasDividends')} /> Paid a dividend</label>
              <label className="flex items-center gap-2"><input type="checkbox" checked={f.hasStockSplits} onChange={setBool('hasStockSplits')} /> Had a stock split</label>
            </div>

            <button className="btn-ghost w-full sm:hidden" onClick={reset}>Reset filters</button>
          </div>
        )}

        {/* Mobile sort picker */}
        <div className="mt-3 flex gap-2 md:hidden">
          <select className="input" aria-label="Sort by" value={sorts[0]?.key || ''} onChange={(e) => setSorts(e.target.value ? [{ key: e.target.value, dir: sorts[0]?.dir || 'desc' }] : [])}>
            <option value="">No sorting</option>
            {COLS.map((c) => <option key={c.key} value={c.key}>Sort: {c.label}</option>)}
          </select>
          <button className="btn-ghost" aria-label="Toggle sort direction" onClick={() => sorts[0] && setSorts([{ ...sorts[0], dir: sorts[0].dir === 'asc' ? 'desc' : 'asc' }])}>{sorts[0]?.dir === 'asc' ? '▲' : '▼'}</button>
        </div>
      </div>

      {taskId && (
        <div className="mb-3 flex items-center gap-2 text-sm">
          <span className="rounded-full bg-amber-100 px-3 py-1">Showing upload {taskId.slice(0, 8)}</span>
          <Link to="/stocks" className="underline">Clear</Link>
        </div>
      )}

      <Alert>{error}</Alert>

      <div className="panel overflow-hidden">
        {loading && !data ? <Spinner /> : rows.length === 0 ? (
          <p className="p-8 text-center text-sm text-slate-600">No rows match these filters. Widen the date range or clear a filter.</p>
        ) : (
          <>
            {/* Desktop table */}
            <div className={`hidden overflow-x-auto md:block ${loading ? 'opacity-60' : ''}`}>
              <table className="w-full whitespace-nowrap text-sm">
                <thead className="bg-slate-50 text-left">
                  <tr>
                    {COLS.map((c) => (
                      <th key={c.key} className={`px-4 py-3 font-medium ${c.num ? 'text-right' : ''}`}>
                        <button className="hover:text-brass" title="Shift-click to sort by several columns" onClick={(e) => toggleSort(c.key, e.shiftKey || e.ctrlKey)}>
                          {c.label} <span className="text-xs text-brass">{arrow(c.key)}</span>
                        </button>
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.id} className="border-t border-line hover:bg-slate-50">
                      <td className="px-4 py-2.5 font-medium">{r.ticker}</td>
                      <td className="px-4 py-2.5">{r.tradeDate}</td>
                      <td className="px-4 py-2.5 text-right">{fmt.num(r.open)}</td>
                      <td className="px-4 py-2.5 text-right">{fmt.num(r.high)}</td>
                      <td className="px-4 py-2.5 text-right">{fmt.num(r.low)}</td>
                      <td className={`px-4 py-2.5 text-right font-medium ${r.close >= r.open ? 'text-gain' : 'text-loss'}`}>{fmt.num(r.close)}</td>
                      <td className="px-4 py-2.5 text-right">{fmt.num(r.adjClose)}</td>
                      <td className="px-4 py-2.5 text-right">{fmt.int(r.volume)}</td>
                      <td className="px-4 py-2.5 text-right">{r.dividends > 0 ? <span className="font-medium text-gain">{num4(r.dividends)}</span> : num4(r.dividends)}</td>
                      <td className="px-4 py-2.5 text-right">{r.stockSplits > 0 ? <span className="font-medium text-brass">{num4(r.stockSplits)}</span> : num4(r.stockSplits)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Mobile cards */}
            <ul className={`divide-y divide-line md:hidden ${loading ? 'opacity-60' : ''}`}>
              {rows.map((r) => (
                <li key={r.id} className="p-4">
                  <div className="flex justify-between"><b>{r.ticker}</b><span className="text-sm text-slate-600">{r.tradeDate}</span></div>
                  <div className="mt-2 grid grid-cols-5 gap-2 text-xs">
                    {[['Open', r.open], ['High', r.high], ['Low', r.low], ['Close', r.close], ['Adj', r.adjClose]].map(([l, v]) => (
                      <div key={l}><div className="text-slate-500">{l}</div><div className={l === 'Close' ? (r.close >= r.open ? 'font-medium text-gain' : 'font-medium text-loss') : ''}>{fmt.num(v)}</div></div>
                    ))}
                  </div>
                  <div className="mt-2 flex flex-wrap gap-x-4 text-xs text-slate-600">
                    <span>Vol {fmt.int(r.volume)}</span>
                    <span>Div {num4(r.dividends)}</span>
                    <span>Split {num4(r.stockSplits)}</span>
                  </div>
                </li>
              ))}
            </ul>
          </>
        )}

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-line p-3 text-sm">
          <label className="flex items-center gap-2">Rows
            <select className="input !w-auto" value={size} onChange={(e) => { setSize(+e.target.value); setPage(0) }}>
              {[10, 20, 50, 100].map((n) => <option key={n}>{n}</option>)}
            </select>
          </label>
          <div className="flex items-center gap-2">
            <button className="btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
            <span>Page {totalPages ? page + 1 : 0} of {fmt.int(totalPages)}</span>
            <button className="btn-ghost" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>Next</button>
          </div>
        </div>
      </div>
    </>
  )
}