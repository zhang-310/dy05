import type { Meta, StoryObj } from '@storybook/react'
import { PageHeader } from './PageHeader'

const meta: Meta<typeof PageHeader> = {
  title: 'Base/PageHeader',
  component: PageHeader,
}
export default meta

type Story = StoryObj<typeof PageHeader>

export const Default: Story = {
  args: {
    title: '页面标题',
    subtitle: '页面描述或副标题',
  },
}

export const WithActions: Story = {
  args: {
    title: '知识库',
    subtitle: '管理知识库与文档',
    actions: <button>创建知识库</button>,
  },
}

export const WithBreadcrumbs: Story = {
  args: {
    title: '文档列表',
    subtitle: '知识库文档管理',
    breadcrumbs: [
      { label: '首页', href: '/' },
      { label: '知识库', href: '/knowledge' },
      { label: '文档列表' },
    ],
  },
}
