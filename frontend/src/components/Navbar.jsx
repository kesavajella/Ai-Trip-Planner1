import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export default function Navbar() {
  const { user, signout } = useAuth()
  const navigate = useNavigate()

  const links = [
    { to: '/dashboard', label: 'Dashboard' },
    { to: '/generate', label: 'Plan Trip' },
    { to: '/trips', label: 'My Trips' },
    { to: '/saved', label: 'Saved' },
  ]

  return (
    <header className="sticky top-0 z-50 border-b bg-background-80 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <a href="#/" className="text-xl font-bold">Intelli<span className="text-primary-60">Trip</span></a>
        <nav className="hidden items-center gap-6 md:flex">
          {user && links.map((l) => (
            <a key={l.to} href={`#${l.to}`} className="nav-link">{l.label}</a>
          ))}
          {user && user.role === 'ADMIN' && (
            <a href="#/admin" className="nav-link">Admin</a>
          )}
        </nav>
        <div className="flex items-center gap-3">
          {user ? (
            <>
              <span className="text-sm text-muted-foreground" style={{ display: 'none' }}>{user.name}</span>
              <button
                className="btn btn-outline btn-sm"
                onClick={() => {
                  signout()
                  navigate('/login')
                }}
              >
                Log out
              </button>
            </>
          ) : (
            <>
              <a href="#/login" className="btn btn-ghost btn-sm">Sign in</a>
              <a href="#/signup" className="btn btn-primary btn-sm">Sign up</a>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
