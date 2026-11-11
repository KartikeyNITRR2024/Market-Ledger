import { useState } from 'react'
import { useAuth } from '../auth.jsx'
import { errMsg } from '../api.js'
import { Alert } from '../components/ui.jsx'

export default function Login() {
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setError('')
    try { await login(email, password) }
    catch (err) { setError(err.response?.status === 401 || err.response?.status === 403 ? 'Email or password is incorrect.' : errMsg(err)) }
    finally { setBusy(false) }
  }

  return (
    <div className="grid min-h-screen md:grid-cols-2">
      <div className="hidden flex-col justify-between bg-ink p-12 text-white md:flex">
        <span className="text-lg font-semibold">Market Ledger</span>
        <div>
          <h2 className="max-w-md text-4xl font-semibold leading-tight">Millions of price rows, one clear picture.</h2>
          <p className="mt-4 max-w-sm text-slate-300">Upload market data, follow ingestion live, and explore it by ticker, date and price.</p>
        </div>
        <span className="text-sm text-slate-400">Admins upload and manage users. Viewers explore.</span>
      </div>
      <div className="flex items-center justify-center p-6">
        <form onSubmit={submit} className="w-full max-w-sm space-y-4">
          <h1 className="text-2xl font-semibold">Sign in</h1>
          <Alert>{error}</Alert>
          <div><label className="label" htmlFor="email">Email</label>
            <input id="email" type="email" required autoComplete="username" className="input" value={email} onChange={(e) => setEmail(e.target.value)} /></div>
          <div><label className="label" htmlFor="pw">Password</label>
            <input id="pw" type="password" required autoComplete="current-password" className="input" value={password} onChange={(e) => setPassword(e.target.value)} /></div>
          <button className="btn-primary w-full" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
        </form>
      </div>
    </div>
  )
}
