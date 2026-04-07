import { useState, useEffect, useCallback } from 'react'
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom'
import { LocaleProvider } from './i18n'
import { ThemeProvider } from './theme'
import { ToastProvider } from './components/Toast'
import { DashboardProvider } from './context/DashboardContext'
import { Layout } from './components/Layout'
import { DashboardPage } from './pages/DashboardPage'
import { PlayersPage } from './pages/PlayersPage'
import { AnnouncementsPage } from './pages/AnnouncementsPage'
import { RestartsPage } from './pages/RestartsPage'
import { EntityCleanerPage } from './pages/EntityCleanerPage'
import { ModulesPage } from './pages/ModulesPage'
import { DiscordPage } from './pages/DiscordPage'
import { RanksPage } from './pages/RanksPage'
import { VotesPage } from './pages/VotesPage'
import { TicketsPage } from './pages/TicketsPage'
import { StatsPage } from './pages/StatsPage'
import { FeedsPage } from './pages/FeedsPage'
import { AiPage } from './pages/AiPage'
import { ServerPage } from './pages/ServerPage'
import { SettingsPage } from './pages/SettingsPage'
import { ConsolePage } from './pages/ConsolePage'
import './styles/app.css'
import './styles/feature-pages.css'

export default function App() {
  const [locale, setLocale] = useState<'en' | 'cz'>(() => {
    const saved = localStorage.getItem('voidium_locale')
    if (saved === 'cz') return 'cz'
    const params = new URLSearchParams(window.location.search)
    return params.get('lang') === 'cz' ? 'cz' : 'en'
  })
  const [theme, setTheme] = useState<'dark' | 'light'>(() =>
    (localStorage.getItem('voidium_theme') as 'light') === 'light' ? 'light' : 'dark'
  )

  useEffect(() => {
    localStorage.setItem('voidium_locale', locale)
  }, [locale])

  useEffect(() => {
    localStorage.setItem('voidium_theme', theme)
    document.documentElement.setAttribute('data-theme', theme)
  }, [theme])

  const toggleTheme = useCallback(() => setTheme(t => t === 'dark' ? 'light' : 'dark'), [])

  return (
    <ThemeProvider value={theme}>
      <LocaleProvider value={locale}>
        <ToastProvider>
          <DashboardProvider>
            <HashRouter>
              <Routes>
                <Route element={<Layout locale={locale} setLocale={setLocale} theme={theme} toggleTheme={toggleTheme} />}>
                  <Route index element={<DashboardPage />} />
                  <Route path="players" element={<PlayersPage />} />
                  <Route path="announcements" element={<AnnouncementsPage />} />
                  <Route path="restarts" element={<RestartsPage />} />
                  <Route path="entity-cleaner" element={<EntityCleanerPage />} />
                  <Route path="modules" element={<ModulesPage />} />
                  <Route path="discord" element={<DiscordPage />} />
                  <Route path="ranks" element={<RanksPage />} />
                  <Route path="playtime" element={<Navigate to="/stats" replace />} />
                  <Route path="votes" element={<VotesPage />} />
                  <Route path="tickets" element={<TicketsPage />} />
                  <Route path="stats" element={<StatsPage />} />
                  <Route path="feeds" element={<FeedsPage />} />
                  <Route path="ai" element={<AiPage />} />
                  <Route path="server" element={<ServerPage />} />
                  <Route path="settings" element={<SettingsPage />} />
                  <Route path="console" element={<ConsolePage />} />
                </Route>
              </Routes>
            </HashRouter>
          </DashboardProvider>
        </ToastProvider>
      </LocaleProvider>
    </ThemeProvider>
  )
}
