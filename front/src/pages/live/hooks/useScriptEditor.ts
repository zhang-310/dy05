import { useState, useEffect, useRef, useCallback } from 'react'
import { useToast } from '@/contexts/ToastContext'
import {
  checkViolation,
  saveLiveScript,
  deleteLiveScript,
  updateScriptExecuted,
  saveScriptToLibrary,
  saveBatchToLibrary,
  exportScripts,
  type LiveScript,
} from '@/api/live'
import type { ScriptSectionData } from './useLiveScriptBuilder'

const DRAFT_KEY_PREFIX = 'live-script-draft'

function getDraftKey(sessionId: number | '', scriptId: number): string {
  return `${DRAFT_KEY_PREFIX}-${sessionId}-${scriptId}`
}

function loadDraft(sessionId: number | '', scriptId: number): { content: string; durationLimitSec?: number; requirement?: string; version?: number; savedAt?: number } | null {
  try {
    const raw = localStorage.getItem(getDraftKey(sessionId, scriptId))
    if (!raw) return null
    const parsed = JSON.parse(raw) as { content?: string; durationLimitSec?: number; requirement?: string; version?: number; savedAt?: number }
    return parsed?.content != null ? { content: String(parsed.content), durationLimitSec: parsed.durationLimitSec, requirement: parsed.requirement, version: parsed.version, savedAt: parsed.savedAt } : null
  } catch {
    return null
  }
}

function saveDraft(sessionId: number | '', scriptId: number, data: { content: string; durationLimitSec?: number; requirement?: string; version?: number }) {
  try {
    localStorage.setItem(getDraftKey(sessionId, scriptId), JSON.stringify({ ...data, savedAt: Date.now() }))
  } catch { /* ignore */ }
}

function clearDraft(sessionId: number | '', scriptId: number) {
  try {
    localStorage.removeItem(getDraftKey(sessionId, scriptId))
  } catch { /* ignore */ }
}

export interface UseScriptEditorDeps {
  sessionId: number | ''
  scripts: LiveScript[]
  setScripts: React.Dispatch<React.SetStateAction<LiveScript[]>>
  scriptSections: ScriptSectionData[]
  loadData: () => Promise<void>
  session: Record<string, unknown> | null
}

export function useScriptEditor({
  sessionId,
  scripts,
  setScripts,
  scriptSections,
  loadData,
  session,
}: UseScriptEditorDeps) {
  const toast = useToast()

  // Cross-hook callbacks (set after construction via refs)
  const onEditStartRef = useRef<((row: LiveScript | undefined, session: Record<string, unknown> | null) => void) | null>(null)
  const onEditCancelRef = useRef<(() => void) | null>(null)

  const [editingId, setEditingId] = useState<number | null>(null)
  const [editingScript, setEditingScript] = useState<LiveScript | null>(null)
  const [editContent, setEditContent] = useState('')
  const [editDurationLimit, setEditDurationLimit] = useState<number | ''>('')
  const [editRequirement, setEditRequirement] = useState('')
  const [editPresenterNotes, setEditPresenterNotes] = useState('')
  // 协同编辑冲突 Dialog 状态
  const [conflictState, setConflictState] = useState<{
    open: boolean
    localContent: string
    serverContent: string
    lastEditor?: string
    pendingId: number | null
    pendingRow: LiveScript | null
  } | null>(null)
  const [violationResult, setViolationResult] = useState<Record<number, { passed: boolean; violations?: string[] }>>({})
  const [checkingId, setCheckingId] = useState<number | null>(null)
  const [deleteConfirm, setDeleteConfirm] = useState<LiveScript | null>(null)
  const [saveLibLoading, setSaveLibLoading] = useState(false)
  const [exportLoading, setExportLoading] = useState(false)
  const [chainPrompt, setChainPrompt] = useState<{ scriptId: number; label: string } | null>(null)
  /** dirty flag: content differs from server version */
  const [dirty, setDirty] = useState(false)
  /** pending draft recovery notification (set script id to show Snackbar) */
  const [draftRecoveryId, setDraftRecoveryId] = useState<number | null>(null)

  // Q.1: Undo/Redo stacks
  const [undoStack, setUndoStack] = useState<string[]>([])
  const [redoStack, setRedoStack] = useState<string[]>([])
  const undoDebounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const prevEditContentRef = useRef<string>('')

  const handleEditContentChange = useCallback((newContent: string) => {
    const oldContent = prevEditContentRef.current
    setEditContent(newContent)
    setDirty(true)
    // 防抖 500ms 合并连续输入到 undo 栈
    if (undoDebounceRef.current) clearTimeout(undoDebounceRef.current)
    undoDebounceRef.current = setTimeout(() => {
      if (oldContent !== newContent) {
        setUndoStack((prev) => [...prev, oldContent])
        setRedoStack([])
        prevEditContentRef.current = newContent
      }
    }, 500)
  }, [])

  const handleUndo = useCallback(() => {
    setUndoStack((prev) => {
      if (prev.length === 0) return prev
      const next = [...prev]
      const restored = next.pop()!
      setRedoStack((r) => [...r, editContent])
      setEditContent(restored)
      prevEditContentRef.current = restored
      return next
    })
  }, [editContent])

  const handleRedo = useCallback(() => {
    setRedoStack((prev) => {
      if (prev.length === 0) return prev
      const next = [...prev]
      const restored = next.pop()!
      setUndoStack((u) => [...u, editContent])
      setEditContent(restored)
      prevEditContentRef.current = restored
      return next
    })
  }, [editContent])

  const handleStartEdit = (id: number, content: string, row?: LiveScript) => {
    const draft = sessionId && typeof sessionId === 'number' ? loadDraft(sessionId, id) : null
    let finalContent = content
    let finalDuration: number | '' = row?.durationLimitSec ?? ''
    let finalRequirement = row?.requirement ?? ''
    let finalPresenterNotes = row?.presenterNotes ?? ''

    // Check for draft with version conflict detection
    if (draft) {
      const serverUpdateTime = typeof row?.updateTime === 'string' ? new Date(row.updateTime).getTime() : (row?.updateTime ?? 0)
      const draftSavedAt = draft.savedAt ?? 0
      const hasDraftConflict = serverUpdateTime > 0 && draftSavedAt > 0 && serverUpdateTime > draftSavedAt

      if (hasDraftConflict) {
        // 协同编辑冲突：服务器版本比草稿新，弹出 ConflictDialog 让用户决定
        setConflictState({
          open: true,
          localContent: draft.content,
          serverContent: content,
          lastEditor: typeof row?.lastEditorName === 'string' ? row.lastEditorName : undefined,
          pendingId: id,
          pendingRow: row ?? null,
        })
        return // 暂不进入编辑，等用户选择
      } else {
        // Draft is valid — auto-apply and show recovery notification
        finalContent = draft.content
        if (draft.durationLimitSec != null) finalDuration = draft.durationLimitSec
        if (draft.requirement != null) finalRequirement = draft.requirement
        setDraftRecoveryId(id)
      }
    }
    setEditingId(id)
    setEditingScript(row ?? null)
    setEditContent(finalContent)
    setEditDurationLimit(finalDuration)
    setEditRequirement(finalRequirement)
    setEditPresenterNotes(finalPresenterNotes)
    setDirty(false)
    setUndoStack([])
    setRedoStack([])
    prevEditContentRef.current = finalContent
    setViolationResult((prev) => {
      if (!(id in prev)) return prev
      const next = { ...prev }
      delete next[id]
      return next
    })
    onEditStartRef.current?.(row ?? undefined, session)
  }

  const handleCancelEdit = () => {
    if (editingId != null && sessionId && typeof sessionId === 'number') {
      clearDraft(sessionId, editingId)
    }
    setEditingId(null)
    setEditingScript(null)
    setEditContent('')
    setEditDurationLimit('')
    setEditRequirement('')
    setEditPresenterNotes('')
    setDirty(false)
    setDraftRecoveryId(null)
    onEditCancelRef.current?.()
  }

  const handleSaveEdit = async (extra?: Record<string, unknown>) => {
    if (editingId == null || !sessionId || typeof sessionId !== 'number') return
    const savedScript = scriptSections.flatMap((s) => s.scripts).find((r) => r.id === editingId)
    const wasOpening = savedScript?.scriptType === 'opening'
    try {
      const payload: Record<string, unknown> = {
        id: editingId,
        sessionId,
        scriptContent: editContent,
        scriptType: savedScript?.scriptType,
        style: savedScript?.style,
      }
      if (editDurationLimit !== '') payload.durationLimitSec = editDurationLimit
      if (editRequirement) payload.requirement = editRequirement
      payload.presenterNotes = editPresenterNotes
      await saveLiveScript({ ...payload, ...extra })
      // Reload data to get updated script from server
      await loadData()
      toast('保存成功', 'success')
      if (sessionId && typeof sessionId === 'number') clearDraft(sessionId, editingId)
      setEditingId(null)
      setEditDurationLimit('')
      setEditRequirement('')
      setEditPresenterNotes('')
      setDirty(false)
      setDraftRecoveryId(null)
      setChainPrompt(null)
      setViolationResult((prev) => {
        const next = { ...prev }
        delete next[editingId]
        return next
      })
      if (wasOpening) {
        const firstProduct = scripts
          .filter((s) => s.scriptType === 'product')
          .sort((a, b) => (a.sequenceNo ?? 0) - (b.sequenceNo ?? 0))[0]
        if (firstProduct?.id) {
          setChainPrompt({ scriptId: firstProduct.id, label: '开场已修改，是否调整第一个产品话术的引入部分？' })
        }
      }
    } catch (e) {
      toast(e instanceof Error ? e.message : '保存失败', 'error')
    }
  }

  // Phase 3：本地草稿保护，编辑内容防抖写入 localStorage（含版本号）
  const draftSavedRef = useRef<{ content: string; dur: number | ''; req: string } | null>(null)
  useEffect(() => {
    if (editingId == null || !sessionId || typeof sessionId !== 'number') return
    // isDirty check: skip save if content unchanged since last draft save
    const current = { content: editContent, dur: editDurationLimit, req: editRequirement }
    const prev = draftSavedRef.current
    if (prev && prev.content === current.content && prev.dur === current.dur && prev.req === current.req) return
    const t = setTimeout(() => {
      saveDraft(sessionId, editingId, {
        content: editContent,
        durationLimitSec: editDurationLimit !== '' ? Number(editDurationLimit) : undefined,
        requirement: editRequirement || undefined,
        version: typeof editingScript?.version === 'number' ? editingScript.version : undefined,
      })
      draftSavedRef.current = current
    }, 1500)
    return () => clearTimeout(t)
  }, [editingId, sessionId, editContent, editDurationLimit, editRequirement, editingScript])

  // beforeunload: warn if dirty
  useEffect(() => {
    if (!dirty) return
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault()
    }
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [dirty])

  /** Dismiss draft recovery notification */
  const dismissDraftRecovery = useCallback(() => {
    setDraftRecoveryId(null)
  }, [])

  /** Discard recovered draft and reload original content */
  const discardDraftRecovery = useCallback(() => {
    if (draftRecoveryId != null && editingScript) {
      const originalContent = (editingScript.scriptContent as string) ?? ''
      setEditContent(originalContent)
      prevEditContentRef.current = originalContent
      setDirty(false)
      if (sessionId && typeof sessionId === 'number') {
        clearDraft(sessionId, draftRecoveryId)
      }
    }
    setDraftRecoveryId(null)
  }, [draftRecoveryId, editingScript, sessionId])

  // Ctrl+S / Escape / Ctrl+Z / Ctrl+Shift+Z shortcuts
  const saveEditRef = useRef<() => void>(() => {})
  const undoRef = useRef(handleUndo)
  const redoRef = useRef(handleRedo)
  useEffect(() => { saveEditRef.current = () => { handleSaveEdit() } })
  useEffect(() => { undoRef.current = handleUndo }, [handleUndo])
  useEffect(() => { redoRef.current = handleRedo }, [handleRedo])
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault()
        if (editingId != null) { saveEditRef.current() }
      }
      if ((e.ctrlKey || e.metaKey) && e.key === 'z' && !e.shiftKey && editingId != null) {
        e.preventDefault()
        undoRef.current()
      }
      if ((e.ctrlKey || e.metaKey) && e.key === 'z' && e.shiftKey && editingId != null) {
        e.preventDefault()
        redoRef.current()
      }
      if (e.key === 'Escape' && editingId != null) {
        handleCancelEdit()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [editingId])

  const handleDeleteScript = async () => {
    if (!deleteConfirm) return
    const id = deleteConfirm.id
    if (!id) return
    const prevScripts = scripts
    setScripts((prev) => prev.filter((s) => s.id !== id))
    setDeleteConfirm(null)
    setViolationResult((prev) => { const next = { ...prev }; delete next[id]; return next })
    try {
      await deleteLiveScript(id)
      toast('删除成功', 'success')
    } catch (e) {
      setScripts(prevScripts)
      toast(e instanceof Error ? e.message : '删除失败', 'error')
    }
  }

  const handleMarkExecuted = async (scriptId: number, currentExecuted: number) => {
    const next = currentExecuted === 1 ? 0 : 1
    setScripts((prev) => prev.map((s) => s.id === scriptId ? { ...s, executed: next } : s))
    try {
      await updateScriptExecuted(scriptId)
      toast(next === 1 ? '已标记为已执行' : '已取消执行', 'success')
    } catch (e) {
      setScripts((prev) => prev.map((s) => s.id === scriptId ? { ...s, executed: currentExecuted } : s))
      toast(e instanceof Error ? e.message : '操作失败', 'error')
    }
  }

  const handleCheckViolation = async (scriptId: number) => {
    setCheckingId(scriptId)
    try {
      const r = await checkViolation({ scriptId }) as Record<string, unknown>
      const passed = Boolean(r.passed)
      const violations = Array.isArray(r.violations) ? (r.violations as string[]) : undefined
      setViolationResult((prev) => ({ ...prev, [scriptId]: { passed, violations } }))
      toast(passed ? '无违规' : `发现 ${(r.violationCount as number) ?? 0} 处违规`, passed ? 'success' : 'error')
    } catch (e) {
      toast(e instanceof Error ? e.message : '检测失败', 'error')
    } finally {
      setCheckingId(null)
    }
  }

  const handleSaveToLibrary = async (scriptId?: number) => {
    if (!sessionId || typeof sessionId !== 'number') return
    setSaveLibLoading(true)
    try {
      if (scriptId != null) {
        await saveScriptToLibrary({ scriptId })
        toast('已保存到话术库', 'success')
      } else {
        const count = await saveBatchToLibrary({ sessionId })
        toast(`已保存 ${count} 条话术到话术库`, 'success')
      }
    } catch (e) {
      toast(e instanceof Error ? e.message : '保存失败', 'error')
    } finally {
      setSaveLibLoading(false)
    }
  }

  const handleExport = async () => {
    if (!sessionId || typeof sessionId !== 'number') return
    setExportLoading(true)
    try {
      const text = await exportScripts(sessionId)
      const blob = new Blob([String(text ?? '')], { type: 'text/markdown;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `直播话术-${sessionId}.md`
      a.click()
      URL.revokeObjectURL(url)
      toast('导出成功', 'success')
    } catch (e) {
      toast(e instanceof Error ? e.message : '导出失败', 'error')
    } finally {
      setExportLoading(false)
    }
  }

  const handleChainPromptAdjust = (scriptId: number) => {
    const row = scriptSections.flatMap((s) => s.scripts).find((r) => r.id === scriptId)
    if (row) {
      handleStartEdit(scriptId, row.scriptContent ?? '', row)
    }
    setChainPrompt(null)
  }

  // ── 协同编辑冲突处理 ──────────────────────────────────────────────────────────

  const handleConflictOverwrite = () => {
    const cs = conflictState
    if (!cs || cs.pendingId == null) { setConflictState(null); return }
    // 用本地草稿覆盖（丢弃服务器版本）
    const row = cs.pendingRow
    const id = cs.pendingId
    setConflictState(null)
    if (sessionId && typeof sessionId === 'number') clearDraft(sessionId, id)
    // 直接以本地内容进入编辑
    setEditingId(id)
    setEditingScript(row ?? null)
    setEditContent(cs.localContent)
    const durLimit: number | '' = row?.durationLimitSec ?? ''
    setEditDurationLimit(durLimit)
    setEditRequirement(row?.requirement ?? '')
    setEditPresenterNotes(row?.presenterNotes ?? '')
    setDirty(true)
    setUndoStack([])
    setRedoStack([])
    prevEditContentRef.current = cs.localContent
  }

  const handleConflictDiscard = () => {
    const cs = conflictState
    if (!cs || cs.pendingId == null) { setConflictState(null); return }
    const id = cs.pendingId
    const row = cs.pendingRow
    setConflictState(null)
    if (sessionId && typeof sessionId === 'number') clearDraft(sessionId, id)
    // 以服务器版本进入编辑
    setEditingId(id)
    setEditingScript(row ?? null)
    setEditContent(cs.serverContent)
    const durLimit: number | '' = row?.durationLimitSec ?? ''
    setEditDurationLimit(durLimit)
    setEditRequirement(row?.requirement ?? '')
    setEditPresenterNotes(row?.presenterNotes ?? '')
    setDirty(false)
    setUndoStack([])
    setRedoStack([])
    prevEditContentRef.current = cs.serverContent
  }

  const handleConflictClose = () => setConflictState(null)

  return {
    editingId, editingScript, editContent, setEditContent,
    editDurationLimit, setEditDurationLimit,
    editRequirement, setEditRequirement,
    editPresenterNotes, setEditPresenterNotes,
    violationResult, checkingId,
    deleteConfirm, setDeleteConfirm,
    saveLibLoading, exportLoading,
    chainPrompt, setChainPrompt,
    handleStartEdit, handleCancelEdit, handleSaveEdit,
    handleDeleteScript, handleMarkExecuted,
    handleCheckViolation,
    handleSaveToLibrary, handleExport,
    handleChainPromptAdjust,
    handleEditContentChange,
    handleUndo, handleRedo, canUndo: undoStack.length > 0, canRedo: redoStack.length > 0,
    dirty, draftRecoveryId, dismissDraftRecovery, discardDraftRecovery,
    // 协同编辑冲突
    conflictState,
    handleConflictOverwrite,
    handleConflictDiscard,
    handleConflictClose,
    // Expose setters for cross-hook use
    setEditingId, setEditingScript, setEditDurationLimit_internal: setEditDurationLimit,
    setEditRequirement_internal: setEditRequirement,
    // Refs for cross-hook callbacks (set by orchestrator)
    onEditStartRef,
    onEditCancelRef,
  }
}
