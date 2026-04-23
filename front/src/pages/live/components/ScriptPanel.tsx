import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  LinearProgress,
  Paper,
  FormControlLabel,
  Switch,
} from '@mui/material'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import PsychologyIcon from '@mui/icons-material/Psychology'
import SaveIcon from '@mui/icons-material/Save'
import DownloadIcon from '@mui/icons-material/Download'
import type { LiveProduct } from '@/api/live-product'
import type { LiveScript } from '@/api/live-script'
import { SCRIPT_STYLE_OPTIONS } from './constants'
import { ScriptSection } from './ScriptSection'
import type { ScriptSectionProps } from './ScriptSection'

export interface ScriptSectionData {
  key: string
  title: string
  subtitle?: string
  scripts: LiveScript[]
  product?: LiveProduct
}

export interface ScriptPanelProps {
  genStyle: string
  genLoading: boolean
  fullGenProgress: { current: number; total: number; slotType: string } | null
  totalEstSeconds: number
  scripts: LiveScript[]
  scriptSections: ScriptSectionData[]
  expandedSections: Set<string>
  chainPrompt: { scriptId: number; label: string } | null
  onGenStyleChange: (v: string) => void
  onGenerateOpening: () => void
  onProductGenOpen: () => void
  onEmotionalOpen: () => void
  onGenerateFull: () => void
  onCheckSimilarity: () => void
  onGenerateSkeleton: () => void
  onSaveToLibrary: () => void
  onExport: () => void
  onToggleSection: (key: string) => void
  onChainPromptAdjust: (scriptId: number) => void
  onChainPromptDismiss: () => void
  onBatchOpen: () => void
  productsCount: number
  selectedScriptIdsCount: number
  similarityLoading: boolean
  skeletonLoading: boolean
  saveLibLoading: boolean
  exportLoading: boolean
  sectionProps: Omit<ScriptSectionProps, 'title' | 'subtitle' | 'scripts' | 'expanded' | 'onToggle'>
  useKbRef?: boolean
  onUseKbRefChange?: (v: boolean) => void
}

export function ScriptPanel({
  genStyle,
  genLoading,
  fullGenProgress,
  totalEstSeconds,
  scripts,
  scriptSections,
  expandedSections,
  chainPrompt,
  onGenStyleChange,
  onGenerateOpening,
  onProductGenOpen,
  onEmotionalOpen,
  onGenerateFull,
  onCheckSimilarity,
  onGenerateSkeleton,
  onSaveToLibrary,
  onExport,
  onToggleSection,
  onChainPromptAdjust,
  onChainPromptDismiss,
  onBatchOpen,
  productsCount,
  selectedScriptIdsCount,
  similarityLoading,
  skeletonLoading,
  saveLibLoading,
  exportLoading,
  sectionProps,
  useKbRef = true,
  onUseKbRefChange,
}: ScriptPanelProps) {
  return (
    <Card variant="outlined" sx={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, alignItems: 'center' }}>
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>话术风格</InputLabel>
            <Select value={genStyle} label="话术风格" onChange={(e) => onGenStyleChange(e.target.value)}>
              {SCRIPT_STYLE_OPTIONS.map((o) => (
                <MenuItem key={o.value || '_'} value={o.value}>
                  {o.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          {onUseKbRefChange != null && (
            <FormControlLabel
              control={<Switch size="small" checked={useKbRef} onChange={(e) => onUseKbRefChange(e.target.checked)} color="primary" />}
              label="参考话术库"
            />
          )}
          <Button size="small" variant="outlined" startIcon={<AutoAwesomeIcon />} onClick={onGenerateOpening} disabled={genLoading}>
            生成开场
          </Button>
          <Button size="small" variant="outlined" startIcon={<AutoAwesomeIcon />} onClick={onProductGenOpen} disabled={genLoading || productsCount === 0}>
            生成产品话术
          </Button>
          <Button size="small" variant="outlined" startIcon={<PsychologyIcon />} onClick={onEmotionalOpen} disabled={genLoading}>
            插入情绪话术
          </Button>
          <Button size="small" variant="contained" startIcon={<AutoAwesomeIcon />} onClick={onGenerateFull} disabled={genLoading || productsCount === 0}>
            一键生成
          </Button>
          {selectedScriptIdsCount > 0 && (
            <Button size="small" variant="contained" color="secondary" startIcon={<AutoAwesomeIcon />} onClick={onBatchOpen}>
              批量应用 ({selectedScriptIdsCount})
            </Button>
          )}
          {scripts.length > 0 && (
            <>
              <Button size="small" variant="outlined" startIcon={<PsychologyIcon />} onClick={onCheckSimilarity} disabled={similarityLoading || scripts.length < 2}>
                {similarityLoading ? '检测中...' : '相似度检测'}
              </Button>
              <Button size="small" variant="outlined" startIcon={<AutoAwesomeIcon />} onClick={onGenerateSkeleton} disabled={skeletonLoading}>
                {skeletonLoading ? '生成中...' : '骨架生成'}
              </Button>
              <Button size="small" variant="outlined" startIcon={<SaveIcon />} onClick={onSaveToLibrary} disabled={saveLibLoading}>
                保存到话术库
              </Button>
              <Button size="small" variant="outlined" startIcon={<DownloadIcon />} onClick={onExport} disabled={exportLoading}>
                导出
              </Button>
            </>
          )}
        </Box>
        {fullGenProgress && (
          <Box sx={{ mt: 1 }}>
            <LinearProgress variant="determinate" value={fullGenProgress.total > 0 ? (100 * fullGenProgress.current / fullGenProgress.total) : 0} sx={{ height: 6, borderRadius: 1 }} />
            <Typography variant="caption" color="text.secondary">
              生成中：{fullGenProgress.slotType} ({fullGenProgress.current}/{fullGenProgress.total})
            </Typography>
          </Box>
        )}
        {totalEstSeconds > 0 && (
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
            预计时长：约 {Math.ceil(totalEstSeconds / 60)} 分钟
          </Typography>
        )}
        {chainPrompt && (
          <Paper variant="outlined" sx={{ mt: 1, p: 1, bgcolor: 'action.hover', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Typography variant="body2">{chainPrompt.label}</Typography>
            <Box>
              <Button size="small" onClick={() => onChainPromptAdjust(chainPrompt.scriptId)}>
                去调整
              </Button>
              <Button size="small" onClick={onChainPromptDismiss}>忽略</Button>
            </Box>
          </Paper>
        )}
      </CardContent>
      <Box sx={{ flex: 1, overflow: 'auto', px: 2, pb: 2 }}>
        {scripts.length === 0 ? (
          <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
            暂无话术，请先添加选品后点击「一键生成」或分别生成
          </Typography>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, maxWidth: 800 }}>
            {scriptSections.map((sec) => (
              <ScriptSection
                key={sec.key}
                title={sec.title}
                subtitle={sec.subtitle}
                scripts={sec.scripts}
                expanded={expandedSections.has(sec.key)}
                onToggle={() => onToggleSection(sec.key)}
                {...sectionProps}
              />
            ))}
          </Box>
        )}
      </Box>
    </Card>
  )
}
