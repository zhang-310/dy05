import { useState, useCallback, useRef } from 'react'
import { useToast } from '@/contexts/ToastContext'
import {
  chatForScriptStream,
  generateForSlotStream,
  saveToCopyIfPassed,
  saveLiveScript,
  type LiveScript,
  type LiveSessionVO,
} from '@/api/live'
import {
  SCRIPT_TYPE_LABEL,
  SCRIPT_STYLE_OPTIONS,
} from '@/pages/live/components/constants'

export interface UseAiChatDeps {
  editingId: number | null
  editingScript: LiveScript | null
  focusedScriptId: number | null
  focusedScript: LiveScript | null
  scripts: LiveScript[]
  setScripts: React.Dispatch<React.SetStateAction<LiveScript[]>>
  setEditContent: (c: string) => void
  session: LiveSessionVO | null
  loadData: () => Promise<void>
  selectedModelId: number | ''
}

export function useAiChat({
  editingId,
  editingScript,
  focusedScriptId,
  focusedScript,
  scripts: _scripts,
  setScripts,
  setEditContent,
  session,
  loadData,
  selectedModelId,
}: UseAiChatDeps) {
  const toast = useToast()

  const [chatMessage, setChatMessage] = useState('')
  const [chatResponse, setChatResponse] = useState('')
  const [chatLoading, setChatLoading] = useState(false)
  const [chatStreaming, setChatStreaming] = useState(false)
  const abortedRef = useRef(false)
  const streamedContentRef = useRef('')
  const [chatHistory, setChatHistory] = useState<Array<{ role: 'user' | 'assistant'; content: string }>>([])
  const [chatPersona, setChatPersona] = useState('')
  const [chatScene, setChatScene] = useState('')
  const [chatDurationSec, setChatDurationSec] = useState<number | ''>('')
  const [chatDimensions, setChatDimensions] = useState<string[]>([])
  const [chatStyle, setChatStyle] = useState('')
  const [copySaveLoading, setCopySaveLoading] = useState(false)

  // Analyst panel
  const [analystScript, setAnalystScript] = useState<LiveScript | null>(null)
  const [analystContent, setAnalystContent] = useState('')
  const [analystDuration, setAnalystDuration] = useState<number | ''>('')
  const [analystRequirement, setAnalystRequirement] = useState('')
  const [analystLoading, setAnalystLoading] = useState(false)
  const [analystStreaming, setAnalystStreaming] = useState(false)

  // Called when editing starts (cross-hook callback)
  const onEditStart = useCallback((row: LiveScript | undefined, sess: Record<string, unknown> | null) => {
    setChatScene(sess?.liveTitle ? String(sess.liveTitle) : '')
    setChatDurationSec((row?.durationLimitSec as number) ?? '')
  }, [])

  // Called when editing is cancelled (cross-hook callback)
  const onEditCancel = useCallback(() => {
    setChatMessage('')
    setChatResponse('')
    setChatHistory([])
    setChatPersona('')
    setChatScene('')
    setChatDurationSec('')
    setChatDimensions([])
  }, [])

  const buildChatMessage = useCallback(() => {
    const parts: string[] = []
    if (chatHistory.length > 0) {
      parts.push('上文对话：')
      chatHistory.forEach((h) => parts.push(`${h.role === 'user' ? '用户' : 'AI'}: ${h.content}`))
      parts.push('')
      parts.push('用户新需求：' + (chatMessage.trim() || '(继续优化)'))
      return parts.join('\n')
    }
    if (chatPersona.trim()) parts.push(`人设：${chatPersona.trim()}`)
    if (chatScene.trim()) parts.push(`场景：${chatScene.trim()}`)
    const targetScript = editingScript ?? focusedScript
    const scriptType = targetScript?.scriptType as string
    if (scriptType) {
      const label = SCRIPT_TYPE_LABEL[scriptType] ?? scriptType
      parts.push(`话术类型：${label}话术`)
    }
    if (chatDurationSec !== '' && Number(chatDurationSec) > 0) parts.push(`时长：${chatDurationSec}秒`)
    if (chatStyle) {
      const styleLabel = SCRIPT_STYLE_OPTIONS.find(s => s.value === chatStyle)?.label ?? chatStyle
      parts.push(`风格：${styleLabel}`)
    }
    if (chatDimensions.length > 0) parts.push(`考虑维度：${chatDimensions.join('、')}`)
    if (chatMessage.trim()) parts.push('\n用户需求：' + chatMessage.trim())
    return parts.length > 0 ? parts.join('\n') : chatMessage.trim()
  }, [chatHistory, chatPersona, chatScene, chatDurationSec, chatStyle, chatDimensions, chatMessage, editingScript?.scriptType, focusedScript?.scriptType])

  const handleChatSend = async () => {
    const message = buildChatMessage()
    const targetId = editingId ?? focusedScriptId
    if (!targetId || !message.trim() || chatLoading || chatStreaming) return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    const userMsg = chatMessage.trim()
    setChatLoading(true)
    setChatStreaming(true)
    setChatResponse('')
    streamedContentRef.current = ''
    abortedRef.current = false
    try {
      const result = await chatForScriptStream({
        scriptId: targetId,
        message: message.trim(),
        modelId: selectedModelId,
      })
      if (abortedRef.current) return
      const finalContent = typeof result === 'string' ? result : String(result ?? '')
      streamedContentRef.current = finalContent
      setChatResponse(finalContent)
      setChatHistory((prev) => {
        const next = [...prev, { role: 'user' as const, content: userMsg }, { role: 'assistant' as const, content: finalContent }]
        return next.slice(-6)
      })
      toast('AI 生成成功', 'success')
    } catch (e) {
      if (!abortedRef.current) {
        toast(e instanceof Error ? e.message : 'AI 生成失败', 'error')
      }
    } finally {
      setChatStreaming(false)
      setChatLoading(false)
    }
  }

  const handleChatApply = async () => {
    if (!chatResponse) return
    // 编辑态：写入 editContent（现有行为）
    if (editingId) {
      setEditContent(chatResponse)
      setChatResponse('')
      toast('已应用到编辑区', 'success')
      return
    }
    // 非编辑态但有聚焦：直接保存到 DB
    if (focusedScriptId && session?.id) {
      try {
        await saveLiveScript({
          id: focusedScriptId,
          sessionId: session.id as number,
          scriptContent: chatResponse,
        })
        await loadData()
        setChatResponse('')
        toast('已应用并保存', 'success')
      } catch (e) {
        toast(e instanceof Error ? e.message : '保存失败', 'error')
      }
    }
  }

  const handleSaveToCopy = async () => {
    const content = chatResponse || (editingScript?.scriptContent as string) || ''
    if (!content) return
    setCopySaveLoading(true)
    try {
      const res = await saveToCopyIfPassed({ content, sessionId: session?.id as number })
      if (res && typeof res === 'object' && (res as Record<string, unknown>).passed) {
        toast('审核通过，已保存到文案库', 'success')
      } else {
        const r = res as Record<string, unknown>
        const violations = r.violations as string[] | undefined
        const msg = violations?.length ? `违规词：${violations.join('、')}` : `检测到 ${(r.violationCount as number) ?? 0} 处违规`
        toast(msg, 'error')
      }
    } catch (e) {
      toast(e instanceof Error ? e.message : '保存失败', 'error')
    } finally {
      setCopySaveLoading(false)
    }
  }

  const handleOpenAnalyst = (row: LiveScript) => {
    setAnalystScript(row)
    setAnalystContent((row.scriptContent as string) ?? '')
    setAnalystDuration((row.durationLimitSec as number) ?? '')
    setAnalystRequirement((row.requirement as string) ?? '')
  }

  const handleAnalystAutoFill = async () => {
    if (!analystScript?.id) return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    setAnalystLoading(true)
    setAnalystStreaming(true)
    setAnalystContent('')
    const dur = analystDuration === '' ? undefined : (typeof analystDuration === 'number' ? analystDuration : Number(analystDuration))
    try {
      const result = await generateForSlotStream({
        scriptId: analystScript.id,
        modelId: selectedModelId,
        requirement: analystRequirement || undefined,
        durationLimitSec: dur && dur > 0 ? dur : undefined,
      })
      const content = typeof result === 'string' ? result : String(result ?? '')
      setAnalystContent(content)
      toast('AI 自动填充成功', 'success')
    } catch (e) {
      toast(e instanceof Error ? e.message : 'AI 填充失败', 'error')
    } finally {
      setAnalystStreaming(false)
      setAnalystLoading(false)
    }
  }

  const handleAnalystApply = async () => {
    if (!analystScript?.id || !session?.id) return
    const sessionIdNum = session.id as number
    try {
      const savedVO = await saveLiveScript({
        id: analystScript.id,
        sessionId: sessionIdNum,
        scriptContent: analystContent,
        durationLimitSec: analystDuration === '' ? undefined : (analystDuration as number),
        requirement: analystRequirement || undefined,
      })
      if (savedVO && typeof savedVO === 'object' && 'id' in (savedVO as object)) {
        setScripts((prev) => prev.map((s) => s.id === analystScript.id ? { ...s, ...(savedVO as LiveScript) } : s))
      } else {
        await loadData()
      }
      toast('已应用', 'success')
      setAnalystScript(null)
    } catch (e) {
      toast(e instanceof Error ? e.message : '应用失败', 'error')
    }
  }

  // For skeleton expand (cross-hook: sets chat state)
  const prepareChatForSkeleton = useCallback((summary: string, liveTitle: string | undefined) => {
    setChatScene(liveTitle ? String(liveTitle) : '')
    setChatMessage(summary ? `按以下要点展开：${summary}` : '')
  }, [])

  return {
    chatMessage, setChatMessage,
    chatResponse, setChatResponse,
    chatLoading, chatStreaming, chatHistory,
    chatPersona, setChatPersona,
    chatScene, setChatScene,
    chatDurationSec, setChatDurationSec,
    chatDimensions, setChatDimensions,
    chatStyle, setChatStyle,
    copySaveLoading,
    handleChatSend, handleChatApply,
    handleSaveToCopy,
    handleChatHistoryClear: () => setChatHistory([]),
    // Analyst
    analystScript, setAnalystScript,
    analystContent, setAnalystContent,
    analystDuration, setAnalystDuration,
    analystRequirement, setAnalystRequirement,
    analystLoading, analystStreaming,
    handleOpenAnalyst, handleAnalystAutoFill, handleAnalystApply,
    // Cross-hook callbacks
    onEditStart,
    onEditCancel,
    prepareChatForSkeleton,
  }
}


