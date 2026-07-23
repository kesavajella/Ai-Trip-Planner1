import { Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import Layout from './Layout'
import NotFound from '../pages/NotFound'

export default function ProtectedRoute({ children, adminOnly }) {
  const { user } = useAuth()

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (adminOnly && user.role !== 'ADMIN') {
    return (
      <Layout>
        <NotFound />
      </Layout>
    )
  }

  return children
}
