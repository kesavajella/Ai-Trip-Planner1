import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './hooks/useAuth'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import Landing from './pages/Landing'
import Login from './pages/Login'
import Signup from './pages/Signup'
import Dashboard from './pages/Dashboard'
import Generate from './pages/Generate'
import Trips from './pages/Trips'
import TripDetail from './pages/TripDetail'
import Saved from './pages/Saved'
import Admin from './pages/Admin'
import NotFound from './pages/NotFound'

function AppContent() {
  const { user } = useAuth()
  const location = useLocation()

  if (user && ['/', '/login', '/signup'].includes(location.pathname)) {
    return <Navigate to="/dashboard" replace />
  }

  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Landing />} />
        <Route path="login" element={<Login />} />
        <Route path="signup" element={<Signup />} />
        <Route path="dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
        <Route path="generate" element={<ProtectedRoute><Generate /></ProtectedRoute>} />
        <Route path="trips" element={<ProtectedRoute><Trips /></ProtectedRoute>} />
        <Route path="trips/:id" element={<ProtectedRoute><TripDetail /></ProtectedRoute>} />
        <Route path="saved" element={<ProtectedRoute><Saved /></ProtectedRoute>} />
        <Route path="admin" element={<ProtectedRoute adminOnly><Admin /></ProtectedRoute>} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}

export default AppContent
