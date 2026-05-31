import { memo, useState, useEffect, useCallback, useMemo } from 'react'
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  CircularProgress,
  Alert,
  Chip,
  IconButton,
} from '@mui/material'
import HistoryIcon from '@mui/icons-material/History'
import RestoreIcon from '@mui/icons-material/Restore'
import CompareArrowsIcon from '@mui/icons-material/CompareArrows'
import CloseIcon from '@mui/icons-material/Close'
import { alpha } from '@mui/material/styles'
import { searchScriptVersions, rollbackScriptVersion, getScriptVersion } from '@/api/live.versions'
import { useToast } from '@/contexts/ToastContext'

export interface VersionHistoryPanelProps {
  scripts: Record<string, unknown>[]
  sessionId: number | ''
  onScriptSelect: (id: number | null) => void
  selectedScriptId: number | null
}

interface VersionItem {
  id: number
  versionNumber?: number
  versionNo?: number
  scriptContent: string
  changeReason?: string
  createTime?: string
  isCurrent?: number
  source?: string
  basedOnVersionId?: number
}

interface ScriptVersionVO {
  scriptContent?: string
  [key: string]: unknown
}

const SOURCE_LABEL: Record<string, { text: string; color: 'info' | 'success' } | null> = {
  manual: null,
  auto_improve: { text: 'AI优化', color: 'info' },
  ab_winner: { text: 'AB胜出', color: 'success' },
}

const VERSION_HISTORY_READY_ENDPOINTS = [
  '/live/script-version/search',
  '/live/script-version/get',
  '/live/script-version/rollback',
]

const VERSION_HISTORY_UNSUPPORTED_ACTIONS = [
  'local-version-fallback',
  'silent-version-load-failure',
  'direct-script-save',
  'script-mutation',
]

export const VersionHistoryPanel = memo(function VersionHistoryPanel({
  scripts,
  onScriptSelect,
  selectedScriptId,
}: VersionHistoryPanelProps) {
  const toast = useToast()
  const [versions, setVersions] = useState<VersionItem[]>([])
  const [loading, setLoading] = useState(false)
  const [rollbackLoading, setRollbackLoading] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)

  const loadVersions = useCallback(async () => {
    if (!selectedScriptId) return
    setLoading(true)
    setLoadError(null)
    try {
      const res = await searchScriptVersions<VersionItem>({
        scriptId: selectedScriptId,
        page: 0,
        rows: 50,
        sortName: 'versionNumber',
        sortOrder: 'desc',
      })
      const list: VersionItem[] = res.list
      setVersions(list)
    } catch (e) {
      setVersions([])
      setLoadError(`/live/script-version/search 版本历史加载失败：${e instanceof Error ? e.message : '未知错误'}`)
    } finally {
      setLoading(false)
    }
  }, [selectedScriptId])

  useEffect(() => {
    loadVersions()
  }, [loadVersions])

  const handleRollback = async (versionNumber: number) => {
    if (!selectedScriptId) return
    setRollbackLoading(true)
    try {
      await rollbackScriptVersion({
        scriptId: selectedScriptId,
        versionNumber,
      })
      toast('回滚成功', 'success')
      void loadVersions()
    } catch (e) {
      toast(e instanceof Error ? e.message : '回滚失败', 'error')
    } finally {
      setRollbackLoading(false)
    }
  }

  // ── 版本对比（内联 diff） ──
  const [diffVersionA, setDiffVersionA] = useState(0)
  const [diffVersionB, setDiffVersionB] = useState(0)
  const [diffContentA, setDiffContentA] = useState('')
  const [diffContentB, setDiffContentB] = useState('')
  const [diffLoading, setDiffLoading] = useState(false)
  const [inlineDiffActive, setInlineDiffActive] = useState(false)

  const handleCompare = async (targetVersion: number) => {
    if (!selectedScriptId) return
    const currentVersion = versions.find((v) => v.isCurrent === 1)
    if (!currentVersion) return
    setDiffVersionA(targetVersion)
    setDiffVersionB(currentVersion.versionNumber ?? currentVersion.versionNo ?? 0)
    setDiffLoading(true)
    setInlineDiffActive(true)
    try {
      const [resA, resB] = await Promise.all([
        getScriptVersion(selectedScriptId, targetVersion),
        getScriptVersion(selectedScriptId, currentVersion.versionNumber ?? currentVersion.versionNo ?? 0),
      ])
      setDiffContentA((resA as ScriptVersionVO)?.scriptContent ?? '')
      setDiffContentB((resB as ScriptVersionVO)?.scriptContent ?? '')
    } catch {
      toast('获取版本内容失败', 'error')
    } finally {
      setDiffLoading(false)
    }
  }

  const scriptOptions = useMemo(
    () =>
      scripts
        .filter((s) => s.id)
        .map((s) => ({
          id: s.id as number,
          label: `#${s.id} ${String(s.scriptType ?? '')} - ${String(s.scriptContent ?? '').slice(0, 30)}...`,
        })),
    [scripts],
  )

  const validScriptIds = useMemo(() => new Set(scriptOptions.map((s) => s.id)), [scriptOptions])

  /** MUI Select 受控值必须在 MenuItem 中存在，否则 InputBase 会反复同步 → Maximum update depth */
  const selectValue =
    selectedScriptId != null && validScriptIds.has(selectedScriptId) ? selectedScriptId : ''

  return (
    <>
    <Card
      variant="outlined"
      data-testid="version-history-panel"
      data-contract-scope="live-version-history-panel"
      data-ready-endpoints={VERSION_HISTORY_READY_ENDPOINTS.join('|')}
      data-unsupported-actions={VERSION_HISTORY_UNSUPPORTED_ACTIONS.join('|')}
      data-no-local-version-fallback="true"
      data-selected-script-id={selectedScriptId ?? ''}
      data-version-count={versions.length}
      data-loading={loading ? 'true' : 'false'}
      data-rollback-loading={rollbackLoading ? 'true' : 'false'}
      data-load-state={loadError ? 'error' : (loading ? 'loading' : (!selectedScriptId ? 'unselected' : 'ready'))}
    >
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }} data-testid="version-history-header" data-contract-source="/live/script-version/search">
          <HistoryIcon />
          <Typography variant="subtitle2">版本历史</Typography>
        </Box>

        <FormControl size="small" fullWidth sx={{ mb: 2 }} data-testid="version-history-script-select-surface" data-contract-source="scripts-prop|onScriptSelect-prop">
          <InputLabel>选择话术</InputLabel>
          <Select
            value={selectValue}
            label="选择话术"
            onChange={(e) => onScriptSelect(e.target.value === '' ? null : Number(e.target.value))}
            data-testid="version-history-script-select"
            data-contract-source="onScriptSelect-prop"
          >
            <MenuItem value="">请选择</MenuItem>
            {scriptOptions.map((s) => (
              <MenuItem key={s.id} value={s.id}>{s.label}</MenuItem>
            ))}
          </Select>
        </FormControl>

        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }} data-testid="version-history-loading" data-contract-source="/live/script-version/search">
            <CircularProgress size={24} />
          </Box>
        ) : loadError ? (
          <Alert
            severity="error"
            data-testid="version-history-load-error"
            data-contract-source="/live/script-version/search"
            data-no-local-version-fallback="true"
          >
            {loadError}
          </Alert>
        ) : !selectedScriptId ? (
          <Typography
            variant="body2"
            color="text.secondary"
            data-testid="version-history-unselected-state"
            data-contract-source="selectedScriptId-prop"
          >
            请选择一段话术查看版本历史
          </Typography>
        ) : versions.length === 0 ? (
          <Alert
            severity="info"
            data-testid="version-history-empty-state"
            data-contract-source="/live/script-version/search"
            data-no-local-version-fallback="true"
          >
            暂无版本记录
          </Alert>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }} data-testid="version-history-list" data-contract-source="/live/script-version/search">
            {versions.map((v) => (
              <Box
                key={v.id}
                data-testid="version-history-item"
                data-contract-source="/live/script-version/search"
                data-version-number={v.versionNumber ?? v.versionNo ?? 0}
                data-current={v.isCurrent === 1 ? 'true' : 'false'}
                data-source={v.source ?? ''}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  p: 1,
                  borderRadius: 1,
                  border: 1,
                  borderColor: 'divider',
                  bgcolor: v.isCurrent ? 'action.selected' : undefined,
                }}
              >
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, flexWrap: 'wrap' }}>
                    <Typography variant="body2">v{v.versionNumber ?? v.versionNo ?? '-'}</Typography>
                    {v.isCurrent === 1 && <Chip label="当前" size="small" color="primary" />}
                    {v.source && v.source !== 'manual' && SOURCE_LABEL[v.source] && (
                      <Chip size="small" label={SOURCE_LABEL[v.source]!.text} color={SOURCE_LABEL[v.source]!.color} />
                    )}
                  </Box>
                  {v.basedOnVersionId && (
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                      基于版本 #{v.basedOnVersionId} 优化
                    </Typography>
                  )}
                  {v.changeReason && (
                    <Typography variant="caption" color="text.secondary">{v.changeReason}</Typography>
                  )}
                  {v.createTime && (
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                      {String(v.createTime).slice(0, 16).replace('T', ' ')}
                    </Typography>
                  )}
                </Box>
                {v.isCurrent !== 1 && (
                  <Box sx={{ display: 'flex', gap: 0.5 }}>
                    <Button
                      size="small"
                      startIcon={<CompareArrowsIcon />}
                      onClick={() => handleCompare(v.versionNumber ?? v.versionNo ?? 0)}
                      data-testid="version-history-compare-button"
                      data-contract-source="/live/script-version/get"
                    >
                      对比
                    </Button>
                    <Button
                      size="small"
                      startIcon={<RestoreIcon />}
                      onClick={() => handleRollback(v.versionNumber ?? v.versionNo ?? 0)}
                      disabled={rollbackLoading}
                      data-testid="version-history-rollback-button"
                      data-contract-source="/live/script-version/rollback"
                      data-disabled-reason={rollbackLoading ? 'rollback-loading' : 'ready'}
                    >
                      回滚
                    </Button>
                  </Box>
                )}
              </Box>
            ))}
          </Box>
        )}
      </CardContent>
    </Card>

    {/* Inline diff view (replaces modal) */}
    {inlineDiffActive && (
      <Card
        variant="outlined"
        sx={{ mt: 1 }}
        data-testid="version-history-inline-diff-panel"
        data-contract-scope="live-version-history-inline-diff"
        data-contract-source="/live/script-version/get"
        data-diff-version-a={diffVersionA}
        data-diff-version-b={diffVersionB}
        data-loading={diffLoading ? 'true' : 'false'}
      >
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
            <Typography variant="subtitle2">
              v{diffVersionA} vs v{diffVersionB}（当前）
            </Typography>
            <IconButton size="small" onClick={() => setInlineDiffActive(false)} data-testid="version-history-inline-diff-close-button" data-contract-source="close-inline-diff">
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>
          {diffLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }} data-testid="version-history-inline-diff-loading" data-contract-source="/live/script-version/get">
              <CircularProgress size={24} />
            </Box>
          ) : (
            <Box sx={{ display: 'flex', gap: 1 }}>
              {/* Old version */}
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography variant="caption" color="error.main" sx={{ fontWeight: 600 }}>
                  v{diffVersionA}（旧）
                </Typography>
                <Box
                  data-testid="version-history-inline-diff-old-surface"
                  data-contract-source="/live/script-version/get"
                  data-diff-tone="error"
                  data-version-number={diffVersionA}
                  sx={(theme) => ({
                    mt: 0.5,
                    p: 1,
                    borderRadius: 1,
                    border: 1,
                    borderColor: alpha(theme.palette.error[theme.palette.mode === 'dark' ? 'light' : 'main'], 0.54),
                    bgcolor: alpha(theme.palette.error.main, theme.palette.mode === 'dark' ? 0.14 : 0.06),
                    whiteSpace: 'pre-wrap',
                    fontSize: '0.8rem',
                    maxHeight: 300,
                    overflow: 'auto',
                  })}
                >
                  {diffContentA || '(空)'}
                </Box>
              </Box>
              {/* New version */}
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography variant="caption" color="success.main" sx={{ fontWeight: 600 }}>
                  v{diffVersionB}（当前）
                </Typography>
                <Box
                  data-testid="version-history-inline-diff-new-surface"
                  data-contract-source="/live/script-version/get"
                  data-diff-tone="success"
                  data-version-number={diffVersionB}
                  sx={(theme) => ({
                    mt: 0.5,
                    p: 1,
                    borderRadius: 1,
                    border: 1,
                    borderColor: alpha(theme.palette.success[theme.palette.mode === 'dark' ? 'light' : 'main'], 0.54),
                    bgcolor: alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.14 : 0.06),
                    whiteSpace: 'pre-wrap',
                    fontSize: '0.8rem',
                    maxHeight: 300,
                    overflow: 'auto',
                  })}
                >
                  {diffContentB || '(空)'}
                </Box>
              </Box>
            </Box>
          )}
        </CardContent>
      </Card>
    )}
    </>
  )
})
