import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  Chip,
  Paper,
  CircularProgress,
} from '@mui/material'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import CheckIcon from '@mui/icons-material/Check'
import LibraryBooksIcon from '@mui/icons-material/LibraryBooks'
import { SCRIPT_TYPE_LABEL, DIMENSION_OPTIONS, REFINE_SUGGESTIONS } from './constants'

const AI_CHAT_READY_ENDPOINTS = [
  '/live/ai/chat-for-script',
  '/live/ai/chat-for-script-sse',
  '/live/script/save',
  '/live/ai/save-to-copy-if-passed',
]

const AI_CHAT_CONTEXT_SOURCES = [
  'editing-script-prop',
  'chat-state-props',
  'selected-model-owned-by-parent',
]

const AI_CHAT_UNSUPPORTED_ACTIONS = [
  'direct-network-call',
  'local-chat-response-fallback',
  'script-mutation-in-panel',
  'copy-library-mutation-in-panel',
  'shortvideo-mutation',
]

export interface AiChatPanelProps {
  editingScript: Record<string, unknown> | null
  chatPersona: string
  chatScene: string
  chatDurationSec: number | ''
  chatDimensions: string[]
  chatMessage: string
  chatResponse: string
  chatLoading: boolean
  chatHistoryLength: number
  copySaveLoading: boolean
  onPersonaChange: (v: string) => void
  onSceneChange: (v: string) => void
  onDurationChange: (v: number | '') => void
  onDimensionsChange: (v: string[]) => void
  onMessageChange: (v: string) => void
  onHistoryClear: () => void
  onSend: () => void
  onApply: () => void
  onSaveToCopy: () => void
}

export function AiChatPanel({
  editingScript,
  chatPersona,
  chatScene,
  chatDurationSec,
  chatDimensions,
  chatMessage,
  chatResponse,
  chatLoading,
  chatHistoryLength,
  copySaveLoading,
  onPersonaChange,
  onSceneChange,
  onDurationChange,
  onDimensionsChange,
  onMessageChange,
  onHistoryClear,
  onSend,
  onApply,
  onSaveToCopy,
}: AiChatPanelProps) {
  const canSend = chatHistoryLength > 0 || chatPersona.trim() || chatScene.trim() || chatMessage.trim() || chatDimensions.length > 0 || (chatDurationSec !== '' && Number(chatDurationSec) > 0)
  const scriptId = editingScript?.id == null ? '' : String(editingScript.id)
  const scriptType = String(editingScript?.scriptType ?? '')
  const disabledReason = chatLoading ? 'chat-loading' : canSend ? 'ready' : 'no-context-or-message'
  const responseState = chatLoading ? 'loading' : chatResponse ? 'ready' : 'empty'

  return (
    <Card
      variant="outlined"
      data-testid="ai-chat-panel-root"
      data-contract-scope="live-ai-chat-props-bridge"
      data-ready-endpoints={AI_CHAT_READY_ENDPOINTS.join('|')}
      data-context-sources={AI_CHAT_CONTEXT_SOURCES.join('|')}
      data-unsupported-actions={AI_CHAT_UNSUPPORTED_ACTIONS.join('|')}
      data-script-id={scriptId}
      data-script-type={scriptType}
      data-history-count={chatHistoryLength}
      data-dimension-count={chatDimensions.length}
      data-response-state={responseState}
      data-can-send={canSend ? 'true' : 'false'}
      data-chat-loading={chatLoading ? 'true' : 'false'}
      data-copy-save-loading={copySaveLoading ? 'true' : 'false'}
      data-no-direct-api-request="true"
      data-no-local-chat-fallback="true"
      sx={{
        width: { xs: '100%', sm: 400 },
        maxWidth: '100%',
        flexShrink: 0,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}
    >
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 }, display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <AutoAwesomeIcon color="primary" />
            <Typography variant="subtitle2">AI 写作助手</Typography>
          </Box>
          {chatHistoryLength > 0 && (
            <Button
              size="small"
              onClick={onHistoryClear}
              data-testid="ai-chat-clear-history-button"
              data-contract-source="onHistoryClear-prop"
            >
              清空对话
            </Button>
          )}
        </Box>
        <Typography
          variant="caption"
          color="text.secondary"
          data-testid="ai-chat-context-caption"
          data-contract-source="editing-script-prop"
          sx={{ display: 'block', mb: 1 }}
        >
          填写人设、场景、时长、考虑维度，AI 生成即用型话术，您可直接粘贴后微调
        </Typography>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, mb: 1 }}>
          <TextField
            size="small"
            fullWidth
            label="人设"
            placeholder="如：美妆达人小美"
            value={chatPersona}
            onChange={(e) => onPersonaChange(e.target.value)}
            inputProps={{
              'data-testid': 'ai-chat-persona-input',
              'data-contract-source': 'chatPersona-prop',
            }}
          />
          <TextField
            size="small"
            fullWidth
            label="场景"
            placeholder="如：护肤品套盒清仓"
            value={chatScene}
            onChange={(e) => onSceneChange(e.target.value)}
            inputProps={{
              'data-testid': 'ai-chat-scene-input',
              'data-contract-source': 'chatScene-prop',
            }}
          />
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            <TextField
              size="small"
              type="number"
              label="时长(秒)"
              placeholder="如：120"
              value={chatDurationSec === '' ? '' : chatDurationSec}
              onChange={(e) => onDurationChange(e.target.value === '' ? '' : (Number(e.target.value) || 0))}
              sx={{ width: 100 }}
              inputProps={{
                min: 0,
                'data-testid': 'ai-chat-duration-input',
                'data-contract-source': 'chatDurationSec-prop',
              }}
            />
            <Typography
              variant="caption"
              color="text.secondary"
              data-testid="ai-chat-script-type-label"
              data-contract-source="editing-script-prop"
            >
              话术类型：{SCRIPT_TYPE_LABEL[editingScript?.scriptType as string] ?? editingScript?.scriptType ?? '-'}
            </Typography>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>考虑维度</Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
              {DIMENSION_OPTIONS.map((d) => {
                const active = chatDimensions.includes(d)
                return (
                  <Chip
                    key={d}
                    label={d}
                    size="small"
                    variant={active ? 'filled' : 'outlined'}
                    color={active ? 'primary' : 'default'}
                    onClick={() => onDimensionsChange(active ? chatDimensions.filter((x) => x !== d) : [...chatDimensions, d])}
                    data-testid="ai-chat-dimension-chip"
                    data-contract-source="chatDimensions-prop"
                    data-dimension-value={d}
                    data-active={active ? 'true' : 'false'}
                    sx={{ cursor: 'pointer' }}
                  />
                )
              })}
            </Box>
          </Box>
        </Box>
        <TextField
          multiline
          minRows={5}
          size="small"
          fullWidth
          label="需求描述"
          placeholder="如：帮我写一段开播话术，重点拉停留、引导关注；或：再短一点、换成促销风格"
          value={chatMessage}
          onChange={(e) => onMessageChange(e.target.value)}
          disabled={chatLoading}
          inputProps={{
            'data-testid': 'ai-chat-message-input',
            'data-contract-source': 'chatMessage-prop',
          }}
          sx={{ mb: 1, '& .MuiInputBase-input': { fontSize: '0.9rem' } }}
        />
        <Box sx={{ display: 'flex', gap: 0.5, mb: 1 }}>
          {REFINE_SUGGESTIONS.map((s) => (
            <Chip
              key={s}
              label={s}
              size="small"
              variant="outlined"
              onClick={() => onMessageChange(chatMessage ? `${chatMessage}\n${s}` : s)}
              data-testid="ai-chat-refine-suggestion-chip"
              data-contract-source="onMessageChange-prop"
              data-suggestion={s}
              sx={{ cursor: 'pointer' }}
            />
          ))}
        </Box>
        <Button
          fullWidth
          variant="contained"
          onClick={onSend}
          disabled={chatLoading || !canSend}
          data-testid="ai-chat-send-button"
          data-contract-source="/live/ai/chat-for-script"
          data-endpoint-owner="useAiChat.handleChatSend"
          data-disabled-reason={disabledReason}
          data-no-direct-api-request="true"
        >
          {chatLoading ? <CircularProgress size={20} /> : 'AI 生成'}
        </Button>
        {chatResponse ? (
          <Box
            data-testid="ai-chat-response-surface"
            data-contract-source="/live/ai/chat-for-script"
            data-endpoint-owner="useAiChat.handleChatSend"
            data-no-local-chat-fallback="true"
            sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', mb: 1, mt: 1 }}
          >
            <Typography variant="caption" color="text.secondary" sx={{ mb: 0.5 }}>AI 生成：</Typography>
            <Paper variant="outlined" sx={{ p: 1.5, flex: 1, overflow: 'auto', maxHeight: 200 }}>
              <Typography sx={{ whiteSpace: 'pre-wrap', fontSize: '0.875rem' }}>{chatResponse}</Typography>
            </Paper>
            <Box sx={{ display: 'flex', gap: 0.5, mt: 1, flexWrap: 'wrap' }}>
              <Button
                size="small"
                variant="contained"
                startIcon={<CheckIcon />}
                onClick={onApply}
                data-testid="ai-chat-apply-button"
                data-contract-source="/live/script/save|local-edit-content"
                data-endpoint-owner="useAiChat.handleChatApply"
              >
                应用到编辑区（可微调后保存）
              </Button>
              <Button
                size="small"
                variant="outlined"
                startIcon={copySaveLoading ? <CircularProgress size={14} /> : <LibraryBooksIcon />}
                onClick={onSaveToCopy}
                disabled={copySaveLoading}
                data-testid="ai-chat-save-copy-button"
                data-contract-source="/live/ai/save-to-copy-if-passed"
                data-endpoint-owner="useAiChat.handleSaveToCopy"
                data-disabled-reason={copySaveLoading ? 'copy-save-loading' : 'ready'}
              >
                审核过关一键到文案库
              </Button>
            </Box>
          </Box>
        ) : (
          <Paper
            variant="outlined"
            data-testid="ai-chat-response-empty"
            data-contract-source="/live/ai/chat-for-script"
            data-endpoint-owner="useAiChat.handleChatSend"
            data-no-local-chat-fallback="true"
            data-response-state={responseState}
            sx={{ mt: 1, p: 1.5, borderStyle: 'dashed', bgcolor: 'action.hover' }}
          >
            <Typography variant="caption" color="text.secondary">
              {chatLoading ? '正在等待 AI 生成结果' : '暂无 AI 生成结果'}
            </Typography>
          </Paper>
        )}
      </CardContent>
    </Card>
  )
}
