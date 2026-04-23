import { useState } from 'react'
import { useParams, Navigate } from 'react-router-dom'
import { Box, Tabs, Tab, CircularProgress, Typography } from '@mui/material'
import { WorkspaceProviders } from './contexts'
import { SelectTabContent } from './components/SelectTabContent'
import { GenerateTabContent } from './components/GenerateTabContent'
import { ScriptTabContent } from './components/ScriptTabContent'
import { ReadinessTab } from './components/ReadinessTab'
import { DataAnalysisTab } from './components/DataAnalysisTab'
import { useCoreData } from './contexts'
import {
  STEP_TO_TAB,
  STEP_LABELS,
  STEP_COLORS,
} from './sessionWorkbenchNav'

interface SessionWorkspaceInnerProps {
  step: number
  onStepChange: (step: number) => void
}

function SessionWorkspaceInner({ step, onStepChange }: SessionWorkspaceInnerProps) {
  const { session, sessionLoading } = useCoreData()
  const sessionId = session?.id ?? 0
  const tab = STEP_TO_TAB[step] ?? 'products'

  if (sessionLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flex: 1 }}>
        <CircularProgress />
      </Box>
    )
  }

  if (!session) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flex: 1 }}>
        <Typography color="text.secondary">场次不存在</Typography>
      </Box>
    )
  }

  return (
    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', minHeight: 0, height: 'calc(100vh - 48px - 41px)' }}>
      {/* Tab 导航 */}
      <Tabs
        value={step}
        onChange={(_, v) => onStepChange(v)}
        sx={{
          borderBottom: '1px solid',
          borderColor: 'divider',
          flexShrink: 0,
          '& .MuiTab-root': { fontSize: 13, minHeight: 40, py: 0.5 },
        }}
      >
        {STEP_TO_TAB.map((t, i) => {
          const colors = STEP_COLORS[t]
          return (
            <Tab
              key={t}
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                  <Box
                    sx={{
                      width: 18, height: 18, borderRadius: '50%',
                      bgcolor: step === i ? colors.active : colors.bg,
                      color: step === i ? '#fff' : colors.active,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 11, fontWeight: 700, flexShrink: 0,
                    }}
                  >
                    {i + 1}
                  </Box>
                  {STEP_LABELS[t]}
                </Box>
              }
            />
          )
        })}
      </Tabs>

      {/* Tab 内容 */}
      <Box sx={{ flex: 1, overflow: 'hidden', minHeight: 0 }}>
        {tab === 'products' && <Box sx={{ height: '100%', overflow: 'hidden' }}><SelectTabContent sessionId={sessionId} onNext={() => onStepChange(1)} /></Box>}
        {tab === 'generate' && <Box sx={{ height: '100%', p: 1.5, overflow: 'hidden' }}><GenerateTabContent /></Box>}
        {tab === 'scripts'  && <Box sx={{ height: '100%', overflow: 'hidden' }}><ScriptTabContent /></Box>}
        {tab === 'readiness'&& <ReadinessTab />}
        {tab === 'data'     && <DataAnalysisTab />}
      </Box>
    </Box>
  )
}

export default function SessionWorkspacePageWrapper() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const [step, setStep] = useState(0)

  if (!sessionId) return <Navigate to="/admin/live/sessions" replace />

  return (
    <WorkspaceProviders sessionId={Number(sessionId)}>
      <SessionWorkspaceInner step={step} onStepChange={setStep} />
    </WorkspaceProviders>
  )
}

export interface SessionWorkspacePageProps {
  sessionId: number
  step: number
  onStepChange: (step: number) => void
}

export function SessionWorkspacePage({ sessionId, step, onStepChange }: SessionWorkspacePageProps) {
  return (
    <WorkspaceProviders sessionId={sessionId}>
      <SessionWorkspaceInner step={step} onStepChange={onStepChange} />
    </WorkspaceProviders>
  )
}
