import { useMemo, useState } from 'react'
import { Box, Button, Card, CardContent, Checkbox, FormControlLabel, LinearProgress, Typography } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useRolePrefix } from '@/hooks/useRolePrefix'

const STORAGE_KEY = 'dy-getting-started-v1'

type RolePrefix = '/admin' | '/org' | '/talent'
type Item = { id: string; label: string; path: string; note?: string; actionLabel?: string }

function rolePath(prefix: RolePrefix, adminPath: string, orgPath = '/org/dashboard', talentPath = '/talent/dashboard') {
  if (prefix === '/admin') return adminPath
  if (prefix === '/org') return orgPath
  return talentPath
}

function livePath(prefix: RolePrefix) {
  if (prefix === '/talent') return '/talent/live/sessions'
  return '/org/live/sessions'
}

function checklistItems(prefix: RolePrefix, t: (key: string) => string): Item[] {
  return [
    { id: 'live', label: t('onboarding.live'), path: livePath(prefix) },
    {
      id: 'product',
      label: t('onboarding.readiness'),
      path: rolePath(prefix, '/org/product/readiness'),
      note: prefix === '/admin' ? undefined : t('onboarding.adminOnlyReadinessNote'),
      actionLabel: prefix === '/admin' ? undefined : t('onboarding.backToWorkbench'),
    },
    {
      id: 'sv',
      label: t('onboarding.sv'),
      path: rolePath(prefix, '/talent/shortvideo/dashboard', '/org/dashboard', '/talent/shortvideo'),
      note: prefix === '/org' ? t('onboarding.orgShortvideoFallbackNote') : undefined,
      actionLabel: prefix === '/org' ? t('onboarding.backToWorkbench') : undefined,
    },
    {
      id: 'kpi',
      label: t('onboarding.kpi'),
      path: rolePath(prefix, '/admin/kpi', '/org/analytics'),
      note: prefix === '/talent' ? t('onboarding.talentKpiFallbackNote') : undefined,
      actionLabel: prefix === '/talent' ? t('onboarding.backToWorkbench') : undefined,
    },
  ]
}

function loadDone(): Record<string, boolean> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return {}
    return JSON.parse(raw) as Record<string, boolean>
  } catch {
    return {}
  }
}

function saveDone(m: Record<string, boolean>) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(m))
}

/**
 * D7-01：工作台新手引导（可勾选、可关闭）
 */
export function GettingStartedChecklist() {
  const { t } = useTranslation()
  const prefix = useRolePrefix() as RolePrefix
  const navigate = useNavigate()
  const [open, setOpen] = useState(() => {
    try {
      return localStorage.getItem(`${STORAGE_KEY}-collapsed`) !== '1'
    } catch {
      return true
    }
  })
  const [done, setDone] = useState<Record<string, boolean>>(loadDone)

  const items: Item[] = useMemo(() => checklistItems(prefix, t), [prefix, t])

  const total = items.length
  const n = items.filter((i) => done[i.id]).length
  const pct = total ? Math.round((n / total) * 100) : 0

  const toggle = (id: string) => {
    setDone((prev) => {
      const next = { ...prev, [id]: !prev[id] }
      saveDone(next)
      return next
    })
  }

  const dismiss = () => {
    setOpen(false)
    localStorage.setItem(`${STORAGE_KEY}-collapsed`, '1')
  }

  if (!open) {
    return (
      <Button size="small" variant="text" onClick={() => setOpen(true)}>
        {t('onboarding.show')}
      </Button>
    )
  }

  return (
    <Card variant="outlined" sx={{ mb: 2 }}>
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, flexWrap: 'wrap' }}>
          <Typography variant="subtitle2" fontWeight={600}>
            {t('onboarding.checklistTitle')}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {n}/{total} · {pct}%
          </Typography>
          <Button size="small" onClick={dismiss}>
            {t('onboarding.collapse')}
          </Button>
        </Box>
        <LinearProgress variant="determinate" value={pct} sx={{ my: 1, borderRadius: 1 }} />
        <Box>
          {items.map((i) => (
            <Box key={i.id} sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1, py: 0.25 }}>
              <FormControlLabel
                sx={{ mr: 0, alignItems: 'flex-start' }}
                control={<Checkbox size="small" checked={!!done[i.id]} onChange={() => toggle(i.id)} />}
                label={(
                  <Box>
                    <Typography variant="body2">{i.label}</Typography>
                    {i.note && (
                      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', lineHeight: 1.4 }}>
                        {i.note}
                      </Typography>
                    )}
                  </Box>
                )}
              />
              <Button
                size="small"
                aria-label={`${i.actionLabel ?? t('onboarding.go')}：${i.label}`}
                onClick={() => navigate(i.path)}
              >
                {i.actionLabel ?? t('onboarding.go')}
              </Button>
            </Box>
          ))}
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 1 }}>
            {prefix === '/admin' ? (
              <Button size="small" variant="outlined" onClick={() => navigate('/admin/onboarding')}>
                {t('onboarding.guide')}
              </Button>
            ) : (
              <Typography variant="caption" color="text.secondary">
                {t('onboarding.adminOnlyGuideNote')}
              </Typography>
            )}
          </Box>
        </Box>
      </CardContent>
    </Card>
  )
}
