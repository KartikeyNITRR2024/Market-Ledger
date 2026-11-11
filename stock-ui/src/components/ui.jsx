export const PageTitle = ({ title, sub, children }) => (
  <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
    <div>
      <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
      {sub && <p className="mt-1 text-sm text-slate-600">{sub}</p>}
    </div>
    {children}
  </div>
)

export const Alert = ({ children, tone = 'error' }) =>
  children ? (
    <div role="alert" className={`rounded-md border px-3 py-2 text-sm ${tone === 'error' ? 'border-red-200 bg-red-50 text-red-800' : 'border-emerald-200 bg-emerald-50 text-emerald-800'}`}>{children}</div>
  ) : null

export const Spinner = () => (
  <div className="flex justify-center p-8"><div className="h-6 w-6 animate-spin rounded-full border-2 border-line border-t-ink" /></div>
)

export const ProgressBar = ({ value, tone = 'bg-ink' }) => (
  <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200" role="progressbar" aria-valuenow={value} aria-valuemin={0} aria-valuemax={100}>
    <div className={`h-full ${tone} transition-all`} style={{ width: `${Math.min(100, value)}%` }} />
  </div>
)
