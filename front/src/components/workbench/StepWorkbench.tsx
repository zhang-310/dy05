import { Box, Step, StepLabel, Stepper, Typography } from '@mui/material'
import type { ReactNode } from 'react'

export interface StepWorkbenchProps {
  /** 当前步骤 0-based */
  activeStep: number
  steps: string[]
  children: ReactNode
  title?: string
}

/**
 * UX-02A：步骤条 + 主工作区（直播 / 短视频流程可共用布局）
 */
export function StepWorkbench({ activeStep, steps, children, title }: StepWorkbenchProps) {
  return (
    <Box>
      {title && (
        <Typography variant="h6" fontWeight={600} sx={{ mb: 2 }}>
          {title}
        </Typography>
      )}
      <Stepper activeStep={activeStep} alternativeLabel sx={{ mb: 3 }}>
        {steps.map((label) => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>
      {children}
    </Box>
  )
}
