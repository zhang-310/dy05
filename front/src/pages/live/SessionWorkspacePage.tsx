import { useState } from 'react'
import { useParams, Navigate, useSearchParams, useLocation } from 'react-router-dom'
import { Alert, Box, Tabs, Tab, CircularProgress, Typography } from '@mui/material'
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
  STEP_COLOR_TONES,
  getStepThemeColors,
  parseStepFromSearch,
} from './sessionWorkbenchNav'
import { inferLiveRouteScope, liveSessionListPath } from './liveRouteScope'

const SESSION_WORKSPACE_READY_ENDPOINTS = [
  '/live/session/get',
  '/live/product/by-session',
  '/live/script/by-session',
  '/live/session/readiness',
] as const

const SESSION_WORKSPACE_CONTEXT_ENDPOINTS = [
  ...SESSION_WORKSPACE_READY_ENDPOINTS,
  '/product/list',
]

const SESSION_WORKSPACE_UNSUPPORTED_ACTIONS = [
  'local-session-fallback',
  'local-product-array-fallback',
  'local-script-array-fallback',
  'shell-clone-session',
  'shell-export-shortvideo',
  'shell-realtime-panel',
  'direct-session-status-mutation',
  'direct-product-mutation',
  'direct-script-mutation',
]

interface SessionWorkspaceInnerProps {
  step: number
  onStepChange: (step: number) => void
}

function SessionWorkspaceInner({ step, onStepChange }: SessionWorkspaceInnerProps) {
  const { session, sessionLoading, dependencyIssues, products, scripts, readiness } = useCoreData()
  const routeScope = inferLiveRouteScope(useLocation().pathname)
  const sessionId = session?.id ?? 0
  const tab = STEP_TO_TAB[step] ?? 'products'
  const readinessScore = typeof readiness?.score === 'number' ? readiness.score : ''

  if (sessionLoading) {
    return (
      <Box
        sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flex: 1 }}
        data-testid="session-workspace-loading"
        data-contract-source={SESSION_WORKSPACE_READY_ENDPOINTS.join('|')}
        data-no-local-session-fallback="true"
      >
        <CircularProgress />
      </Box>
    )
  }

  if (!session) {
    return (
      <Box
        sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flex: 1 }}
        data-testid="session-workspace-not-found"
        data-contract-source="/live/session/get"
        data-no-local-session-fallback="true"
      >
        <Typography color="text.secondary">场次不存在</Typography>
      </Box>
    )
  }

  return (
    <Box
      sx={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', minHeight: 0, height: 'calc(100vh - 48px - 41px)' }}
      data-testid="session-workspace-root"
      data-contract-scope="live-session-workspace-core"
      data-ready-endpoints={SESSION_WORKSPACE_READY_ENDPOINTS.join('|')}
      data-context-endpoints={SESSION_WORKSPACE_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={SESSION_WORKSPACE_UNSUPPORTED_ACTIONS.join('|')}
      data-route-scope={routeScope}
      data-session-id={sessionId}
      data-step={step}
      data-tab={tab}
      data-product-count={products.length}
      data-script-count={scripts.length}
      data-readiness-score={readinessScore}
      data-dependency-issue-count={dependencyIssues.length}
      data-no-local-session-fallback="true"
      data-no-local-product-array-fallback="true"
      data-no-local-script-array-fallback="true"
    >
      {/* Tab 导航 */}
      <Tabs
        value={step}
        onChange={(_, v) => onStepChange(v)}
        data-testid="session-workspace-tabs"
        data-contract-source={SESSION_WORKSPACE_READY_ENDPOINTS.join('|')}
        data-step={step}
        data-tab={tab}
        sx={{
          borderBottom: '1px solid',
          borderColor: 'divider',
          flexShrink: 0,
          '& .MuiTab-root': { fontSize: 13, minHeight: 40, py: 0.5 },
        }}
      >
        {STEP_TO_TAB.map((t, i) => {
          const active = step === i
          return (
            <Tab
              key={t}
              data-testid={active ? 'session-workspace-tab-active' : 'session-workspace-tab'}
              data-step-tab={t}
              data-step-index={i}
              data-step-active={active ? 'true' : 'false'}
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                  <Box
                    data-testid={active ? 'session-workspace-tab-badge-active-surface' : undefined}
                    data-step-tab={t}
                    data-step-tone={STEP_COLOR_TONES[t]}
                    data-step-active={active ? 'true' : 'false'}
                    sx={(theme) => {
                      const colors = getStepThemeColors(theme, t, active)
                      return {
                      '--session-workspace-tab-badge-tone': colors.tone,
                      width: 18, height: 18, borderRadius: '50%',
                      bgcolor: colors.chipBg,
                      color: colors.chipColor,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 11, fontWeight: 700, flexShrink: 0,
                    }}}
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
      {dependencyIssues.length > 0 && (
        <Alert
          severity="warning"
          sx={{ m: 1.5, mb: 0, flexShrink: 0 }}
          data-testid="session-workspace-dependency-downgrade-alert"
          data-contract-sources="/live/product/by-session|/live/script/by-session"
          data-issue-count={dependencyIssues.length}
          data-no-local-product-array-fallback="true"
          data-no-local-script-array-fallback="true"
        >
          直播工作台依赖降级：{dependencyIssues.join('；')}
        </Alert>
      )}
      <Box
        sx={{ flex: 1, overflow: 'hidden', minHeight: 0 }}
        data-testid="session-workspace-tab-content"
        data-tab={tab}
        data-step={step}
        data-session-id={sessionId}
        data-product-count={products.length}
        data-script-count={scripts.length}
      >
        {tab === 'products' && (
          <Box sx={{ height: '100%', overflow: 'hidden' }} data-testid="session-workspace-products-pane" data-contract-owner="SelectTabContent">
            <SelectTabContent sessionId={sessionId} onNext={() => onStepChange(1)} />
          </Box>
        )}
        {tab === 'generate' && (
          <Box sx={{ height: '100%', p: 1.5, overflow: 'hidden' }} data-testid="session-workspace-generate-pane" data-contract-owner="GenerateTabContent">
            <GenerateTabContent />
          </Box>
        )}
        {tab === 'scripts'  && (
          <Box sx={{ height: '100%', overflow: 'hidden' }} data-testid="session-workspace-scripts-pane" data-contract-owner="ScriptTabContent">
            <ScriptTabContent />
          </Box>
        )}
        {tab === 'readiness'&& <Box data-testid="session-workspace-readiness-pane" data-contract-owner="ReadinessTab" sx={{ height: '100%' }}><ReadinessTab /></Box>}
        {tab === 'data'     && <Box data-testid="session-workspace-data-pane" data-contract-owner="DataAnalysisTab" sx={{ height: '100%' }}><DataAnalysisTab /></Box>}
      </Box>
    </Box>
  )
}

export default function SessionWorkspacePageWrapper() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const [searchParams] = useSearchParams()
  const [step, setStep] = useState(() => parseStepFromSearch(searchParams))
  const listPath = liveSessionListPath(inferLiveRouteScope(useLocation().pathname))

  if (!sessionId || !Number.isFinite(Number(sessionId))) return <Navigate to={listPath} replace />

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
