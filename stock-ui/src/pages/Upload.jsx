import { useEffect, useRef, useState } from 'react'
import { Files, Tasks, errMsg } from '../api.js'
import { Alert, PageTitle, ProgressBar } from '../components/ui.jsx'
import { fmt } from '../hooks.js'

const DONE = ['COMPLETED', 'FAILED']
const tone = { PENDING: 'bg-slate-400', PROCESSING: 'bg-brass', COMPLETED: 'bg-gain', FAILED: 'bg-loss' }

function TaskRow({ task }) {
  const [s, setS] = useState(task)
  const [err, setErr] = useState('')

  useEffect(() => {
    if (DONE.includes(s.status)) return
    const t = setInterval(async () => {
      try {
        const latest = await Tasks.get(task.id)
        setS((p) => ({ ...p, ...latest }))
        setErr('')
      } catch (e) { setErr(errMsg(e)) }
    }, 1500)
    return () => clearInterval(t)
  }, [task.id, s.status])

  const total = s.totalRows || 0
  const done = s.processedRows || 0
  const pct = s.status === 'COMPLETED' ? 100 : total ? (done / total) * 100 : 0

  return (
    <li className="panel p-4">
      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <span className="truncate font-medium">{task.fileName}</span>
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium text-white ${tone[s.status] || 'bg-slate-400'}`}>{s.status}</span>
      </div>
      <ProgressBar value={pct} tone={tone[s.status]} />
      <div className="mt-2 flex flex-wrap justify-between gap-x-4 text-xs text-slate-600">
        <span>{fmt.int(done)} / {total ? fmt.int(total) : '?'} rows</span>
        {s.failedRows > 0 && <span className="text-loss">{fmt.int(s.failedRows)} rows skipped</span>}
        <span>Task {task.id.slice(0, 8)}</span>
      </div>
      {(s.message || err) && <p className="mt-2 text-xs text-loss">{s.message || err}</p>}
    </li>
  )
}

export default function Upload() {
  const [file, setFile] = useState(null)
  const [pct, setPct] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [tasks, setTasks] = useState([])
  const [drag, setDrag] = useState(false)
  const input = useRef()

  // Load upload history (newest first, as sorted by the backend)
  useEffect(() => {
    Tasks.list({ page: 0, size: 20 })
      .then((d) => setTasks(d.content || []))
      .catch((e) => setError(errMsg(e)))
  }, [])

  const pick = (f) => {
    setError('')
    if (f && !f.name.toLowerCase().endsWith('.csv')) return setError('Choose a .csv file.')
    setFile(f)
  }

  const submit = async () => {
    setBusy(true); setError(''); setPct(0)
    try {
      const t = await Files.upload(file, setPct)
      setTasks((p) => [t, ...p])
      setFile(null)
      if (input.current) input.current.value = ''
    } catch (e) { setError(errMsg(e)) }
    finally { setBusy(false) }
  }

  return (
    <>
      <PageTitle title="Upload CSV" sub="Large files are sent in one request and processed in the background." />
      <div className="mx-auto max-w-3xl space-y-4">
        <Alert>{error}</Alert>
        <div
          onDragOver={(e) => { e.preventDefault(); setDrag(true) }}
          onDragLeave={() => setDrag(false)}
          onDrop={(e) => { e.preventDefault(); setDrag(false); pick(e.dataTransfer.files[0]) }}
          className={`panel border-2 border-dashed p-8 text-center ${drag ? 'border-brass bg-amber-50' : ''}`}
        >
          <p className="mb-3 text-sm text-slate-600">{file ? <><b>{file.name}</b> · {(file.size / 1048576).toFixed(1)} MB</> : 'Drop a CSV here or choose one'}</p>
          <input ref={input} id="csv" type="file" accept=".csv,text/csv" className="sr-only" onChange={(e) => pick(e.target.files[0])} />
          <label htmlFor="csv" className="btn-ghost cursor-pointer">Choose file</label>
        </div>
        {busy && <div><ProgressBar value={pct} /><p className="mt-1 text-xs text-slate-600">Sending file… {pct}%</p></div>}
        <button className="btn-primary w-full sm:w-auto" disabled={!file || busy} onClick={submit}>{busy ? 'Uploading…' : 'Upload and process'}</button>

        {tasks.length > 0 && (
          <section className="pt-4">
            <h2 className="mb-3 font-semibold">Upload history</h2>
            <ul className="space-y-3">{tasks.map((t) => <TaskRow key={t.id} task={t} />)}</ul>
          </section>
        )}
      </div>
    </>
  )
}