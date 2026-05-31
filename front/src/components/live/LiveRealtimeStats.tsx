/**
 * 直播实时数据面板组件
 * 显示观众、点赞、评论等实时统计数据
 */

import React from 'react'
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  Stack
} from '@mui/material'
import { alpha, useTheme } from '@mui/material'
import type { Theme } from '@mui/material/styles'
import {
  PeopleOutlined,
  ThumbUpOutlined,
  ChatBubbleOutlineOutlined,
  ShareOutlined,
  EmojiEvents,
  ShoppingCart
} from '@mui/icons-material'
import type { LiveSessionRealtimeDataVO } from '@/types/live-realtime'

interface LiveRealtimeStatsProps {
  /** 实时数据 */
  data: LiveSessionRealtimeDataVO
}

/**
 * 数据卡片组件
 */
interface DataCardProps {
  icon: React.ReactNode
  label: string
  value: number | string
  unit?: string
  tone?: MetricTone
}

type MetricTone = 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info'

function semanticColor(theme: Theme, tone: MetricTone) {
  return theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main
}

function DataCard({ icon, label, value, unit = '', tone = 'primary' }: DataCardProps) {
  const theme = useTheme()
  const color = semanticColor(theme, tone)
  const surface = alpha(color, theme.palette.mode === 'dark' ? 0.16 : 0.08)

  return (
    <Card data-testid="live-realtime-stat-card-surface" data-stat-tone={tone} data-stat-color={color} sx={{ height: '100%', bgcolor: surface, border: `1px solid ${alpha(color, 0.28)}` }}>
      <CardContent>
        <Stack spacing={1} sx={{ textAlign: 'center' }}>
          <Box sx={{ color, fontSize: 32 }}>
            {icon}
          </Box>
          <Typography variant="caption" color="textSecondary">
            {label}
          </Typography>
          <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
            {value}
            {unit && <span style={{ fontSize: '14px', marginLeft: '4px' }}>{unit}</span>}
          </Typography>
        </Stack>
      </CardContent>
    </Card>
  )
}

/**
 * 直播实时数据面板组件
 * 网格布局显示各项实时数据
 */
export default function LiveRealtimeStats({ data }: LiveRealtimeStatsProps) {
  const formatNumber = (num: number): string => {
    if (num >= 10000) {
      return (num / 10000).toFixed(1) + '万'
    }
    return num.toString()
  }

  const formatMoney = (num: number | string): string => {
    const val = typeof num === 'string' ? parseFloat(num) : num
    return '¥' + val.toFixed(2)
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
        实时数据
      </Typography>

      {/* 观众相关 */}
      <Grid container spacing={2}>
        <Grid item xs={6} sm={4}>
          <DataCard
            icon={<PeopleOutlined fontSize="large" />}
            label="累计观看"
            value={formatNumber(data.watchedCount || 0)}
            unit="人"
            tone="secondary"
          />
        </Grid>
        <Grid item xs={6} sm={4}>
          <DataCard
            icon={<PeopleOutlined fontSize="large" />}
            label="在线观众"
            value={formatNumber(data.viewerCount || 0)}
            unit="人"
            tone="info"
          />
        </Grid>
        <Grid item xs={6} sm={4}>
          <DataCard
            icon={<EmojiEvents fontSize="large" />}
            label="新增关注"
            value={formatNumber(data.followCount || 0)}
            unit="人"
            tone="warning"
          />
        </Grid>
      </Grid>

      {/* 互动相关 */}
      <Grid container spacing={2}>
        <Grid item xs={6} sm={4}>
          <DataCard
            icon={<ThumbUpOutlined fontSize="large" />}
            label="点赞"
            value={formatNumber(data.likeCount || 0)}
            tone="secondary"
          />
        </Grid>
        <Grid item xs={6} sm={4}>
          <DataCard
            icon={<ChatBubbleOutlineOutlined fontSize="large" />}
            label="评论"
            value={formatNumber(data.commentCount || 0)}
            tone="info"
          />
        </Grid>
        <Grid item xs={6} sm={4}>
          <DataCard
            icon={<ShareOutlined fontSize="large" />}
            label="分享"
            value={formatNumber(data.shareCount || 0)}
            tone="success"
          />
        </Grid>
      </Grid>

      {/* 商品相关 */}
      <Grid container spacing={2}>
        <Grid item xs={6} sm={4}>
          <DataCard
            icon={<ShoppingCart fontSize="large" />}
            label="商品点击"
            value={formatNumber(data.productClickCount || 0)}
            tone="success"
          />
        </Grid>
        <Grid item xs={6} sm={4}>
          <DataCard
            icon={<ShoppingCart fontSize="large" />}
            label="商品购买"
            value={formatNumber(data.productPurchaseCount || 0)}
            tone="warning"
          />
        </Grid>
        <Grid item xs={6} sm={4}>
          <DataCard
            icon={<ShoppingCart fontSize="large" />}
            label="商品金额"
            value={formatMoney(data.productPurchaseAmount || 0)}
            tone="error"
          />
        </Grid>
      </Grid>

      {/* 礼物 */}
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <DataCard
            icon={<EmojiEvents fontSize="large" />}
            label="礼物收入"
            value={formatMoney(data.giftAmount || 0)}
            tone="error"
          />
        </Grid>
      </Grid>

      {/* 数据更新时间 */}
      <Typography variant="caption" color="textSecondary" sx={{ textAlign: 'center' }}>
        数据实时更新
      </Typography>
    </Box>
  )
}
