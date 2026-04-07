import { useState } from 'react'
import { Outlet, useOutletContext } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { IconMenu } from './Icons'

export interface LayoutContext {
  locale: 'en' | 'cz'
  setLocale: (l: 'en' | 'cz') => void
  theme: 'dark' | 'light'
  toggleTheme: () => void
}

interface Props extends LayoutContext {}

export function Layout({ locale, setLocale, theme, toggleTheme }: Props) {
  const [sidebarOpen, setSidebarOpen] = useState(false)

  return (
    <div className="app-layout">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="main-area">
        <header className="mobile-topbar">
          <button
            className="topbar-menu"
            type="button"
            onClick={() => setSidebarOpen(true)}
            aria-label="Open menu"
          >
            <IconMenu size={22} />
          </button>
          <div className="topbar-brand">
            <svg width="24" height="24" viewBox="0 0 100 100" fill="none">
              <defs>
                <linearGradient id="vg2" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#a855f7" />
                  <stop offset="100%" stopColor="#6366f1" />
                </linearGradient>
              </defs>
              <path d="M50 8L90 78H10L50 8Z" fill="url(#vg2)" opacity="0.9" />
              <path d="M50 28L72 68H28L50 28Z" fill="url(#vg2)" opacity="0.5" />
            </svg>
            <strong>VOIDIUM</strong>
          </div>
        </header>
        <main className="main-content">
          <Outlet context={{ locale, setLocale, theme, toggleTheme } satisfies LayoutContext} />
        </main>
      </div>
    </div>
  )
}

export function useLayoutContext() {
  return useOutletContext<LayoutContext>()
}
