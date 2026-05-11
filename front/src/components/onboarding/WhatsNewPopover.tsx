import { useState } from 'react'
import { Box, IconButton, Menu, MenuItem, Typography, Divider, Tooltip } from '@mui/material'
import NewReleasesIcon from '@mui/icons-material/NewReleases'
import { useTranslation } from 'react-i18next'

const LAST_SEEN_KEY = 'dy-whats-new-last'

/** D7-02：轻量更新说明（本地已读） */
export function WhatsNewPopover() {
  const { t } = useTranslation()
  const [anchor, setAnchor] = useState<HTMLElement | null>(null)
  const [hasUnread, setHasUnread] = useState(() => {
    try {
      return localStorage.getItem(LAST_SEEN_KEY) !== '2026-03-21'
    } catch (storageError) {
      console.warn('Failed to read localStorage:', storageError)
      return true
    }
  })

  const open = Boolean(anchor)
  const handleOpen = (e: React.MouseEvent<HTMLElement>) => {
    setAnchor(e.currentTarget)
    setHasUnread(false)
    try {
      localStorage.setItem(LAST_SEEN_KEY, '2026-03-21')
    } catch (storageError) {
      console.warn('Failed to write localStorage:', storageError)
    }
  }

  return (
    <>
      <Tooltip title={t('whatsNew.tooltip')}>
        <IconButton size="small" onClick={handleOpen} sx={{ mr: 0.5 }} aria-label={t('whatsNew.tooltip')}>
          <NewReleasesIcon fontSize="small" color={hasUnread ? 'primary' : 'inherit'} />
        </IconButton>
      </Tooltip>
      <Menu anchorEl={anchor} open={open} onClose={() => setAnchor(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Box sx={{ px: 2, py: 1, maxWidth: 320 }}>
          <Typography variant="subtitle2" fontWeight={600}>
            {t('whatsNew.title')}
          </Typography>
          <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
            2026-03-21
          </Typography>
        </Box>
        <Divider />
        <MenuItem disabled sx={{ whiteSpace: 'normal', alignItems: 'flex-start', py: 1.5 }}>
          <Typography variant="body2" component="span" sx={{ whiteSpace: 'pre-line' }}>
            {t('whatsNew.body')}
          </Typography>
        </MenuItem>
      </Menu>
    </>
  )
}
