import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { IngestionErrors, Tasks, errMsg } from '../api.js'
import { Alert, PageTitle, Spinner } from '../components/ui.jsx'
import { fmt } from '../hooks.js'

const tone = { PENDING: 'bg-slate-400', PROCESSING: 'bg-brass', COMPLETED: 'bg-gain', FAILED: 'bg-loss' }

const Pager = ({ page, setPage, size, setSize, totalPages, sizes = [10, 20, 50, 100] }) => (
  <div className="flex flex-wrap items-center justify-between gap-3 border-t border-line p-3 text-sm">
    <label className="flex items-center gap-2">Rows
      <select className="input !w-auto" value={size} onChange={(e) => { setSize(+e.target.value); setPage(0) }}>
        {sizes.map((n) => <option key={n}>{n}</option>)}
      </select>
    </label>
    <div className="flex items-center gap-2">
      <button className="btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
      <span>Page {totalPages ? page + 1 : 0} of {fmt.int(totalPages)}</span>
      <button className="btn-ghost" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>Next</button>
    </div>
  </div>
)

/* ---------- Page 1: all tasks (file name + task id) ---------- */
function TaskList() {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    Tasks.list({ page, size })
      .then((d) => { setData(d); setError('') })
      .catch((e) => setError(errMsg(e)))
  }, [page, size])

  const rows = data?.content || []

  return (
    <>
      <PageTitle title="Ingestion errors" sub="Choose an upload to see the rows that could not be imported." />
      <Alert>{error}</Alert>
      <div className="panel overflow-hidden">
        {!data ? <Spinner /> : rows.length === 0 ? (
          <p className="p-8 text-center text-sm text-slate-600">No uploads yet.</p>
        ) : (
          <ul className="divide-y divide-line">
            {rows.map((t) => (
              <li key={t.id}>
                <Link to={`/errors/${t.id}`} className="flex flex-wrap items-center justify-between gap-2 p-4 hover:bg-slate-50">
                  <div className="min-w-0 flex-1">
                    <div className="truncate font-medium">{t.fileName}</div>
                    <div className="break-all text-xs text-slate-500">{t.id}</div>
                  </div>
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium text-white ${tone[t.status] || 'bg-slate-400'}`}>{t.status}</span>
                  <span className="text-sm text-brass">View errors →</span>
                </Link>
              </li>
            ))}
          </ul>
        )}
        <Pager page={page} setPage={setPage} size={size} setSize={setSize} totalPages={data?.totalPages || 0} sizes={[10, 20, 50]} />
      </div>
    </>
  )
}

/* ---------- Page 2: errors for one task ---------- */
function TaskErrors({ taskId }) {
  const [task, setTask] = useState(null)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => { Tasks.get(taskId).then(setTask).catch(() => {}) }, [taskId])

  const load = useCallback(() => {
    setLoading(true)
    return IngestionErrors.byTask(taskId, { page, size })
      .then((d) => { setData(d); setError('') })
      .catch((e) => setError(errMsg(e)))
      .finally(() => setLoading(false))
  }, [taskId, page, size])
  useEffect(() => { load() }, [load])

  const rows = data?.content || []

  return (
    <>
      <Link to="/errors" className="mb-3 inline-block text-sm underline">← All uploads</Link>
      <PageTitle
        title={task?.fileName || 'Task errors'}
        sub={data ? `${fmt.int(data.totalElements)} rows failed validation` : taskId}
      >
        <button className="btn-ghost" onClick={load}>Refresh</button>
      </PageTitle>
      <p className="-mt-4 mb-4 break-all text-xs text-slate-500">Task {taskId}</p>
      <Alert>{error}</Alert>

      <div className="panel overflow-hidden">
        {loading && !data ? <Spinner /> : rows.length === 0 ? (
          <p className="p-8 text-center text-sm text-slate-600">No errors for this upload. Every row was imported.</p>
        ) : (
          <>
            {/* Desktop table */}
            <div className={`hidden overflow-x-auto md:block ${loading ? 'opacity-60' : ''}`}>
              <table className="w-full text-sm">
                <thead className="bg-slate-50 text-left">
                  <tr>
                    <th className="px-4 py-3 font-medium">Row #</th>
                    <th className="px-4 py-3 font-medium">Reason</th>
                    <th className="px-4 py-3 font-medium">Raw row</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.id} className="border-t border-line align-top hover:bg-slate-50">
                      <td className="px-4 py-2.5 font-medium">{fmt.int(r.rowNum)}</td>
                      <td className="px-4 py-2.5 text-loss">{r.reason}</td>
                      <td className="max-w-md px-4 py-2.5"><code className="block break-all text-xs text-slate-700">{r.rawRow}</code></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {/* Mobile cards */}
            <ul className={`divide-y divide-line md:hidden ${loading ? 'opacity-60' : ''}`}>
              {rows.map((r) => (
                <li key={r.id} className="space-y-1 p-4">
                  <div className="text-sm font-medium">Row {fmt.int(r.rowNum)}</div>
                  <div className="text-sm text-loss">{r.reason}</div>
                  <code className="block break-all rounded bg-slate-50 p-2 text-xs text-slate-700">{r.rawRow}</code>
                </li>
              ))}
            </ul>
          </>
        )}
        <Pager page={page} setPage={setPage} size={size} setSize={setSize} totalPages={data?.totalPages || 0} />
      </div>
    </>
  )
}

export default function IngestionErrorsPage() {
  const { taskId } = useParams()
  return taskId ? <TaskErrors taskId={taskId} /> : <TaskList />
}