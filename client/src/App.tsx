import { useCallback } from 'react'
import { Effect } from 'effect'
import { RuntimeProvider } from './runtime/RuntimeContext'
import { AuthService } from './services/AuthService'
import { useEffectTs } from './hooks/useEffectTs'
import { useAuthService } from './hooks/useAuthService'
import { useRouterService } from './hooks/useRouterService'
import { Navbar } from './components/Navbar'
import { Toast } from './components/Toast'
import { LandingPage } from './pages/LandingPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { SettingsPage } from './pages/SettingsPage'
import { ChatPage } from './pages/ChatPage'

export default function App() {
  return (
    <RuntimeProvider>
      <AppShell />
      <Toast />
    </RuntimeProvider>
  )
}

// ─── AppShell ─────────────────────────────────────────────────────────────────
// Checks auth on mount, then routes to the correct page.

function AppShell() {
  const { isLoggedIn } = useAuthService()
  const { currentPage } = useRouterService()

  // Check auth on mount — sets isLoggedIn if the session cookie is still valid.
  useEffectTs(
    () =>
      AuthService.pipe(
        Effect.flatMap((svc) => svc.checkAuth),
        Effect.tap((ok) =>
          ok ? AuthService.pipe(Effect.flatMap((s) => s.markLoggedIn)) : Effect.void
        ),
        Effect.catchAll(() => Effect.void)
      ),
    []
  )

  // Auth guard: redirect unauthenticated users to login.
  const isPublicPage = currentPage.type === 'login' || currentPage.type === 'register'

  if (!isLoggedIn && !isPublicPage) {
    return <LoginPage />
  }

  // Auth pages have no Navbar.
  if (currentPage.type === 'login') return <LoginPage />
  if (currentPage.type === 'register') return <RegisterPage />

  // Settings page includes its own Navbar (matching old Scala client).
  if (currentPage.type === 'settings') return <SettingsPage />

  // All other authenticated pages wrap with Navbar.
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <PageContent currentPage={currentPage} />
    </div>
  )
}

type CurrentPage = ReturnType<typeof useRouterService>['currentPage']

function PageContent({ currentPage }: { currentPage: CurrentPage }) {
  const renderChat = useCallback((chatId: string) => <ChatPage chatId={chatId} />, [])

  switch (currentPage.type) {
    case 'landing':
      return <LandingPage />
    case 'chat':
      return renderChat(currentPage.chatId)
    default:
      return <LandingPage />
  }
}
