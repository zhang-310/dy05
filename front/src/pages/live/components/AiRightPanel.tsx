/**
 * AiRightPanel — 统一 AI 右侧面板
 * 合并 InlineAiAssistant（聊天）+ ScriptFlowCanvas（分析）+ 质量检测
 * Tab 切换：「AI 助手」/「话术分析」/「质量检测」
 */
import { memo, useState, useCallback } from 'react'
import {
  Box,
  Button,
  Chip,
  Tab,
  Tabs,
  Typography,
} from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ErrorIcon from '@mui/icons-material/Error'
import { AiChatPanel, type AiChatPanelProps } from './AiChatPanel'
import { AiAnalystPanel, type AiAnalystPanelProps } from './AiAnalystPanel'

export type AiRightPanelTab = 'assistant' | 'analysis' | 'quality'

export interface QualityCheckResult {
  scriptId: number
  passed: boolean
  violations?: string[]
  score?: number
}

export interface AiRightPanelProps {
  /** 当前激活 tab */
  activeTab?: AiRightPanelTab
  onTabChange?: (tab: AiRightPanelTab) => void
  /** AI 助手 */
  chatAvailable: boolean
  chatProps: Omit<AiChatPanelProps, 'fillContainer'>
  /** 话术分析 */
  analystAvailable: boolean
  analystProps: Omit<AiAnalystPanelProps, 'fillContainer'>
  /** 质量检测 */
  qualityResults?: QualityCheckResult[]
  onRunQualityCheck?: () => void
  qualityLoading?: boolean
  /** 选中文本发送到 AI 优化 */
  selectedText?: string
  onOptimizeSelected?: (text: string) => void
}

export const AiRightPanel = memo(function AiRightPanel({
  activeTab: controlledTab,
  onTabChange,
  chatAvailable,
  chatProps,
  analystAvailable,
  analystProps,
  qualityResults = [],
  onRunQualityCheck,
  qualityLoading = false,
  selectedText,
  onOptimizeSelected,
}: AiRightPanelProps) {
  const [internalTab, setInternalTab] = useState<AiRightPanelTab>('assistant')
  const tab = controlledTab ?? internalTab

  const handleTabChange = useCallback((_: unknown, value: AiRightPanelTab) => {
    if (onTabChange) onTabChange(value)
    else setInternalTab(value)
  }, [onTabChange])

  const passedCount = qualityResults.filter((r) => r.passed).length
  const failedCount = qualityResults.filter((r) => !r.passed).length

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
      {/* Tab navigation */}
      <Tabs
        value={tab}
        onChange={handleTabChange}
        variant="fullWidth"
        sx={{ flexShrink: 0, minHeight: 36, '& .MuiTab-root': { minHeight: 36, py: 0.5, fontSize: '0.8rem' } }}
      >
        <Tab label="AI 助手" value="assistant" />
        <Tab label="话术分析" value="analysis" disabled={!analystAvailable} />
        <Tab
          label={
            qualityResults.length > 0
              ? <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>质量检测 <Chip size="small" label={`${passedCount}/${qualityResults.length}`} sx={{ height: 18, fontSize: '0.7rem' }} /></Box>
              : '质量检测'
          }
          value="quality"
        />
      </Tabs>

      {/* Selected text optimization hint */}
      {selectedText && tab === 'assistant' && onOptimizeSelected && (
        <Box sx={{ px: 1.5, py: 0.75, bgcolor: 'action.hover', borderBottom: 1, borderColor: 'divider', display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="caption" color="text.secondary" noWrap sx={{ flex: 1 }}>
            已选中: {selectedText.slice(0, 30)}{selectedText.length > 30 ? '…' : ''}
          </Typography>
          <Button size="small" variant="outlined" onClick={() => onOptimizeSelected(selectedText)}>
            发送优化
          </Button>
        </Box>
      )}

      {/* Tab content */}
      <Box sx={{ flex: 1, overflow: 'auto', display: tab === 'assistant' ? 'flex' : 'none', flexDirection: 'column' }}>
        {chatAvailable ? (
          <AiChatPanel {...chatProps} />
        ) : (
          <Box sx={{ p: 2, textAlign: 'center' }}>
            <Typography variant="body2" color="text.secondary">请先选中话术开始编辑</Typography>
          </Box>
        )}
      </Box>

      <Box sx={{ flex: 1, overflow: 'auto', display: tab === 'analysis' ? 'flex' : 'none', flexDirection: 'column' }}>
        {analystAvailable ? (
          <AiAnalystPanel {...analystProps} />
        ) : (
          <Box sx={{ p: 2, textAlign: 'center' }}>
            <Typography variant="body2" color="text.secondary">请先选中话术进行分析</Typography>
          </Box>
        )}
      </Box>

      <Box sx={{ flex: 1, overflow: 'auto', display: tab === 'quality' ? 'flex' : 'none', flexDirection: 'column', p: 1.5 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
          <Typography variant="subtitle2">质量检测</Typography>
          <Button
            size="small"
            variant="outlined"
            onClick={onRunQualityCheck}
            disabled={qualityLoading}
          >
            {qualityLoading ? '检测中…' : '运行检测'}
          </Button>
        </Box>

        {qualityResults.length === 0 ? (
          <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', mt: 4 }}>
            点击「运行检测」检查话术质量
          </Typography>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {/* Summary */}
            <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
              {passedCount > 0 && (
                <Chip icon={<CheckCircleIcon />} label={`${passedCount} 通过`} color="success" size="small" variant="outlined" />
              )}
              {failedCount > 0 && (
                <Chip icon={<ErrorIcon />} label={`${failedCount} 违规`} color="error" size="small" variant="outlined" />
              )}
            </Box>
            {/* Detail list */}
            {qualityResults.map((r) => (
              <Box
                key={r.scriptId}
                sx={{
                  p: 1,
                  borderRadius: 1,
                  border: 1,
                  borderColor: r.passed ? 'success.light' : 'error.light',
                  bgcolor: r.passed ? 'success.50' : 'error.50',
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  {r.passed ? <CheckCircleIcon fontSize="small" color="success" /> : <ErrorIcon fontSize="small" color="error" />}
                  <Typography variant="body2">话术 #{r.scriptId}</Typography>
                  {r.score != null && (
                    <Chip label={`${r.score}分`} size="small" sx={{ height: 18, fontSize: '0.7rem', ml: 'auto' }} />
                  )}
                </Box>
                {!r.passed && r.violations && r.violations.length > 0 && (
                  <Box sx={{ mt: 0.5, pl: 3 }}>
                    {r.violations.map((v, i) => (
                      <Typography key={i} variant="caption" color="error" display="block">• {v}</Typography>
                    ))}
                  </Box>
                )}
              </Box>
            ))}
          </Box>
        )}
      </Box>
    </Box>
  )
})
