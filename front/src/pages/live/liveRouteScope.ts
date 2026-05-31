export type LiveRouteScope = 'admin' | 'org' | 'talent'

export function inferLiveRouteScope(pathname: string): LiveRouteScope {
  if (pathname.startsWith('/org')) return 'org'
  if (pathname.startsWith('/talent')) return 'talent'
  return 'admin'
}

export function liveScopeLabel(scope: LiveRouteScope) {
  if (scope === 'org') return '机构端'
  if (scope === 'talent') return '达人端'
  return '管理员端'
}

export function liveSessionListPath(scope: LiveRouteScope) {
  if (scope === 'org') return '/org/live/sessions'
  if (scope === 'talent') return '/talent/live/sessions'
  return '/org/live/sessions'
}

export function liveSessionPath(scope: LiveRouteScope, id: number | string) {
  if (scope === 'org') return `/org/live/sessions/${id}`
  if (scope === 'talent') return `/talent/live/sessions/${id}`
  return `/org/live/sessions/${id}`
}

export function liveRealtimePath(scope: LiveRouteScope, id: number | string) {
  if (scope !== 'org') return ''
  return `/org/live/sessions/${id}/realtime`
}

export function shortVideoProjectPath(scope: LiveRouteScope, projectId?: number | string) {
  if (scope === 'org') return ''
  if (scope === 'talent') return projectId ? `/talent/shortvideo?projectId=${projectId}` : '/talent/shortvideo'
  return projectId ? `/talent/shortvideo/workbench?projectId=${projectId}` : '/talent/shortvideo/projects'
}
