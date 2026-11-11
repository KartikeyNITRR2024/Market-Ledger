import { useEffect, useState } from 'react'
import { Users as UsersApi, errMsg } from '../api.js'
import { Alert, PageTitle, Spinner } from '../components/ui.jsx'
import { useAuth } from '../auth.jsx'

export default function Users() {
  const { user: me } = useAuth()
  const [users, setUsers] = useState(null)
  const [error, setError] = useState('')
  const [form, setForm] = useState({ email: '', password: '', role: 'VIEWER' })
  const [busy, setBusy] = useState(false)

  const load = () => UsersApi.list().then(setUsers).catch((e) => setError(errMsg(e)))
  useEffect(() => { load() }, [])

  const run = async (fn) => { setError(''); try { await fn(); await load() } catch (e) { setError(errMsg(e)) } }

  const create = async (e) => {
    e.preventDefault(); setBusy(true)
    await run(async () => { await UsersApi.create(form); setForm({ email: '', password: '', role: 'VIEWER' }) })
    setBusy(false)
  }

  return (
    <>
      <PageTitle title="Users" sub="Create accounts and control who can upload data." />
      <div className="space-y-4">
        <Alert>{error}</Alert>
        <form onSubmit={create} className="panel grid gap-3 p-4 sm:grid-cols-2 lg:grid-cols-[1fr_1fr_auto_auto]">
          <div><label className="label">Email</label><input type="email" required className="input" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></div>
          <div><label className="label">Password (8+ characters)</label><input type="password" required minLength={8} className="input" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></div>
          <div><label className="label">Role</label>
            <select className="input" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}><option>VIEWER</option><option>ADMIN</option></select></div>
          <button className="btn-primary self-end" disabled={busy}>Add user</button>
        </form>

        <div className="panel overflow-x-auto">
          {!users ? <Spinner /> : (
            <table className="w-full min-w-[640px] text-sm">
              <thead className="bg-slate-50 text-left"><tr>
                <th className="px-4 py-3 font-medium">Email</th><th className="px-4 py-3 font-medium">Role</th>
                <th className="px-4 py-3 font-medium">Status</th><th className="px-4 py-3 font-medium">Created</th><th /></tr></thead>
              <tbody>
                {users.map((u) => {
                  const self = u.email === me.email
                  return (
                    <tr key={u.id} className="border-t border-line">
                      <td className="px-4 py-2.5">{u.email}{self && <span className="ml-2 text-xs text-slate-500">(you)</span>}</td>
                      <td className="px-4 py-2.5">
                        <select className="input !w-auto" disabled={self} aria-label={`Role for ${u.email}`} value={u.role} onChange={(e) => run(() => UsersApi.changeRole(u.id, e.target.value))}>
                          <option>ADMIN</option><option>VIEWER</option>
                        </select>
                      </td>
                      <td className="px-4 py-2.5">
                        <button disabled={self} onClick={() => run(() => UsersApi.changeStatus(u.id, !u.enabled))}
                          className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${u.enabled ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-200 text-slate-600'} disabled:opacity-60`}>
                          {u.enabled ? 'Active' : 'Disabled'}
                        </button>
                      </td>
                      <td className="px-4 py-2.5 text-slate-600">{u.createdAt?.slice(0, 10)}</td>
                      <td className="px-4 py-2.5 text-right">
                        <button className="btn-danger !py-1" disabled={self} onClick={() => confirm(`Delete ${u.email}?`) && run(() => UsersApi.remove(u.id))}>Delete</button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </>
  )
}
