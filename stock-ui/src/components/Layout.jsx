import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth.jsx'

const links = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/stocks', label: 'Stock data' },
  { to: '/upload', label: 'Upload CSV', admin: true },
  { to: '/errors', label: 'Ingestion errors', admin: true },
  { to: '/users', label: 'Users', admin: true },
]

export default function Layout() {
  const { user, logout, isAdmin } = useAuth()
  const [open, setOpen] = useState(false)
  const visible = links.filter((l) => !l.admin || isAdmin)

  return (
    <div className="min-h-screen md:flex">
      {/* Mobile top bar */}
      <header className="sticky top-0 z-30 flex items-center justify-between bg-ink px-4 py-3 text-white md:hidden">
        <span className="font-semibold">Market Ledger</span>
        <button aria-label="Toggle menu" aria-expanded={open} onClick={() => setOpen(!open)} className="rounded p-1 hover:bg-white/10">
          <svg width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2"><path d={open ? 'M5 5l14 14M19 5L5 19' : 'M4 7h16M4 12h16M4 17h16'} /></svg>
        </button>
      </header>

      {open && <div className="fixed inset-0 z-30 bg-black/40 md:hidden" onClick={() => setOpen(false)} />}

      <aside className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-ink text-white transition-transform md:sticky md:top-0 md:h-screen md:translate-x-0 ${open ? 'translate-x-0' : '-translate-x-full'}`}>
        <div className="px-6 py-6 text-lg font-semibold tracking-tight">Market Ledger</div>
        <nav className="flex-1 space-y-1 px-3">
          {visible.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.end} onClick={() => setOpen(false)}
              className={({ isActive }) => `block rounded-md px-3 py-2 text-sm ${isActive ? 'bg-white/15 font-medium' : 'text-slate-300 hover:bg-white/10'}`}>
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-white/10 p-4 text-sm">
          <div className="truncate font-medium">{user.email}</div>
          <div className="mb-3 text-xs text-brass" style={{ color: '#e0b25a' }}>{isAdmin ? 'Admin' : 'Viewer'}</div>
          <button onClick={logout} className="w-full rounded-md border border-white/20 px-3 py-2 hover:bg-white/10">Sign out</button>
        </div>
      </aside>

      <main className="min-w-0 flex-1 p-4 md:p-8">
        <Outlet />
      </main>
    </div>
  )
}
