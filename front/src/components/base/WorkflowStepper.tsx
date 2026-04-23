import { Stepper, Step, StepButton, Box } from '@mui/material'

export interface WorkflowStepperProps {
  steps: string[]
  /** 当前高亮步骤（0-based） */
  activeStep: number
  completed: boolean[]
  onStepClick?: (index: number) => void
}

/**
 * 直播 / 短视频共用：非线性 Stepper，可点击跳转
 */
export function WorkflowStepper({ steps, activeStep, completed, onStepClick }: WorkflowStepperProps) {
  return (
    <Box sx={{ width: '100%', overflowX: 'auto' }}>
      <Stepper activeStep={activeStep} nonLinear alternativeLabel sx={{ minWidth: 480 }}>
        {steps.map((label, i) => (
          <Step key={label} completed={!!completed[i]}>
            <StepButton
              onClick={() => onStepClick?.(i)}
              sx={{ '& .MuiStepLabel-label': { fontSize: { xs: 11, sm: 12 } } }}
            >
              {label}
            </StepButton>
          </Step>
        ))}
      </Stepper>
    </Box>
  )
}
