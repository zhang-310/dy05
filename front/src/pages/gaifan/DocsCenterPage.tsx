import { Box, Typography, Paper, Link, Stack } from '@mui/material'

const LINKS = [
  { title: 'MCP 运行时', href: '/api/mcp/runtime' },
  { title: 'MCP 发现', href: '/mcp' },
  { title: '积分治理', href: '/api/credits/overview?tenantId=demo-tenant' },
  { title: 'OpenAPI 总览', href: '/api/openapi/overview?tenantId=demo-tenant' },
]

export default function DocsCenterPage() {
  return (
    <Box sx={{ p: 3, maxWidth: 720 }}>
      <Typography variant="h5" gutterBottom>文档中心</Typography>
      <Typography color="text.secondary" paragraph>
        商业化 API 与 MCP 协议入口（本地开发环境）。
      </Typography>
      <Paper sx={{ p: 2 }}>
        <Stack spacing={1}>
          {LINKS.map((item) => (
            <Link key={item.href} href={item.href} target="_blank" rel="noopener">
              {item.title}
            </Link>
          ))}
        </Stack>
      </Paper>
    </Box>
  )
}
