import { createBrowserRouter, Navigate, Outlet, useLocation } from 'react-router-dom'
import {
  allowedRoleScope,
  migrateLegacyAdminBusinessPath,
  roleDefaultPath,
  roleScopeFromPath,
} from '@/constants/roleRoutes'
import { useUserStore } from '@/stores'
import { isAuthenticated } from '@/utils/auth'
import { buildLoginHref } from '@/utils/login-redirect'
import { adminRoutes } from './roles/adminRoutes'
import { orgRoutes } from './roles/orgRoutes'
import { talentRoutes, userRoutes } from './roles/talentRoutes'
import {
  CreditGovernancePage,
  DocsCenterPage,
  LazyRoute,
  LoginPage,
  NotFoundPage,
  OfficialWebsitePage,
  PaymentCenterPage,
  TalentShortvideoEntry,
} from './routeComponents'

export function RequireAuth() {
  const location = useLocation()
  const roleCode = useUserStore((state) => state.userInfo?.roles?.[0])
  if (!isAuthenticated()) {
    const returnPath = location.pathname + location.search + location.hash
    return <Navigate to={buildLoginHref(returnPath)} replace />
  }
  const migratedPath = migrateLegacyAdminBusinessPath(location.pathname)
  if (migratedPath) {
    return <Navigate to={`${migratedPath}${location.search}${location.hash}`} replace />
  }
  const scope = roleScopeFromPath(location.pathname)
  if (!allowedRoleScope(roleCode, scope)) {
    return <Navigate to={roleDefaultPath(roleCode)} replace />
  }
  return <Outlet />
}

function RoleHomeRedirect() {
  const roleCode = useUserStore((state) => state.userInfo?.roles?.[0])
  return <Navigate to={roleDefaultPath(roleCode)} replace />
}

export { TalentShortvideoEntry }

export const router = createBrowserRouter([
  { path: '/login', element: <LazyRoute element={<LoginPage />} /> },
  { path: '/official', element: <LazyRoute element={<OfficialWebsitePage />} /> },
  { path: '/docs', element: <LazyRoute element={<DocsCenterPage />} /> },
  { path: '/payment', element: <LazyRoute element={<PaymentCenterPage />} /> },
  { path: '/credits', element: <LazyRoute element={<CreditGovernancePage />} /> },
  {
    element: <RequireAuth />,
    children: [
      adminRoutes,
      orgRoutes,
      talentRoutes,
      userRoutes,
      { path: '/', element: <RoleHomeRedirect /> },
    ],
  },
  { path: '*', element: <LazyRoute element={<NotFoundPage />} /> },
])
