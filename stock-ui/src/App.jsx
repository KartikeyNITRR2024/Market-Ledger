import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth.jsx'
import Layout from './components/Layout.jsx'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import StockList from './pages/StockList.jsx'
import Upload from './pages/Upload.jsx'
import Users from './pages/Users.jsx'
import IngestionErrors from './pages/IngestionErrors.jsx'

function Protected({ adminOnly, children }) {
  const { user, isAdmin } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (adminOnly && !isAdmin) return <Navigate to="/" replace />
  return children
}

export default function App() {
  const { user } = useAuth()
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
      <Route element={<Protected><Layout /></Protected>}>
        <Route index element={<Dashboard />} />
        <Route path="stocks" element={<StockList />} />
        <Route path="upload" element={<Protected adminOnly><Upload /></Protected>} />
        <Route path="users" element={<Protected adminOnly><Users /></Protected>} />
        <Route path="errors" element={<Protected adminOnly><IngestionErrors /></Protected>} />
        <Route path="errors/:taskId" element={<Protected adminOnly><IngestionErrors /></Protected>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
