import { Box, Typography, List, ListItem, ListItemText, Button, Stack, ListItemButton } from '@mui/material'
import { Link } from 'react-router-dom'
import { useGaifanEntitledProducts } from '@/hooks/useGaifanEntitledProducts'

export default function OfficialWebsitePage() {
  const { products, loading } = useGaifanEntitledProducts()

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>盖饭 Ops 官网</Typography>
      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <Button component={Link} to="/admin/gaifan/payment" variant="contained" size="small">
          购买套餐
        </Button>
        <Button component={Link} to="/admin/gaifan/credits" variant="outlined" size="small">
          积分治理
        </Button>
      </Stack>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
        {loading ? '加载产品目录…' : `可售产品 ${products.length} 个（JWT 登录后可进入控制台）`}
      </Typography>
      <List>
        {products.map((p) => (
          <ListItem key={p.code} disablePadding>
            {p.path ? (
              <ListItemButton component={Link} to={p.path}>
                <ListItemText primary={p.name} secondary={p.code} />
              </ListItemButton>
            ) : (
              <ListItemText primary={p.name} secondary={p.code} />
            )}
          </ListItem>
        ))}
      </List>
    </Box>
  )
}
