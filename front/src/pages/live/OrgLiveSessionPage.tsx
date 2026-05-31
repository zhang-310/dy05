import { Box } from '@mui/material'
import SessionsPage from './SessionsPage'

const ORG_LIVE_SESSION_READY_ENDPOINTS = [
  '/live/session/search',
  '/live/session/save',
  '/live/session/delete',
  '/live/session/status',
  '/live/session/clone',
]

const ORG_LIVE_SESSION_UNSUPPORTED_ACTIONS = [
  'admin-route-leak',
  'admin-batch-actions',
  'admin-delete-action',
  'realtime-panel-direct',
  'shortvideo-project-export-navigation',
  'local-session-fallback',
]

export default function OrgLiveSessionPage() {
  return (
    <Box
      data-testid="org-live-session-entry-workbench"
      data-contract-scope="org-live-session-route-wrapper"
      data-route-scope="org"
      data-child-page="SessionsPage"
      data-ready-endpoints={ORG_LIVE_SESSION_READY_ENDPOINTS.join('|')}
      data-unsupported-actions={ORG_LIVE_SESSION_UNSUPPORTED_ACTIONS.join('|')}
      data-workbench-path-template="/org/live/sessions/:id"
      data-list-path="/org/live/sessions"
      data-admin-path-template="unsupported"
      data-api-owner="SessionsPage"
      data-no-admin-route-leak="true"
      data-no-admin-batch-actions="true"
      data-no-admin-delete="true"
      data-no-local-session-fallback="true"
      sx={{ minHeight: '100%' }}
    >
      <SessionsPage />
    </Box>
  )
}
