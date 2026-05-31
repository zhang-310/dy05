/**
 * 首次使用引导 — 可跳过的 3 步浮层
 */
import { useState, useEffect } from 'react'
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, Typography, Box, Stepper, Step, StepLabel } from '@mui/material'
import Inventory2Icon from '@mui/icons-material/Inventory2'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import EditIcon from '@mui/icons-material/Edit'

const STORAGE_KEY = 'live-script-first-guide-done'

function getGuideDone(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === '1'
  } catch {
    return false
  }
}

function setGuideDone() {
  try {
    localStorage.setItem(STORAGE_KEY, '1')
  } catch { /* ignore */ }
}

const STEPS = [
  { label: '添加选品', desc: '在左侧产品栏添加本场要讲解的商品，支持单个添加或批量导入', icon: <Inventory2Icon sx={{ fontSize: 40, color: 'primary.main' }} /> },
  { label: '一键生成', desc: '点击「一键生成」由 AI 自动生成开场、产品话术、转场、结尾', icon: <AutoAwesomeIcon sx={{ fontSize: 40, color: 'primary.main' }} /> },
  { label: '逐项优化', desc: '点击任一块可编辑，或使用 AI 润色、AI 分析师进一步优化', icon: <EditIcon sx={{ fontSize: 40, color: 'primary.main' }} /> },
]

const GUIDE_READY_SOURCES = ['showInStandalone-prop', 'localStorage:live-script-first-guide-done']

export interface FirstTimeGuideProps {
  /** 是否在独立页（LiveScriptBuilderPage）显示，ScriptTab 可选隐藏 */
  showInStandalone?: boolean
}

export function FirstTimeGuide({ showInStandalone = true }: FirstTimeGuideProps) {
  const [open, setOpen] = useState(false)
  const [step, setStep] = useState(0)

  useEffect(() => {
    if (!showInStandalone) return
    if (getGuideDone()) return
    setOpen(true)
  }, [showInStandalone])

  const handleSkip = () => {
    setGuideDone()
    setOpen(false)
  }

  const handleNext = () => {
    if (step >= STEPS.length - 1) {
      setGuideDone()
      setOpen(false)
    } else {
      setStep((s) => s + 1)
    }
  }

  const handleFinish = () => {
    setGuideDone()
    setOpen(false)
  }

  if (!open) {
    return (
      <Box
        data-testid="first-time-guide-hidden-state"
        data-contract-scope="live-first-time-guide-state"
        data-ready-sources={GUIDE_READY_SOURCES.join('|')}
        data-show-in-standalone={showInStandalone ? 'true' : 'false'}
        data-open="false"
        data-no-direct-api-request="true"
        sx={{ display: 'none' }}
      />
    )
  }

  const current = STEPS[step]
  const isLast = step === STEPS.length - 1

  return (
    <Dialog
      open
      onClose={handleSkip}
      maxWidth="sm"
      fullWidth
      data-testid="first-time-guide-dialog"
      data-contract-scope="live-first-time-guide-dialog"
      data-ready-sources={GUIDE_READY_SOURCES.join('|')}
      data-show-in-standalone={showInStandalone ? 'true' : 'false'}
      data-step={step}
      data-step-count={STEPS.length}
      data-open="true"
      data-no-direct-api-request="true"
      PaperProps={{ sx: { borderRadius: 2 } }}
    >
      <DialogTitle data-testid="first-time-guide-title" data-contract-source="local-guide-copy">欢迎使用话术工作台</DialogTitle>
      <DialogContent data-testid="first-time-guide-content" data-contract-source="local-guide-copy|localStorage">
        <Stepper activeStep={step} sx={{ mb: 2 }} data-testid="first-time-guide-stepper" data-contract-source="local-guide-steps">
          {STEPS.map((s, i) => (
            <Step key={s.label} completed={i < step} data-testid="first-time-guide-step" data-step-index={i}>
              <StepLabel>{s.label}</StepLabel>
            </Step>
          ))}
        </Stepper>
        <Box
          sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2, py: 1 }}
          data-testid="first-time-guide-current-step"
          data-step={step}
          data-step-label={current.label}
        >
          {current.icon}
          <Typography variant="body1" color="text.primary">
            {current.desc}
          </Typography>
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={handleSkip} color="inherit" data-testid="first-time-guide-skip-button" data-contract-source="localStorage-dismiss">跳过</Button>
        <Button
          variant="contained"
          onClick={isLast ? handleFinish : handleNext}
          data-testid="first-time-guide-next-button"
          data-contract-source="localStorage-dismiss|local-step-state"
          data-action={isLast ? 'finish' : 'next'}
        >
          {isLast ? '开始使用' : '下一步'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
