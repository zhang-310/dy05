import type { Meta, StoryObj } from '@storybook/react'
import { VirtualList } from './VirtualList'
import { Paper, Typography } from '@mui/material'

const meta: Meta<typeof VirtualList> = {
  title: 'Base/VirtualList',
  component: VirtualList,
}
export default meta

type Story = StoryObj<typeof VirtualList>

interface MockItem {
  id: number
  title: string
  desc: string
}

const mockItems: MockItem[] = Array.from({ length: 1000 }, (_, i) => ({
  id: i + 1,
  title: `项目 ${i + 1}`,
  desc: `这是第 ${i + 1} 个项目的描述`,
}))

export const Default: Story = {
  args: {
    items: mockItems,
    height: 400,
    itemHeight: 72,
    renderItem: (item, index) => {
      const i = item as MockItem
      return (
        <Paper key={index} variant="outlined" sx={{ m: 0.5, p: 2 }}>
          <Typography variant="subtitle1">{i.title}</Typography>
          <Typography variant="body2" color="text.secondary">
            {i.desc}
          </Typography>
        </Paper>
      )
    },
  },
}
