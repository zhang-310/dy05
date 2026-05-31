import { StrictMode } from 'react'
import ReactDOM from 'react-dom/client'
import './i18n'
import App from './App'

const installPreloadErrorReload = () => {
  window.addEventListener('vite:preloadError', (event) => {
    event.preventDefault()

    const reloadKey = 'dy05:preload-error-reload-at'
    const now = Date.now()
    const lastReloadAt = Number(sessionStorage.getItem(reloadKey) ?? 0)

    if (now - lastReloadAt > 10_000) {
      sessionStorage.setItem(reloadKey, String(now))
      window.location.reload()
    }
  })
}

installPreloadErrorReload()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
)
