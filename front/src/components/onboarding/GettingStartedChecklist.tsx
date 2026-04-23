import { useMemo, useState } from 'react'
import { Box, Button, Card, CardContent, Checkbox, FormControlLabel, LinearProgress, Typography } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useRolePrefix } from '@/hooks/useRolePrefix'

const STORAGE_KEY = 'dy-getting-started-v1'

type Item = { id: string; label: string; path: string }

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
  const prefix = useRolePrefix()
  const navigate = useNavigate()
  const [open, setOpen] = useState(() => {
    try {
      return localStorage.getItem(`${STORAGE_KEY}-collapsed`) !== '1'
    } catch {
      return true
    }
  })
  const [done, setDone] = useState<Record<string, boolean>>(loadDone)

  const items: Item[] = useMemo(
    () => [
      { id: 'live', label: t('onboarding.live'), path: `${prefix}/live/sessions` },
      { id: 'product', label: t('onboarding.readiness'), path: `${prefix}/product/readiness` },
      { id: 'sv', label: t('onboarding.sv'), path: `${prefix}/shortvideo/dashboard` },
      { id: 'kpi', label: t('onboarding.kpi'), path: `${prefix}/analytics/kpi` },
    ],
    [prefix, t],
  )

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
                control={<Checkbox size="small" checked={!!done[i.id]} onChange={() => toggle(i.id)} />}
                label={<Typography variant="body2">{i.label}</Typography>}
              />
              <Button size="small" onClick={() => navigate(i.path)}>
                {t('onboarding.go')}
              </Button>
            </Box>
          ))}
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 1 }}>
            <Button size="small" variant="outlined" onClick={() => navigate(`${prefix}/onboarding`)}>
              步骤向导
            </Button>
          </Box>
        </Box>
      </CardContent>
    </Card>
  )
}
