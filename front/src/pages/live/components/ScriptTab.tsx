import { useState, useCallback } from 'react'
import { useThrottledCallback } from '@/hooks/useDebouncedCallback'
import OfficialReferencesPanel from '@/components/OfficialReferencesPanel'
import {
  Box,
  Typography,
  Card,
  Button,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  List,
  ListItemButton,
  ListItemText,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Switch,
  FormControlLabel,
  Chip,
  Accordion,
  AccordionSummary,
  AccordionDetails,
} from '@mui/material'
import DeleteIcon from '@mui/icons-material/Delete'
import SaveIcon from '@mui/icons-material/Save'
import DownloadIcon from '@mui/icons-material/Download'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import EditIcon from '@mui/icons-material/Edit'
import CheckIcon from '@mui/icons-material/Check'
import GavelIcon from '@mui/icons-material/Gavel'
import PlayCircleIcon from '@mui/icons-material/PlayCircle'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import MenuBookIcon from '@mui/icons-material/MenuBook'
import {
  generateOpening,
  generateProduct,
  generateFull,
  checkViolation,
  saveLiveScript,
  deleteLiveScript,
  updateScriptExecuted,
  saveScriptToLibrary,
  saveBatchToLibrary,
  exportScripts,
  type LiveRagRef,
  type LiveOfficialReference,
} from '@/api/live'

const SCRIPT_TYPE_LABEL: Record<string, string> = {
  opening: '开场',
  product: '产品',
  transition: '转场',
  closing: '结尾',
  custom: '自定义',
}

const SCRIPT_STYLE_OPTIONS = [
  { value: '', label: '默认（使用场次风格）' },
  { value: 'professional', label: '专业' },
  { value: 'friendly', label: '亲切' },
  { value: 'passionate', label: '激情' },
  { value: 'seeding', label: '种草' },
  { value: 'promotion', label: '促销' },
]

interface ScriptTabProps {
  scripts: Record<string, unknown>[]
  sessionId: number
  session: Record<string, unknown> | null
  products: Record<string, unknown>[]
  onRefresh: () => void
  toast: (msg: string, severity?: 'success' | 'error' | 'info' | 'warning') => void
}
interface AiGenerateResponse { ragRefs?: LiveRagRef[]; officialReferences?: LiveOfficialReference[] }
interface CheckViolationResult { passed?: boolean; violations?: string[]; violationCount?: number; officialReferences?: LiveOfficialReference[] }
interface DeleteConfirmData { id?: unknown; sequenceNo?: unknown; scriptType?: unknown; scriptContent?: unknown; executed?: unknown; estimatedDurationSeconds?: unknown }

const READY_ENDPOINTS = [
  '/live/ai/generate-opening',
  '/live/ai/generate-product',
  '/live/ai/generate-full',
  '/live/ai/check-violation',
  '/live/script/save',
  '/live/script/delete',
  '/live/script/executed',
  '/live/script/save-to-library',
  '/live/script/save-batch-to-library',
  '/live/script/export',
].join('|')
const UNSUPPORTED_ACTIONS = 'local-script-fallback|mock-rag-refs|direct-product-write|direct-session-write'

export function ScriptTab({
  scripts,
  sessionId,
  session,
  products,
  onRefresh,
  toast,
}: ScriptTabProps) {
  const [genLoading, setGenLoading] = useState(false)
  const [productGenOpen, setProductGenOpen] = useState(false)
  const [genStyle, setGenStyle] = useState<string>(() => (session?.scriptStyle as string) || '')
  const [useKbRef, setUseKbRef] = useState(true)
  const [lastRagRefs, setLastRagRefs] = useState<LiveRagRef[] | null>(null)
  const [lastOfficialRefs, setLastOfficialRefs] = useState<LiveOfficialReference[] | null>(null)
  const [exportLoading, setExportLoading] = useState(false)
  const [saveLibLoading, setSaveLibLoading] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editContent, setEditContent] = useState('')
  const [violationResult, setViolationResult] = useState<Record<number, { passed: boolean; violations?: string[]; officialReferences?: LiveOfficialReference[] }>>({})
  const [checkingId, setCheckingId] = useState<number | null>(null)
  const [deleteConfirm, setDeleteConfirm] = useState<DeleteConfirmData | null>(null)

  const handleGenerateOpening = useCallback(async () => {
    setGenLoading(true)
    setLastRagRefs(null)
    setLastOfficialRefs(null)
    try {
      const style = genStyle || (session?.scriptStyle as string) || undefined
      const res = await generateOpening({ sessionId, genStyle: style, useKbRef }) as AiGenerateResponse
      const ragRefs = res?.ragRefs
      if (ragRefs?.length) setLastRagRefs(ragRefs)
      else setLastRagRefs(null)
      const officialRefs = res?.officialReferences
      if (officialRefs?.length) setLastOfficialRefs(officialRefs)
      else setLastOfficialRefs(null)
      toast('开场话术生成成功', 'success')
      onRefresh()
    } catch (e) {
      toast(e instanceof Error ? e.message : '生成失败', 'error')
    } finally {
      setGenLoading(false)
    }
  }, [sessionId, genStyle, session?.scriptStyle, useKbRef, onRefresh, toast])
  const handleGenerateOpeningThrottled = useThrottledCallback(handleGenerateOpening, 500)

  const handleGenerateProduct = useCallback(async (productId: number) => {
    setGenLoading(true)
    setLastRagRefs(null)
    setLastOfficialRefs(null)
    try {
      const style = genStyle || (session?.scriptStyle as string) || undefined
      const res = await generateProduct({ sessionId, productId, genStyle: style, useKbRef }) as AiGenerateResponse
      const ragRefs = res?.ragRefs
      if (ragRefs?.length) setLastRagRefs(ragRefs)
      else setLastRagRefs(null)
      const officialRefs = res?.officialReferences
      if (officialRefs?.length) setLastOfficialRefs(officialRefs)
      else setLastOfficialRefs(null)
      toast('产品话术生成成功', 'success')
      setProductGenOpen(false)
      onRefresh()
    } catch (e) {
      toast(e instanceof Error ? e.message : '生成失败', 'error')
    } finally {
      setGenLoading(false)
    }
  }, [sessionId, genStyle, session?.scriptStyle, useKbRef, onRefresh, toast])
  const handleGenerateProductThrottled = useThrottledCallback(handleGenerateProduct, 500)

  const handleGenerateFull = useCallback(async () => {
    setGenLoading(true)
    try {
      const style = genStyle || (session?.scriptStyle as string) || undefined
      await generateFull({ sessionId, genStyle: style, useKbRef })
      toast('整场话术生成成功', 'success')
      onRefresh()
    } catch (e) {
      toast(e instanceof Error ? e.message : '生成失败', 'error')
    } finally {
      setGenLoading(false)
    }
  }, [sessionId, genStyle, session?.scriptStyle, useKbRef, onRefresh, toast])
  const handleGenerateFullThrottled = useThrottledCallback(handleGenerateFull, 500)

  const handleSaveToLibrary = async (scriptId?: number) => {
    setSaveLibLoading(true)
    try {
      if (scriptId != null) {
        await saveScriptToLibrary({ scriptId })
        toast('已保存到话术库', 'success')
      } else {
        const count = await saveBatchToLibrary({ sessionId })
        toast(`已保存 ${count} 条话术到话术库`, 'success')
      }
      onRefresh()
    } catch (e) {
      toast(e instanceof Error ? e.message : '保存失败', 'error')
    } finally {
      setSaveLibLoading(false)
    }
  }

  const handleExport = async () => {
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

  const handleSaveEdit = async () => {
    if (editingId == null) return
    try {
      await saveLiveScript({
        id: editingId,
        sessionId,
        scriptContent: editContent,
      })
      toast('保存成功', 'success')
      setEditingId(null)
      setViolationResult((prev) => {
        const next = { ...prev }
        delete next[editingId]
        return next
      })
      onRefresh()
    } catch (e) {
      toast(e instanceof Error ? e.message : '保存失败', 'error')
    }
  }

  const handleCheckViolation = async (scriptId: number) => {
    setCheckingId(scriptId)
    try {
      const r = await checkViolation({ scriptId }) as CheckViolationResult
      const passed = r?.passed !== false
      const violations = r?.violations
      const violationCount = r?.violationCount ?? 0
      setViolationResult((prev) => ({ ...prev, [scriptId]: { passed, violations, officialReferences: r?.officialReferences } }))
      toast(passed ? '无违规' : `发现 ${violationCount} 处违规`, passed ? 'success' : 'error')
    } catch (e) {
      toast(e instanceof Error ? e.message : '检测失败', 'error')
    } finally {
      setCheckingId(null)
    }
  }

  const handleDelete = async () => {
    if (!deleteConfirm) return
    const id = typeof deleteConfirm.id === 'number' ? deleteConfirm.id : 0
    if (!id) return
    try {
      await deleteLiveScript(id)
      toast('删除成功', 'success')
      setDeleteConfirm(null)
      onRefresh()
    } catch (e) {
      toast(e instanceof Error ? e.message : '删除失败', 'error')
    }
  }

  const handleMarkExecuted = async (scriptId: number, currentExecuted: number) => {
    const next = currentExecuted === 1 ? 0 : 1
    try {
      await updateScriptExecuted(scriptId, next)
      toast(next === 1 ? '已标记为已执行' : '已取消执行', 'success')
      onRefresh()
    } catch (e) {
      toast(e instanceof Error ? e.message : '操作失败', 'error')
    }
  }

  const sortedScripts = [...scripts].sort((a, b) => ((a.sequenceNo as number) ?? 0) - ((b.sequenceNo as number) ?? 0))
  const totalEstSeconds = sortedScripts.reduce((sum, s) => sum + ((s.estimatedDurationSeconds as number) ?? Math.ceil(((s.scriptContent as string)?.length ?? 0) / 3)), 0)
  const ragRefTitle = (ref: LiveRagRef) => String(ref.title ?? ref.docTitle ?? '未知文档')
  const ragRefPreview = (ref: LiveRagRef) => String(ref.contentPreview ?? ref.snippet ?? '')

  return (
    <Box
      data-testid="script-tab-legacy-root"
      data-contract-scope="live-script-tab-legacy-api-panel"
      data-contract-source="scripts-prop|session-prop|products-prop|sessionId-prop|onRefresh-prop|toast-prop"
      data-ready-endpoints={READY_ENDPOINTS}
      data-unsupported-actions={UNSUPPORTED_ACTIONS}
      data-script-count={scripts.length}
      data-product-count={products.length}
      data-session-id={sessionId}
      data-generation-loading={genLoading ? 'true' : 'false'}
      data-export-loading={exportLoading ? 'true' : 'false'}
      data-save-library-loading={saveLibLoading ? 'true' : 'false'}
      data-editing-id={editingId ?? ''}
      data-checking-id={checkingId ?? ''}
      data-no-local-script-fallback="true"
      data-no-mock-rag-refs="true"
      sx={{ overflowX: 'auto' }}
    >
      <Box
        data-testid="script-tab-legacy-toolbar"
        data-contract-source={READY_ENDPOINTS}
        sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, alignItems: 'center', mb: 2, flexDirection: { xs: 'column', sm: 'row' }, alignContent: { xs: 'stretch', sm: 'flex-start' } }}
      >
        <Typography variant="subtitle2" color="text.secondary" sx={{ mr: 1 }}>
          AI 生成 / 编辑 / 违规检测
        </Typography>
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>话术风格</InputLabel>
          <Select
            data-testid="script-tab-style-select"
            inputProps={{ 'data-contract-source': 'session.scriptStyle|local-generation-options' }}
            value={genStyle}
            label="话术风格"
            onChange={(e) => setGenStyle(e.target.value)}
          >
            {SCRIPT_STYLE_OPTIONS.map((o) => (
              <MenuItem key={o.value || '_'} value={o.value}>{o.label}</MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControlLabel
          control={<Switch size="small" checked={useKbRef} onChange={(e) => setUseKbRef(e.target.checked)} color="primary" />}
          label="参考话术库"
        />
        <Button
          data-testid="script-tab-generate-opening-button"
          data-contract-source="/live/ai/generate-opening"
          data-disabled-reason={genLoading ? 'generation-loading' : 'ready'}
          size="small"
          variant="outlined"
          startIcon={<AutoAwesomeIcon />}
          onClick={handleGenerateOpeningThrottled}
          disabled={genLoading}
        >
          生成开场
        </Button>
        <Button
          data-testid="script-tab-generate-product-open-button"
          data-contract-source="/live/ai/generate-product"
          data-disabled-reason={genLoading ? 'generation-loading' : products.length === 0 ? 'no-products' : 'ready'}
          size="small"
          variant="outlined"
          startIcon={<AutoAwesomeIcon />}
          onClick={() => setProductGenOpen(true)}
          disabled={genLoading || products.length === 0}
        >
          生成产品话术
        </Button>
        <Button
          data-testid="script-tab-generate-full-button"
          data-contract-source="/live/ai/generate-full"
          data-disabled-reason={genLoading ? 'generation-loading' : products.length === 0 ? 'no-products' : 'ready'}
          size="small"
          variant="contained"
          startIcon={<AutoAwesomeIcon />}
          onClick={handleGenerateFullThrottled}
          disabled={genLoading || products.length === 0}
        >
          一键生成
        </Button>
        {scripts.length > 0 && (
          <>
            <Button
              data-testid="script-tab-save-batch-library-button"
              data-contract-source="/live/script/save-batch-to-library"
              data-disabled-reason={saveLibLoading ? 'save-library-loading' : 'ready'}
              size="small"
              variant="outlined"
              startIcon={<SaveIcon />}
              onClick={() => handleSaveToLibrary()}
              disabled={saveLibLoading}
            >
              保存到话术库
            </Button>
            <Button
              data-testid="script-tab-export-button"
              data-contract-source="/live/script/export"
              data-disabled-reason={exportLoading ? 'export-loading' : 'ready'}
              size="small"
              variant="outlined"
              startIcon={<DownloadIcon />}
              onClick={handleExport}
              disabled={exportLoading}
            >
              导出话术
            </Button>
          </>
        )}
      </Box>

      {lastOfficialRefs != null && lastOfficialRefs.length > 0 && (
        <Accordion
          data-testid="script-tab-official-ref-panel"
          data-contract-scope="live-script-tab-official-rule-ref-readonly"
          data-contract-source="/live/ai/generate-opening|/live/ai/generate-product"
          data-official-ref-count={lastOfficialRefs.length}
          data-no-mock-rag-refs="true"
          sx={{ mb: 2 }}
          defaultExpanded
        >
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <GavelIcon sx={{ mr: 1, verticalAlign: 'middle', fontSize: 20 }} />
            <Typography variant="subtitle2">抖音官方规则引用（本次生成共 {lastOfficialRefs.length} 条）</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <OfficialReferencesPanel
              testId="script-tab-official-ref-list"
              endpoint="/live/ai/generate-opening|/live/ai/generate-product"
              references={lastOfficialRefs}
              title="生成引用"
              maxItems={8}
            />
          </AccordionDetails>
        </Accordion>
      )}

      {lastRagRefs != null && lastRagRefs.length > 0 && (
        <Accordion
          data-testid="script-tab-rag-ref-panel"
          data-contract-scope="live-script-tab-rag-ref-readonly"
          data-contract-source="/live/ai/generate-opening|/live/ai/generate-product"
          data-rag-ref-count={lastRagRefs.length}
          data-no-mock-rag-refs="true"
          sx={{ mb: 2 }}
          defaultExpanded
        >
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <MenuBookIcon sx={{ mr: 1, verticalAlign: 'middle', fontSize: 20 }} />
            <Typography variant="subtitle2">参考来源（本次生成共 {lastRagRefs.length} 条）</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Box component="ul" sx={{ m: 0, pl: 2.5, display: 'flex', flexDirection: 'column', gap: 1 }}>
              {lastRagRefs.map((ref, idx) => (
                <Box
                  data-testid="script-tab-rag-ref-row"
                  data-contract-source="ragRefs-response"
                  data-score={String(ref.score ?? '')}
                  component="li"
                  key={String(ref.chunkId ?? ref.docId ?? idx)}
                  sx={{ listStyle: 'none' }}
                >
                  <Typography variant="body2" color="text.secondary">
                    「{ragRefTitle(ref)}」
                    {ref.score != null && <Chip size="small" label={`相关度 ${((ref.score as number) * 100).toFixed(0)}%`} sx={{ ml: 0.5, height: 20 }} />}
                  </Typography>
                  {!!ref.contentPreview && (
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.25 }}>
                      {ragRefPreview(ref)}
                    </Typography>
                  )}
                </Box>
              ))}
            </Box>
          </AccordionDetails>
        </Accordion>
      )}

      {productGenOpen && (
        <Dialog
          open
          onClose={() => setProductGenOpen(false)}
          maxWidth="xs"
          fullWidth
          PaperProps={{
            'data-testid': 'script-tab-product-generate-dialog',
            'data-contract-scope': 'live-script-tab-product-generate-dialog',
            'data-contract-source': 'products-prop|/live/ai/generate-product',
            'data-product-count': products.length,
            'data-generation-loading': genLoading ? 'true' : 'false',
            'data-no-local-product-fallback': 'true',
          } as Record<string, string | number>}
        >
          <DialogTitle>选择产品</DialogTitle>
          <DialogContent>
            <List>
              {products
                .sort((a, b) => ((a.position as number) ?? 0) - ((b.position as number) ?? 0))
                .map((p) => (
                  <ListItemButton
                    data-testid="script-tab-product-generate-row"
                    data-contract-source="/live/ai/generate-product"
                    data-product-id={String(p.productId ?? '')}
                    key={String(p.id)}
                    onClick={() => handleGenerateProductThrottled(p.productId as number)}
                  >
                    <ListItemText primary={String(p.productName ?? p.id)} />
                  </ListItemButton>
                ))}
            </List>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setProductGenOpen(false)}>取消</Button>
          </DialogActions>
        </Dialog>
      )}

      {scripts.length === 0 ? (
        <Typography
          data-testid="script-tab-empty-state"
          data-contract-scope="live-script-tab-empty-state"
          data-contract-source="scripts-prop"
          data-no-local-script-fallback="true"
          color="text.secondary"
          sx={{ py: 4, textAlign: 'center' }}
        >
          暂无话术，请先添加选品后点击「一键生成」或分别生成
        </Typography>
      ) : (
        <Box
          data-testid="script-tab-script-list"
          data-contract-source="scripts-prop"
          data-row-count={sortedScripts.length}
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          {totalEstSeconds > 0 && (
            <Typography
              data-testid="script-tab-estimated-duration"
              data-contract-source="scripts-prop.estimatedDurationSeconds|scriptContent-length"
              data-estimated-seconds={totalEstSeconds}
              variant="body2"
              color="text.secondary"
            >
              整场话术预计时长：约 {Math.ceil(totalEstSeconds / 60)} 分钟
            </Typography>
          )}
          {sortedScripts.map((row, idx) => {
            const id = row.id as number
            const isEditing = editingId === id
            const content = isEditing ? editContent : (row.scriptContent as string) ?? '-'
            const vr = violationResult[id]
            return (
              <Card
                data-testid="script-tab-script-card"
                data-contract-scope="live-script-tab-script-card"
                data-contract-source="scripts-prop"
                data-script-id={id}
                data-script-type={String(row.scriptType ?? '')}
                data-executed={row.executed ? 'true' : 'false'}
                data-editing={isEditing ? 'true' : 'false'}
                key={String(id)}
                variant="outlined"
                sx={{ p: 2 }}
              >
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Typography variant="caption" color="text.secondary">
                    #{String(typeof row.sequenceNo === 'number' ? row.sequenceNo : idx + 1)} · {(() => {
                      const t = row.scriptType as string
                      if (t === 'transition') {
                        const sortedProducts = [...products].sort((a, b) => ((a.position as number) ?? 0) - ((b.position as number) ?? 0))
                        const fromIdx = Math.floor((idx - 2) / 2)
                        const toIdx = fromIdx + 1
                        const fromName = sortedProducts[fromIdx]?.productName ?? ''
                        const toName = sortedProducts[toIdx]?.productName ?? ''
                        return fromName && toName ? `${fromName} → ${toName}` : '转场话术'
                      }
                      return SCRIPT_TYPE_LABEL[t] ?? t ?? '-'
                    })()} · {row.executed ? '已执行' : '未执行'}
                    {(row.estimatedDurationSeconds as number) > 0 && (
                      <Typography component="span" variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                        约 {(row.estimatedDurationSeconds as number)}s
                      </Typography>
                    )}
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 0.5 }}>
                    {isEditing ? (
                      <>
                        <Button
                          data-testid="script-tab-save-edit-button"
                          data-contract-source="/live/script/save"
                          size="small"
                          startIcon={<CheckIcon />}
                          onClick={handleSaveEdit}
                        >
                          保存
                        </Button>
                        <Button data-testid="script-tab-cancel-edit-button" size="small" onClick={() => { setEditingId(null); setEditContent('') }}>
                          取消
                        </Button>
                      </>
                    ) : (
                      <>
                        <Tooltip title="编辑">
                          <IconButton
                            data-testid="script-tab-edit-open-button"
                            data-contract-source="local-edit-state"
                            size="small"
                            onClick={() => {
                              setEditingId(id)
                              setEditContent((row.scriptContent as string) ?? '')
                            }}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title={row.executed ? '已执行' : '标记已执行'}>
                          <IconButton
                            data-testid="script-tab-executed-toggle-button"
                            data-contract-source="/live/script/executed"
                            size="small"
                            onClick={() => handleMarkExecuted(id, (row.executed as number) ?? 0)}
                            color={row.executed ? 'success' : 'default'}
                          >
                            {row.executed ? <CheckCircleIcon fontSize="small" /> : <PlayCircleIcon fontSize="small" />}
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="违规检测">
                          <IconButton
                            data-testid="script-tab-check-violation-button"
                            data-contract-source="/live/ai/check-violation"
                            data-disabled-reason={checkingId != null ? 'checking' : 'ready'}
                            size="small"
                            onClick={() => handleCheckViolation(id)}
                            disabled={checkingId != null}
                          >
                            <GavelIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="保存到话术库">
                          <IconButton
                            data-testid="script-tab-save-one-library-button"
                            data-contract-source="/live/script/save-to-library"
                            data-disabled-reason={saveLibLoading ? 'save-library-loading' : 'ready'}
                            size="small"
                            onClick={() => handleSaveToLibrary(id)}
                            disabled={saveLibLoading}
                          >
                            <SaveIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="删除">
                          <IconButton
                            data-testid="script-tab-delete-open-button"
                            data-contract-source="/live/script/delete"
                            size="small"
                            color="error"
                            onClick={() => setDeleteConfirm(row)}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </>
                    )}
                  </Box>
                </Box>
                {isEditing ? (
                  <TextField
                    inputProps={{
                      'data-testid': 'script-tab-edit-content-input',
                      'data-contract-source': 'local-edit-state',
                    }}
                    multiline
                    fullWidth
                    minRows={3}
                    value={editContent}
                    onChange={(e) => setEditContent(e.target.value)}
                    size="small"
                  />
                ) : (
                  <Typography sx={{ whiteSpace: 'pre-wrap' }}>{content}</Typography>
                )}
                {vr && !isEditing && (
                  <Box
                    data-testid="script-tab-violation-result"
                    data-contract-source="/live/ai/check-violation"
                    data-passed={vr.passed ? 'true' : 'false'}
                    data-official-ref-count={vr.officialReferences?.length ?? 0}
                    sx={{ mt: 1 }}
                  >
                    {vr.passed ? (
                      <Chip label="无违规" size="small" color="success" variant="outlined" />
                    ) : (
                      <Box>
                        <Chip label={`${vr.violations?.length ?? 0} 处违规`} size="small" color="warning" variant="outlined" sx={{ mr: 1 }} />
                        {vr.violations?.map((v, i) => (
                          <Chip key={i} label={v} size="small" sx={{ mr: 0.5, mt: 0.5 }} />
                        ))}
                      </Box>
                    )}
                    <Box sx={{ mt: 1 }}>
                      <OfficialReferencesPanel
                        testId="script-tab-violation-official-ref-panel"
                        endpoint="/live/ai/check-violation"
                        references={vr.officialReferences}
                        title="引用的抖音违规规则"
                        required
                        satisfied={(vr.officialReferences?.length ?? 0) > 0}
                        maxItems={3}
                      />
                    </Box>
                  </Box>
                )}
              </Card>
            )
          })}
        </Box>
      )}

      <Dialog
        open={!!deleteConfirm}
        onClose={() => setDeleteConfirm(null)}
        PaperProps={{
          'data-testid': 'script-tab-delete-dialog',
          'data-contract-scope': 'live-script-tab-delete-dialog',
          'data-contract-source': '/live/script/delete',
          'data-script-id': typeof deleteConfirm?.id === 'number' ? deleteConfirm.id : '',
        } as Record<string, string | number>}
      >
        <DialogTitle>确认删除</DialogTitle>
        <DialogContent>
          确定删除该条话术？
        </DialogContent>
        <DialogActions>
          <Button data-testid="script-tab-delete-cancel-button" onClick={() => setDeleteConfirm(null)}>取消</Button>
          <Button
            data-testid="script-tab-delete-confirm-button"
            data-contract-source="/live/script/delete"
            color="error"
            variant="contained"
            onClick={handleDelete}
          >
            删除
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
