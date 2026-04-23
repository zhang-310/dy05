import { useEffect } from 'react'

export interface UseKeyboardShortcutsParams {
  enabled: boolean
  onToggleHelp: () => void
  onSelectAll?: () => void
  onExpandAll?: () => void
  onCollapseAll?: () => void
}

export function useKeyboardShortcuts({
  enabled,
  onToggleHelp,
  onSelectAll,
  onExpandAll,
  onCollapseAll,
}: UseKeyboardShortcutsParams) {
  useEffect(() => {
    if (!enabled) return
    const handler = (e: KeyboardEvent) => {
      // Ignore when typing in inputs
      const tag = (e.target as HTMLElement)?.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || (e.target as HTMLElement)?.isContentEditable) return

      // Ctrl+/ or ? — toggle help
      if (((e.ctrlKey || e.metaKey) && e.key === '/') || (e.key === '?' && !e.ctrlKey && !e.metaKey && !e.altKey)) {
        e.preventDefault()
        onToggleHelp()
        return
      }

      // Ctrl+A — select all (only if not in input)
      if ((e.ctrlKey || e.metaKey) && e.key === 'a' && !e.shiftKey) {
        e.preventDefault()
        onSelectAll?.()
        return
      }

      // Ctrl+Shift+E — expand all
      if ((e.ctrlKey || e.metaKey) && e.shiftKey && (e.key === 'E' || e.key === 'e')) {
        e.preventDefault()
        onExpandAll?.()
        return
      }

      // Ctrl+Shift+C — collapse all
      if ((e.ctrlKey || e.metaKey) && e.shiftKey && (e.key === 'C' || e.key === 'c')) {
        e.preventDefault()
        onCollapseAll?.()
        return
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [enabled, onToggleHelp, onSelectAll, onExpandAll, onCollapseAll])
}
