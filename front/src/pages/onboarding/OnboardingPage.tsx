import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box, Button, Paper, Step, StepContent,
  StepLabel, Stepper, Typography,
} from '@mui/material'
import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import InventoryIcon from '@mui/icons-material/Inventory'
import LiveTvIcon from '@mui/icons-material/LiveTv'

const STEPS = [
  {
    label: '绑定抖音账号',
    icon: <AccountCircleIcon />,
    description: '连接您的抖音账号，授权平台管理您的直播和短视频内容。',
    action: '去绑定账号',
    path: '/admin/douyin/accounts',
  },
  {
    label: '导入商品',
    icon: <InventoryIcon />,
    description: '将您的商品信息导入平台，系统将自动为商品生成 AI 卖点和话术。',
    action: '去添加商品',
    path: '/admin/product/list',
  },
  {
    label: '创建第一场直播',
    icon: <LiveTvIcon />,
    description: '创建直播场次，配置话术风格，使用 AI 一键生成完整的直播话术脚本。',
    action: '去创建场次',
    path: '/admin/live/sessions/create',
  },
]

export default function OnboardingPage() {
  const navigate = useNavigate()
  const [activeStep, setActiveStep] = useState(0)

  const handleNext = () => setActiveStep((s) => s + 1)
  const handleBack = () => setActiveStep((s) => s - 1)
  const handleGo = (path: string) => navigate(path)

  return (
    <Box sx={{ p: 3, maxWidth: 680, mx: 'auto' }}>
      <Typography variant="h4" sx={{ mb: 1, fontWeight: 700 }}>欢迎使用抖运营平台</Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        完成以下 3 个步骤，开始您的抖音直播运营之旅。
      </Typography>

      <Stepper activeStep={activeStep} orientation="vertical">
        {STEPS.map((step, index) => (
          <Step key={step.label}>
            <StepLabel icon={step.icon}>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>{step.label}</Typography>
            </StepLabel>
            <StepContent>
              <Paper variant="outlined" sx={{ p: 2.5, mb: 2 }}>
                <Typography variant="body2" color="text.secondary">{step.description}</Typography>
              </Paper>
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button variant="contained" onClick={() => handleGo(step.path)}>
                  {step.action}
                </Button>
                <Button variant="outlined" onClick={handleNext} disabled={index === STEPS.length - 1}>
                  稍后完成
                </Button>
                {index > 0 && (
                  <Button onClick={handleBack}>上一步</Button>
                )}
              </Box>
            </StepContent>
          </Step>
        ))}
      </Stepper>

      {activeStep === STEPS.length && (
        <Paper sx={{ p: 3, mt: 2, textAlign: 'center' }}>
          <Typography variant="h6" sx={{ mb: 1 }}>引导完成！</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            您已完成所有初始化步骤，开始使用平台全部功能。
          </Typography>
          <Button variant="contained" onClick={() => navigate('/admin/dashboard')}>
            进入控制台
          </Button>
        </Paper>
      )}
    </Box>
  )
}
