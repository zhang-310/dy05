import { lazy, Suspense } from 'react'
import { createBrowserRouter, Navigate, Outlet, useLocation } from 'react-router-dom'
import { ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION } from '@/constants/shortvideoRoutes'
import { isAuthenticated } from '@/utils/auth'
import { buildLoginHref } from '@/utils/login-redirect'
import { PageSkeleton } from '@/components/base'
import { AdminLayout } from '@/layouts/AdminLayout'
import { OrgLayout } from '@/layouts/OrgLayout'
import { TalentLayout } from '@/layouts/TalentLayout'

const LoginPage = lazy(() => import('@/pages/LoginPage'))
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage'))
const DashboardPage = lazy(() => import('@/pages/DashboardPage'))

// auth
const UsersPage = lazy(() => import('@/pages/auth/UsersPage'))
const RolesPage = lazy(() => import('@/pages/auth/RolesPage'))
const LoginLogsPage = lazy(() => import('@/pages/auth/LoginLogsPage'))

// douyin
const AccountsPage = lazy(() => import('@/pages/douyin/AccountsPage'))
const VideosPage = lazy(() => import('@/pages/douyin/VideosPage'))

// live
const SessionsPage = lazy(() => import('@/pages/live/SessionsPage'))
const LiveScriptsPage = lazy(() => import('@/pages/live/ScriptsPage'))
const LiveWorkbenchPage = lazy(() => import('@/pages/live/LiveWorkbenchPage'))
const SessionWorkspacePageWrapper = lazy(() => import('@/pages/live/SessionWorkspacePage'))

// product
const ProductsPage = lazy(() => import('@/pages/product/ProductsPage'))

// script
const ScriptListPage = lazy(() => import('@/pages/script/ScriptListPage'))
const ViolationWordPage = lazy(() => import('@/pages/script/ViolationWordPage'))

// shortvideo
const ProjectsPage = lazy(() => import('@/pages/shortvideo/ProjectsPage'))
const AccountCollectPage = lazy(() => import('@/pages/shortvideo/AccountCollectPage'))
const SvAccountListPage = lazy(() => import('@/pages/shortvideo/SvAccountListPage'))
const SvAccountDetailPage = lazy(() => import('@/pages/shortvideo/SvAccountDetailPage'))
const DouyinCookieManagePage = lazy(() => import('@/pages/benchmark/DouyinCookieManagePage'))
const RemakeTemplatePage = lazy(() => import('@/pages/shortvideo/RemakeTemplatePage'))

// copy
const CopyLibraryPage = lazy(() => import('@/pages/copy/CopyLibraryPage'))
const CopyTemplatePage = lazy(() => import('@/pages/copy/CopyTemplatePage'))

// ai
const KnowledgeBasePage = lazy(() => import('@/pages/ai/KnowledgeBasePage'))
const KnowledgeSourcePage = lazy(() => import('@/pages/ai/KnowledgeSourcePage'))
const EvolutionPage = lazy(() => import('@/pages/ai/EvolutionPage'))

// agent
const AgentListPage = lazy(() => import('@/pages/agent/AgentListPage'))
const AgentMarketPage = lazy(() => import('@/pages/agent/AgentMarketPage'))

// abtest
const ExperimentsPage = lazy(() => import('@/pages/abtest/ExperimentsPage'))

// config / storage / system / log
const ConfigPage = lazy(() => import('@/pages/config/ConfigPage'))
const StoragePage = lazy(() => import('@/pages/storage/StoragePage'))
const SystemPage = lazy(() => import('@/pages/system/SystemPage'))
const OperationLogPage = lazy(() => import('@/pages/log/OperationLogPage'))
const AuditLogPage = lazy(() => import('@/pages/log/AuditLogPage'))

// wecom / payment / attribution
const WecomPage = lazy(() => import('@/pages/wecom/WecomPage'))
const RobotsPage = lazy(() => import('@/pages/wecom/RobotsPage'))
const RulesPage = lazy(() => import('@/pages/wecom/RulesPage'))
const OrdersPage = lazy(() => import('@/pages/payment/OrdersPage'))
const SubscriptionPage = lazy(() => import('@/pages/payment/SubscriptionPage'))
const UsageQuotaPage = lazy(() => import('@/pages/payment/UsageQuotaPage'))
const AttributionPage = lazy(() => import('@/pages/attribution/AttributionPage'))

// Phase 1 new pages
const ResourcesPage = lazy(() => import('@/pages/auth/ResourcesPage'))
const ScriptRankingPage = lazy(() => import('@/pages/live/ScriptRankingPage'))
const RhythmPage = lazy(() => import('@/pages/live/RhythmPage'))
const HistoryComparePage = lazy(() => import('@/pages/live/HistoryComparePage'))
const CopyApprovalPage = lazy(() => import('@/pages/copy/CopyApprovalPage'))
const ScriptTemplatePage = lazy(() => import('@/pages/script/ScriptTemplatePage'))
const AlertRulesPage = lazy(() => import('@/pages/system/AlertRulesPage'))
const ApiLogPage = lazy(() => import('@/pages/system/ApiLogPage'))

// New pages — AI additional
const AiDashboardPage = lazy(() => import('@/pages/ai/AiDashboardPage'))
const KnowledgeSearchPage = lazy(() => import('@/pages/ai/KnowledgeSearchPage'))
const KnowledgeDocumentsPage = lazy(() => import('@/pages/ai/KnowledgeDocumentsPage'))

// New pages — System additional
const ComplianceCheckPage = lazy(() => import('@/pages/system/ComplianceCheckPage'))
const ExternalApiConfigPage = lazy(() => import('@/pages/system/ExternalApiConfigPage'))

// New pages — ShortVideo additional
const ViralVideoPage = lazy(() => import('@/pages/shortvideo/ViralVideoPage'))

// Phase 2 new pages — AI
const PromptTemplatePage = lazy(() => import('@/pages/ai/PromptTemplatePage'))
const ViralAnalysisPage = lazy(() => import('@/pages/ai/ViralAnalysisPage'))
const CreativeStudioPage = lazy(() => import('@/pages/ai/CreativeStudioPage'))
const AiCallLogPage = lazy(() => import('@/pages/ai/AiCallLogPage'))
const AiMonitoringPage = lazy(() => import('@/pages/ai/AiMonitoringPage'))
const DigitalHumanPage = lazy(() => import('@/pages/ai/DigitalHumanPage'))

// Phase 2 new pages — Agent / ABTest
const AgentChatPage = lazy(() => import('@/pages/agent/AgentChatPage'))
const AgentSharePage = lazy(() => import('@/pages/agent/AgentSharePage'))
const AgentWorkflowListPage = lazy(() => import('@/pages/agent/AgentWorkflowListPage'))
const AgentWorkflowEditorPage = lazy(() => import('@/pages/agent/AgentWorkflowEditorPage'))
const ExperimentDetailPage = lazy(() => import('@/pages/abtest/ExperimentDetailPage'))

// Phase 3 new pages — Org
const MembersPage = lazy(() => import('@/pages/org/MembersPage'))
const OrgAnalyticsPage = lazy(() => import('@/pages/org/OrgAnalyticsPage'))
const OrgLiveReviewsPage = lazy(() => import('@/pages/org/OrgLiveReviewsPage'))

// Final gap pages
const LiveSessionFormPage = lazy(() => import('@/pages/live/LiveSessionFormPage'))
const LiveRealtimePanelPage = lazy(() => import('@/pages/live/LiveRealtimePanelPage'))
const LiveProductPage = lazy(() => import('@/pages/live/LiveProductPage'))
const OrgLiveSessionPage = lazy(() => import('@/pages/live/OrgLiveSessionPage'))
const ProductScriptVersionPage = lazy(() => import('@/pages/product/ProductScriptVersionPage'))
const ProductScriptManagePage = lazy(() => import('@/pages/product/ProductScriptManagePage'))
const HybridSearchPage = lazy(() => import('@/pages/script/HybridSearchPage'))
const OnboardingPage = lazy(() => import('@/pages/onboarding/OnboardingPage'))
const UnifiedKpiPage = lazy(() => import('@/pages/UnifiedKpiPage'))

// Phase 2 new pages — ShortVideo
const ShotListPage = lazy(() => import('@/pages/shortvideo/ShotListPage'))
const MaterialPage = lazy(() => import('@/pages/shortvideo/MaterialPage'))
const VideoEditingPage = lazy(() => import('@/pages/shortvideo/VideoEditingPage'))
const PublishPage = lazy(() => import('@/pages/shortvideo/PublishPage'))
const QualityPage = lazy(() => import('@/pages/shortvideo/QualityPage'))
const DailyContentPage = lazy(() => import('@/pages/shortvideo/DailyContentPage'))

// New pages — AI deep
const KnowledgeEvolutionPage = lazy(() => import('@/pages/ai/KnowledgeEvolutionPage'))
const EvolutionReviewPage = lazy(() => import('@/pages/ai/EvolutionReviewPage'))
const IndustryBrainPage = lazy(() => import('@/pages/ai/IndustryBrainPage'))
const PromptLabPage = lazy(() => import('@/pages/ai/PromptLabPage'))
const ModelsConfigPage = lazy(() => import('@/pages/ai/ModelsConfigPage'))
const AdminInfraPage = lazy(() => import('@/pages/ai/AdminInfraPage'))

// New pages — ShortVideo additional
const ViralChainPage = lazy(() => import('@/pages/shortvideo/ViralChainPage'))
const HotTopicPage = lazy(() => import('@/pages/shortvideo/HotTopicPage'))
const HotTopicCreationPage = lazy(() => import('@/pages/shortvideo/HotTopicCreationPage'))
const ContentCalendarPage = lazy(() => import('@/pages/shortvideo/ContentCalendarPage'))
const DramaPage = lazy(() => import('@/pages/shortvideo/DramaPage'))

// New pages — System additional
const ExternalApiHealthPage = lazy(() => import('@/pages/system/ExternalApiHealthPage'))
const PerformanceMonitoringPage = lazy(() => import('@/pages/system/PerformanceMonitoringPage'))
const TianApiPanelPage = lazy(() => import('@/pages/system/TianApiPanelPage'))

// New pages — Product additional
const EffectivenessScorePage = lazy(() => import('@/pages/product/EffectivenessScorePage'))
const StylePresetPage = lazy(() => import('@/pages/product/StylePresetPage'))

// New pages — SlangDict
const SlangDictPage = lazy(() => import('@/pages/slangdict/SlangDictPage'))

// New pages — AI evolution
const EvolutionTasksPage = lazy(() => import('@/pages/ai/EvolutionTasksPage'))
const EvolutionTopicPage = lazy(() => import('@/pages/ai/EvolutionTopicPage'))

// New pages — Script additional
const ViolationCheckPage = lazy(() => import('@/pages/script/ViolationCheckPage'))

// New pages — this session
const ModelBenchmarkPage = lazy(() => import('@/pages/ai/ModelBenchmarkPage'))
const IndustryBrainDiagnosisPage = lazy(() => import('@/pages/ai/IndustryBrainDiagnosisPage'))
const PromptToolsPage = lazy(() => import('@/pages/ai/PromptToolsPage'))
const ScriptGenerationPage = lazy(() => import('@/pages/script/ScriptGenerationPage'))
const ScriptOptimizationPage = lazy(() => import('@/pages/script/ScriptOptimizationPage'))
const CompetitorMonitorPage = lazy(() => import('@/pages/shortvideo/CompetitorMonitorPage'))
const ContentEffectPredictPage = lazy(() => import('@/pages/shortvideo/ContentEffectPredictPage'))
const DataAnalysisPage = lazy(() => import('@/pages/shortvideo/DataAnalysisPage'))
const ShortVideoDashboardPage = lazy(() => import('@/pages/shortvideo/ShortVideoDashboardPage'))
const ShortVideoInsightsHubPage = lazy(() => import('@/pages/shortvideo/ShortVideoInsightsHubPage'))
const AiQuotaPage = lazy(() => import('@/pages/ai/AiQuotaPage'))
const TaskModelConfigPage = lazy(() => import('@/pages/ai/TaskModelConfigPage'))
const QuickGeneratePage = lazy(() => import('@/pages/shortvideo/QuickGeneratePage'))
const ScriptPlanningPage = lazy(() => import('@/pages/shortvideo/ScriptPlanningPage'))
const MaterialPreparationPage = lazy(() => import('@/pages/shortvideo/MaterialPreparationPage'))
const MaterialProductionPage = lazy(() => import('@/pages/shortvideo/MaterialProductionPage'))
const SvProjectWorkbenchPage = lazy(() => import('@/pages/shortvideo/SvProjectWorkbenchPage'))
const WorkflowEditorPage = lazy(() => import('@/pages/shortvideo/WorkflowEditorPage'))
const SubtitleEditorPage = lazy(() => import('@/pages/shortvideo/SubtitleEditorPage'))
const PersonaViralFusionPage = lazy(() => import('@/pages/shortvideo/PersonaViralFusionPage'))
const AiMusicPage = lazy(() => import('@/pages/shortvideo/AiMusicPage'))
const SeoOptimizePage = lazy(() => import('@/pages/shortvideo/SeoOptimizePage'))
const ProductReadinessPage = lazy(() => import('@/pages/product/ProductReadinessPage'))

// Final gap pages — round 2
const DouyinAccountDetailPage = lazy(() => import('@/pages/douyin/DouyinAccountDetailPage'))
const SalesHistoryPage = lazy(() => import('@/pages/product/SalesHistoryPage'))
const SyncLogPage = lazy(() => import('@/pages/system/SyncLogPage'))
const SystemLogPage = lazy(() => import('@/pages/log/SystemLogPage'))
const ContentLibraryPage = lazy(() => import('@/pages/content/ContentLibraryPage'))
const CopyPage = lazy(() => import('@/pages/copy/CopyPage'))

function LazyRoute({ element }: { element: React.ReactNode }) {
  return <Suspense fallback={<PageSkeleton />}>{element}</Suspense>
}

function RequireAuth() {
  const location = useLocation()
  if (!isAuthenticated()) {
    const returnPath = location.pathname + location.search + location.hash
    return <Navigate to={buildLoginHref(returnPath)} replace />
  }
  return <Outlet />
}

export const router = createBrowserRouter([
  { path: '/login', element: <LazyRoute element={<LoginPage />} /> },
  {
    element: <RequireAuth />,
    children: [
      {
        path: '/admin',
        element: <AdminLayout />,
        children: [
          { index: true, element: <Navigate to="/admin/dashboard" replace /> },
          { path: 'dashboard', element: <LazyRoute element={<DashboardPage />} /> },
          { path: 'auth/users', element: <LazyRoute element={<UsersPage />} /> },
          { path: 'auth/roles', element: <LazyRoute element={<RolesPage />} /> },
          { path: 'auth/login-logs', element: <LazyRoute element={<LoginLogsPage />} /> },
          { path: 'douyin/accounts', element: <LazyRoute element={<AccountsPage />} /> },
          { path: 'live/sessions', element: <LazyRoute element={<SessionsPage />} /> },
          { path: 'live/scripts', element: <LazyRoute element={<LiveScriptsPage />} /> },
          { path: 'live/workbench/:sessionId', element: <LazyRoute element={<LiveWorkbenchPage />} /> },
          { path: 'product/list', element: <LazyRoute element={<ProductsPage />} /> },
          { path: 'script/list', element: <LazyRoute element={<ScriptListPage />} /> },
          { path: 'script/violation-words', element: <LazyRoute element={<ViolationWordPage />} /> },
          { path: 'shortvideo/projects', element: <LazyRoute element={<ProjectsPage />} /> },
          { path: 'shortvideo/collect', element: <LazyRoute element={<AccountCollectPage />} /> },
          { path: 'shortvideo/accounts', element: <LazyRoute element={<SvAccountListPage />} /> },
          { path: 'shortvideo/accounts/:id', element: <LazyRoute element={<SvAccountDetailPage />} /> },
          { path: 'shortvideo/douyin-cookies', element: <LazyRoute element={<DouyinCookieManagePage />} /> },
          { path: 'shortvideo/remake-templates', element: <LazyRoute element={<RemakeTemplatePage />} /> },
          { path: 'copy/library', element: <LazyRoute element={<CopyLibraryPage />} /> },
          { path: 'copy/templates', element: <LazyRoute element={<CopyTemplatePage />} /> },
          { path: 'ai/knowledge', element: <LazyRoute element={<KnowledgeBasePage />} /> },
          { path: 'ai/knowledge-source', element: <LazyRoute element={<KnowledgeSourcePage />} /> },
          { path: 'ai/evolution', element: <LazyRoute element={<EvolutionPage />} /> },
          { path: 'config', element: <LazyRoute element={<ConfigPage />} /> },
          { path: 'storage', element: <LazyRoute element={<StoragePage />} /> },
          { path: 'system', element: <LazyRoute element={<SystemPage />} /> },
          { path: 'log/operations', element: <LazyRoute element={<OperationLogPage />} /> },
          { path: 'wecom', element: <LazyRoute element={<WecomPage />} /> },
          { path: 'wecom/robots', element: <LazyRoute element={<RobotsPage />} /> },
          { path: 'wecom/rules', element: <LazyRoute element={<RulesPage />} /> },
          { path: 'douyin/videos', element: <LazyRoute element={<VideosPage />} /> },
          { path: 'payment/orders', element: <LazyRoute element={<OrdersPage />} /> },
          { path: 'payment/subscription', element: <LazyRoute element={<SubscriptionPage />} /> },
          { path: 'payment/usage', element: <LazyRoute element={<UsageQuotaPage />} /> },
          { path: 'attribution', element: <LazyRoute element={<AttributionPage />} /> },
          { path: 'auth/resources', element: <LazyRoute element={<ResourcesPage />} /> },
          { path: 'live/ranking', element: <LazyRoute element={<ScriptRankingPage />} /> },
          { path: 'live/rhythm', element: <LazyRoute element={<RhythmPage />} /> },
          { path: 'live/history-compare', element: <LazyRoute element={<HistoryComparePage />} /> },
          { path: 'copy/approval', element: <LazyRoute element={<CopyApprovalPage />} /> },
          { path: 'script/templates', element: <LazyRoute element={<ScriptTemplatePage />} /> },
          { path: 'system/alert-rules', element: <LazyRoute element={<AlertRulesPage />} /> },
          { path: 'system/api-log', element: <LazyRoute element={<ApiLogPage />} /> },
          { path: 'log/audit', element: <LazyRoute element={<AuditLogPage />} /> },
          // New routes — AI additional
          { path: 'ai/dashboard', element: <LazyRoute element={<AiDashboardPage />} /> },
          { path: 'ai/knowledge-search', element: <LazyRoute element={<KnowledgeSearchPage />} /> },
          { path: 'ai/knowledge/:kbId/documents', element: <LazyRoute element={<KnowledgeDocumentsPage />} /> },
          // New routes — System additional
          { path: 'system/compliance', element: <LazyRoute element={<ComplianceCheckPage />} /> },
          { path: 'system/external-api', element: <LazyRoute element={<ExternalApiConfigPage />} /> },
          // New routes — ShortVideo additional
          { path: 'shortvideo/viral-videos', element: <LazyRoute element={<ViralVideoPage />} /> },
          // Phase 2 routes — AI
          { path: 'ai/prompt-templates', element: <LazyRoute element={<PromptTemplatePage />} /> },
          { path: 'ai/viral-analysis', element: <LazyRoute element={<ViralAnalysisPage />} /> },
          { path: 'ai/creative-studio', element: <LazyRoute element={<CreativeStudioPage />} /> },
          { path: 'ai/call-log', element: <LazyRoute element={<AiCallLogPage />} /> },
          { path: 'ai/monitoring', element: <LazyRoute element={<AiMonitoringPage />} /> },
          { path: 'ai/digital-human', element: <LazyRoute element={<DigitalHumanPage />} /> },
          // Phase 2 routes — ShortVideo
          { path: 'shortvideo/shot-list', element: <LazyRoute element={<ShotListPage />} /> },
          { path: 'shortvideo/material', element: <LazyRoute element={<MaterialPage />} /> },
          { path: 'shortvideo/editing', element: <LazyRoute element={<VideoEditingPage />} /> },
          { path: 'shortvideo/publish', element: <LazyRoute element={<PublishPage />} /> },
          { path: 'shortvideo/quality', element: <LazyRoute element={<QualityPage />} /> },
          { path: 'shortvideo/daily', element: <LazyRoute element={<DailyContentPage />} /> },
          // New routes — AI deep
          { path: 'ai/knowledge-evolution', element: <LazyRoute element={<KnowledgeEvolutionPage />} /> },
          { path: 'ai/evolution-review', element: <LazyRoute element={<EvolutionReviewPage />} /> },
          { path: 'ai/industry-brain', element: <LazyRoute element={<IndustryBrainPage />} /> },
          { path: 'ai/industry-brain/diagnosis', element: <LazyRoute element={<IndustryBrainDiagnosisPage />} /> },
          { path: 'ai/prompt-lab', element: <LazyRoute element={<PromptLabPage />} /> },
          { path: 'ai/models-config', element: <LazyRoute element={<ModelsConfigPage />} /> },
          { path: 'ai/admin-infra', element: <LazyRoute element={<AdminInfraPage />} /> },
          // New routes — AI: Agent / ABTest (intelligence module)
          { path: 'ai/agent/list', element: <LazyRoute element={<AgentListPage />} /> },
          { path: 'ai/agent/market', element: <LazyRoute element={<AgentMarketPage />} /> },
          { path: 'ai/agent/chat/:id', element: <LazyRoute element={<AgentChatPage />} /> },
          { path: 'ai/agent/share/:shareCode', element: <LazyRoute element={<AgentSharePage />} /> },
          { path: 'ai/agent/workflow/list', element: <LazyRoute element={<AgentWorkflowListPage />} /> },
          { path: 'ai/agent/workflow/edit/:id', element: <LazyRoute element={<AgentWorkflowEditorPage />} /> },
          { path: 'ai/agent/workflow/edit', element: <LazyRoute element={<AgentWorkflowEditorPage />} /> },
          { path: 'ai/abtest/experiments', element: <LazyRoute element={<ExperimentsPage />} /> },
          { path: 'ai/abtest/:id', element: <LazyRoute element={<ExperimentDetailPage />} /> },
          // New routes — ShortVideo additional
          { path: 'shortvideo/viral-chain', element: <LazyRoute element={<ViralChainPage />} /> },
          { path: 'shortvideo/hot-topics/create', element: <LazyRoute element={<HotTopicCreationPage />} /> },
          { path: 'shortvideo/hot-topics', element: <LazyRoute element={<HotTopicPage />} /> },
          { path: 'shortvideo/content-calendar', element: <LazyRoute element={<ContentCalendarPage />} /> },
          { path: 'shortvideo/drama', element: <LazyRoute element={<DramaPage />} /> },
          // New routes — System additional
          { path: 'system/external-api-health', element: <LazyRoute element={<ExternalApiHealthPage />} /> },
          { path: 'system/performance', element: <LazyRoute element={<PerformanceMonitoringPage />} /> },
          { path: 'system/tianapi', element: <LazyRoute element={<TianApiPanelPage />} /> },
          // New routes — Product additional
          { path: 'product/effectiveness', element: <LazyRoute element={<EffectivenessScorePage />} /> },
          { path: 'product/style-presets', element: <LazyRoute element={<StylePresetPage />} /> },
          // New routes — SlangDict
          { path: 'slangdict', element: <LazyRoute element={<SlangDictPage />} /> },
          // New routes — AI evolution
          { path: 'ai/evolution-tasks', element: <LazyRoute element={<EvolutionTasksPage />} /> },
          { path: 'ai/evolution-topics', element: <LazyRoute element={<EvolutionTopicPage />} /> },
          // New routes — Script additional
          { path: 'script/violation-check', element: <LazyRoute element={<ViolationCheckPage />} /> },
          // New routes — this session
          { path: 'ai/model-benchmark', element: <LazyRoute element={<ModelBenchmarkPage />} /> },
          { path: 'ai/quota', element: <LazyRoute element={<AiQuotaPage />} /> },
          { path: 'ai/task-model-config', element: <LazyRoute element={<TaskModelConfigPage />} /> },
          { path: 'ai/brain-diagnosis', element: <LazyRoute element={<IndustryBrainDiagnosisPage />} /> },
          { path: 'ai/prompt-tools', element: <LazyRoute element={<PromptToolsPage />} /> },
          { path: 'script/generation', element: <LazyRoute element={<ScriptGenerationPage />} /> },
          { path: 'script/optimization', element: <LazyRoute element={<ScriptOptimizationPage />} /> },
          { path: 'shortvideo/competitor-monitor', element: <LazyRoute element={<CompetitorMonitorPage />} /> },
          { path: 'shortvideo/effect-predict', element: <LazyRoute element={<ContentEffectPredictPage />} /> },
          { path: 'shortvideo/data-analysis', element: <LazyRoute element={<DataAnalysisPage />} /> },
          { path: 'shortvideo/dashboard', element: <LazyRoute element={<ShortVideoDashboardPage />} /> },
          { path: 'shortvideo/insights', element: <LazyRoute element={<ShortVideoInsightsHubPage />} /> },
          {
            path: 'shortvideo/viral-analysis',
            element: <Navigate to={ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION} replace />,
          },
          { path: 'shortvideo/quick-generate', element: <LazyRoute element={<QuickGeneratePage />} /> },
          { path: 'shortvideo/script-planning', element: <LazyRoute element={<ScriptPlanningPage />} /> },
          { path: 'shortvideo/material-prepare', element: <LazyRoute element={<MaterialPreparationPage />} /> },
          { path: 'shortvideo/material-production', element: <LazyRoute element={<MaterialProductionPage />} /> },
          { path: 'shortvideo/workbench', element: <LazyRoute element={<SvProjectWorkbenchPage />} /> },
          { path: 'shortvideo/workflow-editor', element: <LazyRoute element={<WorkflowEditorPage />} /> },
          { path: 'shortvideo/subtitle-editor/:id', element: <LazyRoute element={<SubtitleEditorPage />} /> },
          { path: 'shortvideo/persona-fusion', element: <LazyRoute element={<PersonaViralFusionPage />} /> },
          { path: 'shortvideo/ai-music', element: <LazyRoute element={<AiMusicPage />} /> },
          { path: 'shortvideo/seo-optimize', element: <LazyRoute element={<SeoOptimizePage />} /> },
          { path: 'product/readiness', element: <LazyRoute element={<ProductReadinessPage />} /> },
          // Final gap routes — round 2
          { path: 'douyin/accounts/:id', element: <LazyRoute element={<DouyinAccountDetailPage />} /> },
          { path: 'product/sales-history', element: <LazyRoute element={<SalesHistoryPage />} /> },
          { path: 'system/sync-log', element: <LazyRoute element={<SyncLogPage />} /> },
          { path: 'log/system', element: <LazyRoute element={<SystemLogPage />} /> },
          { path: 'content/library', element: <LazyRoute element={<ContentLibraryPage />} /> },
          { path: 'copy/all', element: <LazyRoute element={<CopyPage />} /> },
          // Final gap routes
          { path: 'live/sessions/create', element: <LazyRoute element={<LiveSessionFormPage />} /> },
          { path: 'live/sessions/:sessionId', element: <LazyRoute element={<SessionWorkspacePageWrapper />} /> },
          { path: 'live/sessions/:id/edit', element: <LazyRoute element={<LiveSessionFormPage />} /> },
          { path: 'live/sessions/:id/realtime', element: <LazyRoute element={<LiveRealtimePanelPage />} /> },
          { path: 'live/products', element: <LazyRoute element={<LiveProductPage />} /> },
          { path: 'product/:productId/scripts', element: <LazyRoute element={<ProductScriptManagePage />} /> },
          { path: 'product/:productId/script-versions', element: <LazyRoute element={<ProductScriptVersionPage />} /> },
          { path: 'script/hybrid-search', element: <LazyRoute element={<HybridSearchPage />} /> },
          { path: 'onboarding', element: <LazyRoute element={<OnboardingPage />} /> },
          { path: 'kpi', element: <LazyRoute element={<UnifiedKpiPage />} /> },
        ],

      },
      {
        path: '/org',
        element: <OrgLayout />,
        children: [
          { index: true, element: <Navigate to="/org/dashboard" replace /> },
          { path: 'dashboard', element: <LazyRoute element={<DashboardPage />} /> },
          { path: 'members', element: <LazyRoute element={<MembersPage />} /> },
          { path: 'analytics', element: <LazyRoute element={<OrgAnalyticsPage />} /> },
          { path: 'live/sessions', element: <LazyRoute element={<OrgLiveSessionPage />} /> },
          { path: 'live/sessions/:sessionId', element: <LazyRoute element={<LiveWorkbenchPage />} /> },
          { path: 'live/reviews', element: <LazyRoute element={<OrgLiveReviewsPage />} /> },
        ],
      },
      {
        path: '/talent',
        element: <TalentLayout />,
        children: [
          { index: true, element: <Navigate to="/talent/dashboard" replace /> },
          { path: 'dashboard', element: <LazyRoute element={<DashboardPage />} /> },
          { path: 'live/sessions', element: <LazyRoute element={<SessionsPage />} /> },
          { path: 'live/sessions/:sessionId', element: <LazyRoute element={<LiveWorkbenchPage />} /> },
          { path: 'shortvideo', element: <LazyRoute element={<ProjectsPage />} /> },
        ],
      },
    ],
  },
  { path: '/', element: <Navigate to="/admin/dashboard" replace /> },
  { path: '*', element: <LazyRoute element={<NotFoundPage />} /> },
])
