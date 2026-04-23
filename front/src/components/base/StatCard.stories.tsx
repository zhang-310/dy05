import type { Meta, StoryObj } from '@storybook/react'
import { StatCard } from './StatCard'
import { TrendingUp } from '@mui/icons-material'

const meta: Meta<typeof StatCard> = {
  title: 'Base/StatCard',
  component: StatCard,
}
export default meta

type Story = StoryObj<typeof StatCard>

export const Default: Story = {
  args: {
    title: '今日调用',
    value: 1280,
  },
}

export const WithUnit: Story = {
  args: {
    title: '剩余额度',
    value: 500,
    unit: '次',
  },
}

export const WithTrend: Story = {
  args: {
    title: '环比增长',
    value: '12.5%',
    trend: { value: 15, label: '较昨日' },
  },
}

export const WithIcon: Story = {
  args: {
    title: '成功率',
    value: '99.2%',
    icon: <TrendingUp />,
    color: 'success',
  },
}
