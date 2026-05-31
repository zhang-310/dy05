import { Box, type SxProps, type Theme } from '@mui/material'
import { alpha } from '@mui/material/styles'
import ReactMarkdown, { defaultUrlTransform } from 'react-markdown'
import remarkGfm from 'remark-gfm'

interface MarkdownViewerProps {
  content?: string | null
  compact?: boolean
  sx?: SxProps<Theme>
}

function transformMarkdownUrl(url: string): string {
  return defaultUrlTransform(url) || ''
}

function normalizedHref(href: unknown): string | undefined {
  return typeof href === 'string' && href.length > 0 ? href : undefined
}

function linkTarget(href: unknown): '_blank' | undefined {
  return typeof href === 'string' && /^[a-z][a-z\d+.-]*:/i.test(href) ? '_blank' : undefined
}

export default function MarkdownViewer({ content, compact = false, sx }: MarkdownViewerProps) {
  const text = content?.trim() ? content : '暂无内容'

  return (
    <Box
      data-testid="markdown-viewer"
      data-renderer-contract="react-markdown+remark-gfm"
      data-safe-url-transform="react-markdown-defaultUrlTransform"
      data-table-overflow="horizontal-scroll"
      data-image-policy="lazy-safe-url"
      data-no-static-markdown-render="true"
      className="markdown-viewer"
      sx={[
        (theme) => ({
        color: 'inherit',
        fontSize: compact ? '0.875rem' : '0.9375rem',
        lineHeight: compact ? 1.7 : 1.8,
        wordBreak: 'break-word',
        overflowWrap: 'anywhere',
        '& > :first-of-type': { mt: 0 },
        '& > :last-child': { mb: 0 },
        '& h1': { fontSize: compact ? '1.15rem' : '1.45rem', lineHeight: 1.35, mt: compact ? 1 : 1.5, mb: 0.75, fontWeight: 700 },
        '& h2': { fontSize: compact ? '1.05rem' : '1.25rem', lineHeight: 1.4, mt: compact ? 1 : 1.4, mb: 0.65, fontWeight: 700 },
        '& h3': { fontSize: compact ? '0.98rem' : '1.1rem', lineHeight: 1.45, mt: 1.1, mb: 0.5, fontWeight: 700 },
        '& h4, & h5, & h6': { fontSize: '0.95rem', mt: 1, mb: 0.5, fontWeight: 700 },
        '& p': { my: compact ? 0.5 : 0.75 },
        '& ul, & ol': { my: compact ? 0.5 : 0.75, pl: 2.5 },
        '& li': { my: 0.25 },
        '& blockquote': {
          m: 0,
          my: 1,
          py: 0.75,
          pr: 1,
          pl: 1.5,
          borderLeft: '3px solid',
          borderColor: theme.palette.primary.main,
          bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.12 : 0.06),
          color: 'text.secondary',
          borderRadius: 0.5,
        },
        '& code': {
          px: 0.5,
          py: 0.15,
          borderRadius: 0.5,
          bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.18 : 0.08),
          color: 'text.primary',
          fontFamily: 'Consolas, Monaco, monospace',
          fontSize: '0.88em',
        },
        '& pre': {
          my: 1,
          p: 1.25,
          borderRadius: 1,
          bgcolor: theme.palette.mode === 'dark' ? theme.palette.background.default : theme.palette.grey[50],
          color: 'text.primary',
          border: '1px solid',
          borderColor: 'divider',
          overflow: 'auto',
          maxWidth: '100%',
        },
        '& pre code': { p: 0, bgcolor: 'transparent', color: 'inherit', fontSize: '0.875rem' },
        '& img': {
          display: 'block',
          maxWidth: '100%',
          height: 'auto',
          my: 1,
          borderRadius: 1,
          border: '1px solid',
          borderColor: 'divider',
          objectFit: 'contain',
        },
        '& [data-markdown-node="table-scroll"]': {
          width: '100%',
          my: 1,
          overflowX: 'auto',
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 1,
        },
        '& table': {
          width: '100%',
          borderCollapse: 'collapse',
          minWidth: 'max-content',
        },
        '& th, & td': { border: '1px solid', borderColor: 'divider', px: 1, py: 0.75, textAlign: 'left', verticalAlign: 'top' },
        '& [data-markdown-node="table-scroll"] th, & [data-markdown-node="table-scroll"] td': {
          borderTop: 0,
          borderLeft: 0,
        },
        '& [data-markdown-node="table-scroll"] th:last-child, & [data-markdown-node="table-scroll"] td:last-child': {
          borderRight: 0,
        },
        '& [data-markdown-node="table-scroll"] tr:last-child td': {
          borderBottom: 0,
        },
        '& th': {
          bgcolor: theme.palette.mode === 'dark' ? theme.palette.background.default : theme.palette.grey[50],
          fontWeight: 700,
        },
        '& tr:nth-of-type(even) td': {
          bgcolor: theme.palette.mode === 'dark' ? alpha(theme.palette.common.white, 0.02) : alpha(theme.palette.common.black, 0.015),
        },
        '& a': { color: 'primary.main', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } },
        '& hr': { border: 0, borderTop: '1px solid', borderColor: 'divider', my: 1.5 },
        '& input[type="checkbox"]': { mr: 0.5 },
        }),
        ...(Array.isArray(sx) ? sx : [sx]),
      ]}
    >
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        urlTransform={transformMarkdownUrl}
        components={{
          a: ({ node: _node, href, ...props }) => {
            const safeHref = normalizedHref(href)
            return (
              <a
                {...props}
                href={safeHref}
                data-markdown-node="link"
                data-safe-url={safeHref ? 'true' : 'false'}
                target={linkTarget(safeHref)}
                rel="noopener noreferrer"
              />
            )
          },
          img: ({ node: _node, alt, ...props }) => (
            <img
              {...props}
              alt={alt ?? ''}
              loading="lazy"
              decoding="async"
              referrerPolicy="no-referrer"
              data-markdown-node="image"
            />
          ),
          pre: ({ node: _node, ...props }) => (
            <pre {...props} data-markdown-node="code-block" />
          ),
          table: ({ node: _node, ...props }) => (
            <Box component="div" data-markdown-node="table-scroll">
              <table {...props} data-markdown-node="table" />
            </Box>
          ),
        }}
      >
        {text}
      </ReactMarkdown>
    </Box>
  )
}
