import { useMemo, useState } from 'react'
import {
  Alert,
  Box,
  Chip,
  Dialog,
  DialogContent,
  DialogTitle,
  Divider,
  InputAdornment,
  List,
  ListItemButton,
  ListItemText,
  TextField,
  Typography,
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import { roleScopeFromPath, type RoleScope } from '@/constants/roleRoutes'
import { ADMIN_NAV_GROUPS, ORG_NAV_ITEMS, TALENT_NAV_ITEMS, USER_NAV_ITEMS, type NavChild } from '@/layouts/roleNavigation'
import type { FavoriteNavItem, RecentVisitItem } from '@/stores/recentVisits'

export interface CommandPaletteEntry {
  label: string
  path: string
  group: string
  keywords?: string[]
}

interface CommandPaletteProps {
  open: boolean
  pathname: string
  recentItems: RecentVisitItem[]
  favoriteItems: FavoriteNavItem[]
  onClose: () => void
  onNavigate: (path: string) => void
}

const READY_SEARCH_SOURCES = ['registered-pages', 'favorite-pages', 'recent-visits'] as const
const UNSUPPORTED_ENTITY_SEARCH = [
  'live-entities',
  'product-entities',
  'script-entities',
  'shortvideo-entities',
  'douyin-entities',
] as const
const MAX_RESULTS = 12

function navChildToEntry(item: NavChild, group: string): CommandPaletteEntry {
  return {
    label: item.label,
    path: item.path,
    group,
    keywords: [item.label, item.path, item.section ?? group],
  }
}

const ADMIN_ENTRIES: CommandPaletteEntry[] = ADMIN_NAV_GROUPS.flatMap((group) => {
  const current = group.path ? [navChildToEntry({ label: group.label, path: group.path, icon: group.icon }, group.label)] : []
  const children = (group.children ?? []).map((item) => navChildToEntry(item, group.label))
  return [...current, ...children]
})

const ORG_ENTRIES: CommandPaletteEntry[] = ORG_NAV_ITEMS.map((item) => navChildToEntry(item, '机构'))

const TALENT_ENTRIES: CommandPaletteEntry[] = TALENT_NAV_ITEMS.map((item) => navChildToEntry(item, '达人'))

const USER_ENTRIES: CommandPaletteEntry[] = USER_NAV_ITEMS.map((item) => navChildToEntry(item, '个人'))

function routeEntries(scope: RoleScope): CommandPaletteEntry[] {
  if (scope === 'org') return ORG_ENTRIES
  if (scope === 'talent') return TALENT_ENTRIES
  if (scope === 'user') return USER_ENTRIES
  return ADMIN_ENTRIES
}

function entryMatchesScope(entry: CommandPaletteEntry, scope: RoleScope) {
  if (scope === 'org') return entry.path.startsWith('/org/')
  if (scope === 'talent') return entry.path.startsWith('/talent/')
  if (scope === 'user') return entry.path.startsWith('/user/')
  return entry.path.startsWith('/admin/')
}

function textMatches(entry: CommandPaletteEntry, query: string) {
  const normalized = query.trim().toLowerCase()
  if (!normalized) return true
  const haystack = [entry.label, entry.path, entry.group, ...(entry.keywords ?? [])].join(' ').toLowerCase()
  return haystack.includes(normalized)
}

function uniqByPath(entries: CommandPaletteEntry[]) {
  const seen = new Set<string>()
  return entries.filter((entry) => {
    if (seen.has(entry.path)) return false
    seen.add(entry.path)
    return true
  })
}

export function CommandPalette({ open, pathname, recentItems, favoriteItems, onClose, onNavigate }: CommandPaletteProps) {
  const [query, setQuery] = useState('')
  const scope = roleScopeFromPath(pathname)
  const shortcutEntries = useMemo(() => routeEntries(scope), [scope])
  const favoriteEntries = useMemo<CommandPaletteEntry[]>(
    () => favoriteItems
      .map((item) => ({ label: item.label, path: item.path, group: '收藏' }))
      .filter((entry) => entryMatchesScope(entry, scope)),
    [favoriteItems, scope],
  )
  const recentEntries = useMemo<CommandPaletteEntry[]>(
    () => recentItems
      .map((item) => ({ label: item.label, path: item.path, group: '最近访问' }))
      .filter((entry) => entryMatchesScope(entry, scope)),
    [recentItems, scope],
  )
  const entries = useMemo(
    () => uniqByPath([...favoriteEntries, ...recentEntries, ...shortcutEntries]).filter((entry) => textMatches(entry, query)).slice(0, MAX_RESULTS),
    [favoriteEntries, query, recentEntries, shortcutEntries],
  )

  const showEntityFallback = query.trim().length >= 2 && entries.length === 0

  const handleNavigate = (path: string) => {
    setQuery('')
    onNavigate(path)
  }

  const handleClose = () => {
    setQuery('')
    onClose()
  }

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm" aria-label="命令面板 · 全局搜索">
      <DialogTitle sx={{ pb: 1 }}>命令面板 · 全局搜索</DialogTitle>
      <DialogContent sx={{ pt: 0 }}>
        <Box
          data-testid="command-palette-root"
          data-contract-scope="global-route-command-palette"
          data-route-scope={scope}
          data-ready-search-sources={READY_SEARCH_SOURCES.join('|')}
          data-unsupported-entity-search={UNSUPPORTED_ENTITY_SEARCH.join('|')}
          data-result-count={entries.length}
          data-query-length={query.trim().length}
          data-priority-order="favorites|recent-visits|registered-pages"
          data-dedupe-by="path"
          data-max-results={MAX_RESULTS}
        >
          <TextField
            autoFocus
            fullWidth
            size="small"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="搜索页面、路径、收藏或最近访问…"
            inputProps={{
              'aria-label': '搜索页面、路径、收藏或最近访问',
              'data-testid': 'command-palette-search-input',
            }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              ),
            }}
            sx={{ mt: 0.5, mb: 1.5 }}
          />
          <Alert
            data-testid="command-palette-source-contract"
            data-no-entity-search-fallback="true"
            severity="info"
            variant="outlined"
            sx={{ mb: 1.5 }}
          >
            当前仅检索已注册页面、收藏和最近访问；直播/商品/话术/视频实体搜索尚无统一聚合接口，未接入前不会展示本地假结果。
          </Alert>
          <List
            data-testid="command-palette-results"
            data-result-count={entries.length}
            data-empty-state={entries.length === 0 ? (showEntityFallback ? 'entity-search-unsupported' : 'no-registered-route') : 'none'}
            dense
            disablePadding
            sx={{ maxHeight: 360, overflowY: 'auto', border: 1, borderColor: 'divider', borderRadius: 1 }}
          >
            {entries.length === 0 ? (
              <Box
                data-testid={showEntityFallback ? 'command-palette-entity-search-downgrade' : 'command-palette-empty'}
                data-no-local-entity-results="true"
                data-query={query.trim()}
                sx={{ p: 2, textAlign: 'center' }}
              >
                <Typography variant="body2" color="text.secondary">{showEntityFallback ? '无匹配页面，实体搜索暂未接入。' : '暂无匹配入口'}</Typography>
              </Box>
            ) : entries.map((entry) => (
              <ListItemButton
                key={entry.path}
                data-testid="command-palette-result-item"
                data-entry-path={entry.path}
                data-entry-group={entry.group}
                data-route-scope={scope}
                onClick={() => handleNavigate(entry.path)}
              >
                <ListItemText
                  primary={entry.label}
                  secondary={entry.path}
                  primaryTypographyProps={{ variant: 'body2', fontWeight: 600 }}
                  secondaryTypographyProps={{ variant: 'caption', sx: { fontFamily: 'monospace' } }}
                />
                <Chip size="small" label={entry.group} variant="outlined" />
              </ListItemButton>
            ))}
          </List>
          <Divider sx={{ my: 1.5 }} />
          <Typography
            data-testid="command-palette-scope-footnote"
            data-no-cross-shell-navigation="true"
            variant="caption"
            color="text.secondary"
          >
            Ctrl / ⌘ + K 打开或关闭 · 只导航到当前角色壳可访问的真实页面
          </Typography>
        </Box>
      </DialogContent>
    </Dialog>
  )
}
