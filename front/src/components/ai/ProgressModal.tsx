import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  LinearProgress,
  Box,
  Typography,
  Stepper,
  Step,
  StepLabel,
  StepContent,
} from '@mui/material'
import { Sync as LoadingIcon, CheckCircle as CheckCircleIcon } from '@mui/icons-material'

export interface StageInfo {
  name: string
  status: 'wait' | 'process' | 'finish' | 'error'
  duration?: number
  detail?: string
}

interface ProgressModalProps {
  open: boolean
  title: string
  progress: number
  processed?: number
  total?: number
  stages?: StageInfo[]
  currentTask?: string
  estimatedTime?: number
  completedItems?: string[]
  pendingItems?: string[]
  onCancel?: () => void
  onMinimize?: () => void
}

export function ProgressModal({
  open,
  title,
  progress,
  processed,
  total,
  stages = [],
  currentTask,
  estimatedTime,
  completedItems = [],
  pendingItems = [],
  onCancel,
  onMinimize,
}: ProgressModalProps) {
  return (
    <Dialog open={open} onClose={() => {}} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span>{title}</span>
        {onMinimize && (
          <Button size="small" onClick={onMinimize}>
            最小化
          </Button>
        )}
      </DialogTitle>
      <DialogContent>
        <Box sx={{ mb: 2 }}>
          <LinearProgress
            variant="determinate"
            value={Math.min(progress, 100)}
            sx={{ height: 8, borderRadius: 1 }}
          />
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            总进度: {progress}% {processed != null && total != null && total > 0 ? `(${processed}/${total})` : ''}
          </Typography>
          {currentTask && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              当前任务: {currentTask}
            </Typography>
          )}
          {estimatedTime != null && estimatedTime > 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              预计剩余时间: 约 {estimatedTime} 秒
            </Typography>
          )}
        </Box>

        {stages.length > 0 && (
          <Stepper orientation="vertical" activeStep={stages.findIndex((s) => s.status === 'process')}>
            {stages.map((stage, idx) => (
              <Step key={idx} completed={stage.status === 'finish'}>
                <StepLabel
                  StepIconComponent={
                    stage.status === 'process'
                      ? () => <LoadingIcon color="primary" fontSize="small" />
                      : stage.status === 'finish'
                        ? () => <CheckCircleIcon color="success" fontSize="small" />
                        : undefined
                  }
                >
                  {stage.name}
                </StepLabel>
                <StepContent>
                  {stage.detail && (
                    <Typography variant="body2" color="text.secondary">
                      {stage.detail}
                    </Typography>
                  )}
                  {stage.duration != null && (
                    <Typography variant="caption" color="text.secondary">
                      {stage.duration}s
                    </Typography>
                  )}
                </StepContent>
              </Step>
            ))}
          </Stepper>
        )}

        {completedItems.length > 0 && (
          <Box sx={{ mt: 2 }}>
            <Typography variant="subtitle2" gutterBottom>
              已完成:
            </Typography>
            {completedItems.map((item, i) => (
              <Typography key={i} variant="body2" color="success.main">
                ✓ {item}
              </Typography>
            ))}
          </Box>
        )}
        {pendingItems.length > 0 && (
          <Box sx={{ mt: 1 }}>
            <Typography variant="subtitle2" gutterBottom>
              待处理:
            </Typography>
            {pendingItems.map((item, i) => (
              <Typography key={i} variant="body2" color="text.secondary">
                ⏳ {item}
              </Typography>
            ))}
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        {onMinimize && (
          <Button onClick={onMinimize}>后台运行</Button>
        )}
        {onCancel && (
          <Button color="error" onClick={onCancel}>
            取消导入
          </Button>
        )}
      </DialogActions>
    </Dialog>
  )
}
