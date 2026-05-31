import { useState, useEffect, useCallback } from 'react'
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Menu,
  MenuItem,
  TextField,
  Tooltip,
  Typography,
  Alert,
} from '@mui/material'
import BookmarkBorderIcon from '@mui/icons-material/BookmarkBorder'
import BookmarkIcon from '@mui/icons-material/Bookmark'
import SaveIcon from '@mui/icons-material/Save'
import DeleteIcon from '@mui/icons-material/Delete'
import {
  liveApi,
  type LiveGenerationPreset as GenerationPreset,
} from '@/api/live'

/** 高级配置字段的中文标签 */
const LABEL_MAP: Record<string, Record<string, string>> = {
  ipType: { phenomenal: '现象级', top: '顶级' },
  materialType: { joke: '段子', chicken_soup: '鸡汤', quote: '名言', jingle: '顺口溜', proverb: '歇后语', interactive_game: '互动' },
  scriptModule: { emotion_drive: '情绪驱动', value_creation: '价值塑造', conversion_engine: '转化引擎', trust_reinforcement: '信任加固' },
  retentionStrategy: { high_suspense: '高频悬念', high_practical: '干货密集', high_climax: '情绪高潮' },
  interactionLevel: { light: '轻互动', medium: '中互动', heavy: '强互动' },
}

interface PresetSelectorProps {
  currentStyle: string
  currentModelId: number | string | ''
  currentUseKbRef: boolean
  /** 扩展：当前高级配置 */
  currentIpType?: string
  currentMaterialType?: string
  currentScriptModule?: string
  currentRetentionStrategy?: string
  currentInteractionLevel?: string
  onApplyPreset: (preset: GenerationPreset) => void
  disabled?: boolean
}

export function PresetSelector({
  currentStyle,
  currentModelId,
  currentUseKbRef,
  currentIpType,
  currentMaterialType,
  currentScriptModule,
  currentRetentionStrategy,
  currentInteractionLevel,
  onApplyPreset,
  disabled,
}: PresetSelectorProps) {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null)
  const [presets, setPresets] = useState<GenerationPreset[]>([])
  const [saveOpen, setSaveOpen] = useState(false)
  const [saveName, setSaveName] = useState('')
  const [saving, setSaving] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const loadPresets = useCallback(async () => {
    setLoadError(null)
    try {
      const data = await liveApi.presetList()
      setPresets(data ?? [])
    } catch (error: unknown) {
      setPresets([])
      setLoadError(`/live/generation-preset/list 加载失败：${error instanceof Error ? error.message : String(error)}`)
    }
  }, [])

  useEffect(() => {
    loadPresets()
  }, [loadPresets])

  const handleApply = (preset: GenerationPreset) => {
    setActionError(null)
    onApplyPreset(preset)
    setAnchorEl(null)
  }

  const handleSave = async () => {
    if (!saveName.trim() || saving) return
    setSaving(true)
    setActionError(null)
    try {
      await liveApi.presetSave({
        presetName: saveName.trim(),
        style: currentStyle,
        genStyle: currentStyle,
        modelId: typeof currentModelId === 'number' ? currentModelId : undefined,
        useKbRef: currentUseKbRef,
        ipType: currentIpType || undefined,
        materialType: currentMaterialType || undefined,
        scriptModule: currentScriptModule || undefined,
        retentionStrategy: currentRetentionStrategy || undefined,
        interactionLevel: currentInteractionLevel || undefined,
      } as Partial<import('@/api/live').LiveGenerationPresetSave>)
      setSaveOpen(false)
      setSaveName('')
      await loadPresets()
    } catch (error: unknown) {
      setActionError(`/live/generation-preset/save 保存失败：${error instanceof Error ? error.message : String(error)}`)
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number, e: React.MouseEvent) => {
    e.stopPropagation()
    setActionError(null)
    try {
      await liveApi.presetDelete(id)
      await loadPresets()
    } catch (error: unknown) {
      setActionError(`/live/generation-preset/delete 删除失败：${error instanceof Error ? error.message : String(error)}`)
    }
  }

  /** 将预设的高级配置渲染为 Chip 摘要 */
  const renderPresetTags = (p: GenerationPreset) => {
    const tags: string[] = []
    if (p.style) tags.push(p.style)
    if (p.useKbRef) tags.push('KB')
    if (p.ipType) tags.push(LABEL_MAP.ipType[p.ipType] ?? p.ipType)
    if (p.materialType) tags.push(LABEL_MAP.materialType[p.materialType] ?? p.materialType)
    if (p.scriptModule) tags.push(LABEL_MAP.scriptModule[p.scriptModule] ?? p.scriptModule)
    if (p.retentionStrategy) tags.push(LABEL_MAP.retentionStrategy[p.retentionStrategy] ?? p.retentionStrategy)
    if (p.interactionLevel) tags.push(LABEL_MAP.interactionLevel[p.interactionLevel] ?? p.interactionLevel)
    return tags
  }

  /** 保存对话框中展示当前配置摘要 */
  const currentTags: { label: string; color?: 'primary' | 'secondary' | 'info' | 'warning' | 'success' | 'default' }[] = [
    { label: `风格: ${currentStyle || '默认'}` },
    { label: `模型: ${currentModelId || '未选择'}` },
    { label: `知识库: ${currentUseKbRef ? '开' : '关'}`, color: currentUseKbRef ? 'info' : 'default' },
  ]
  if (currentIpType) currentTags.push({ label: `IP: ${LABEL_MAP.ipType[currentIpType] ?? currentIpType}`, color: 'primary' })
  if (currentMaterialType) currentTags.push({ label: `素材: ${LABEL_MAP.materialType[currentMaterialType] ?? currentMaterialType}`, color: 'secondary' })
  if (currentScriptModule) currentTags.push({ label: LABEL_MAP.scriptModule[currentScriptModule] ?? currentScriptModule, color: 'info' })
  if (currentRetentionStrategy) currentTags.push({ label: LABEL_MAP.retentionStrategy[currentRetentionStrategy] ?? currentRetentionStrategy, color: 'warning' })
  if (currentInteractionLevel) currentTags.push({ label: LABEL_MAP.interactionLevel[currentInteractionLevel] ?? currentInteractionLevel, color: 'success' })

  return (
    <>
      <Tooltip title="生成配置预设">
        <IconButton
          size="small"
          onClick={(e) => setAnchorEl(e.currentTarget)}
          disabled={disabled}
          color={presets.some((p) => p.isDefault) ? 'primary' : 'default'}
          data-testid="generation-preset-open-button"
          data-contract-source="/live/generation-preset/list"
          data-no-local-preset-fallback="true"
        >
          {presets.some((p) => p.isDefault) ? <BookmarkIcon fontSize="small" /> : <BookmarkBorderIcon fontSize="small" />}
        </IconButton>
      </Tooltip>
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
        slotProps={{ paper: { sx: { minWidth: 260, maxHeight: 360 } } }}
      >
        <Box
          data-testid="generation-preset-menu"
          data-contract-scope="live-generation-preset-selector"
          data-ready-endpoints="/live/generation-preset/list|/live/generation-preset/save|/live/generation-preset/delete"
          data-unsupported-actions="local-preset-fallback|script-generation|script-mutation|product-mutation"
          data-preset-count={presets.length}
          data-state={loadError || actionError ? 'error' : 'ready'}
          data-no-local-preset-fallback="true"
          sx={{ py: 0.5 }}
        >
        {(loadError || actionError) && (
          <Box sx={{ px: 1, py: 0.5 }}>
            <Alert
              severity="warning"
              data-testid={loadError ? 'generation-preset-load-error' : 'generation-preset-action-error'}
              data-contract-source={loadError ? '/live/generation-preset/list' : actionError?.includes('/delete') ? '/live/generation-preset/delete' : '/live/generation-preset/save'}
              data-no-local-preset-fallback="true"
              sx={{ fontSize: 12 }}
            >
              {loadError || actionError}
            </Alert>
          </Box>
        )}
        {presets.length === 0 && !loadError && (
          <MenuItem disabled>
            <Typography
              variant="body2"
              color="text.secondary"
              data-testid="generation-preset-empty-state"
              data-contract-source="/live/generation-preset/list"
              data-no-local-preset-fallback="true"
            >
              暂无预设
            </Typography>
          </MenuItem>
        )}
        {presets.map((p) => {
          const tags = renderPresetTags(p)
          return (
            <MenuItem
              key={p.id}
              onClick={() => handleApply(p)}
              data-testid="generation-preset-item"
              data-contract-source="/live/generation-preset/list"
              sx={{ display: 'flex', justifyContent: 'space-between', gap: 1 }}
            >
              <Box sx={{ minWidth: 0 }}>
                <Typography variant="body2" fontWeight={600}>{p.presetName ?? ''}</Typography>
                {tags.length > 0 && (
                  <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mt: 0.25 }}>
                    {tags.slice(0, 4).map((tag) => (
                      <Chip key={tag} label={tag} size="small" sx={{ height: 18, fontSize: '0.7rem' }} />
                    ))}
                    {tags.length > 4 && (
                      <Chip label={`+${tags.length - 4}`} size="small" variant="outlined" sx={{ height: 18, fontSize: '0.7rem' }} />
                    )}
                  </Box>
                )}
              </Box>
              <IconButton
                size="small"
                onClick={(e) => handleDelete(p.id, e)}
                data-testid="generation-preset-delete-button"
                data-contract-source="/live/generation-preset/delete"
              >
                <DeleteIcon fontSize="small" />
              </IconButton>
            </MenuItem>
          )
        })}
        <MenuItem
          onClick={() => { setAnchorEl(null); setActionError(null); setSaveOpen(true) }}
          data-testid="generation-preset-save-open"
          data-contract-source="/live/generation-preset/save"
        >
          <SaveIcon fontSize="small" sx={{ mr: 1 }} />
          <Typography variant="body2">保存当前配置</Typography>
        </MenuItem>
        </Box>
      </Menu>

      <Dialog
        open={saveOpen}
        onClose={() => setSaveOpen(false)}
        maxWidth="xs"
        fullWidth
        data-testid="generation-preset-save-dialog"
        data-contract-source="/live/generation-preset/save"
        data-no-local-preset-fallback="true"
      >
        <DialogTitle>保存生成预设</DialogTitle>
        <DialogContent>
          {actionError && (
            <Alert
              severity="warning"
              data-testid="generation-preset-save-error"
              data-contract-source="/live/generation-preset/save"
              data-input-preserved="true"
              sx={{ mb: 1 }}
            >
              {actionError}
            </Alert>
          )}
          <TextField
            autoFocus
            fullWidth
            size="small"
            label="预设名称"
            value={saveName}
            onChange={(e) => setSaveName(e.target.value)}
            inputProps={{
              'data-testid': 'generation-preset-name-input',
              'data-contract-source': '/live/generation-preset/save',
            }}
            sx={{ mt: 1 }}
          />
          <Box sx={{ mt: 1.5, display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
            {currentTags.map((t) => (
              <Chip key={t.label} label={t.label} size="small" color={t.color ?? 'default'} variant="outlined" />
            ))}
          </Box>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
            以上配置将一起保存到预设中
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSaveOpen(false)}>取消</Button>
          <Button
            variant="contained"
            onClick={handleSave}
            disabled={!saveName.trim() || saving}
            data-testid="generation-preset-save-submit"
            data-contract-source="/live/generation-preset/save"
          >
            保存
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}
