## DashboardController
Base: /api/v1/dashboard

- [Post] /api/v1/dashboard/admin
- [Post] /api/v1/dashboard/org
- [Post] /api/v1/dashboard/talent

## DashboardController
Base: /api/v1/dashboard

- [Post] /api/v1/dashboard/admin/stats
- [Post] /api/v1/dashboard/org/stats
- [Post] /api/v1/dashboard/kpi-unified
- [Post] /api/v1/dashboard/live-format-gmv
- [Post] /api/v1/dashboard/product-gmv-summary
- [Post] /api/v1/dashboard/cockpit-preview
- [Post] /api/v1/dashboard/profit-matrix-preview
- [Post] /api/v1/dashboard/conversion-funnel
- [Post] /api/v1/dashboard/cockpit-export

## LiveCompetitorMonitorBridgeController
Base: /api/v1/live/competitor-monitor

- [Post] /api/v1/live/competitor-monitor/list

## StorageController
Base: /api/v1/storage

- [Post] /api/v1/storage/configured
- [Post] /api/v1/storage/list
- [Post] /api/v1/storage/upload
- [Post] /api/v1/storage/delete
- [Post] /api/v1/storage/url

## UploadController
Base: /api/v1/storage/upload

- [Post] /api/v1/storage/upload/init
- [Post] /api/v1/storage/upload/chunk
- [Get] /api/v1/storage/upload/chunks
- [Get] /api/v1/storage/upload/progress
- [Post] /api/v1/storage/upload/complete
- [Post] /api/v1/storage/upload/cancel

## CopyApprovalController
Base: /api/v1/copy/approval

- [Post] /api/v1/copy/approval/search
- [Post] /api/v1/copy/approval/get
- [Post] /api/v1/copy/approval/save
- [Post] /api/v1/copy/approval/delete

## CopyLibraryController
Base: /api/v1/copy/library

- [Post] /api/v1/copy/library/search
- [Post] /api/v1/copy/library/get
- [Post] /api/v1/copy/library/save
- [Post] /api/v1/copy/library/delete
- [Post] /api/v1/copy/library/update-status
- [Post] /api/v1/copy/library/increment-use-count

## CopyTemplateController
Base: /api/v1/copy/template

- [Post] /api/v1/copy/template/search
- [Post] /api/v1/copy/template/get
- [Post] /api/v1/copy/template/save
- [Post] /api/v1/copy/template/delete
- [Post] /api/v1/copy/template/update-status

## ComplianceController
Base: /api/v1/script/compliance

- [Post] /api/v1/script/compliance/check
- [Post] /api/v1/script/compliance/rules
- [Post] /api/v1/script/compliance/industry-codes
- [Post] /api/v1/script/compliance/douyin-official-references

## ComplianceWordAdminController
Base: /api/v1/script/admin/compliance

- [Post] /api/v1/script/admin/compliance/refresh

## HybridSearchController
Base: /api/v1/script/search

- [Post] /api/v1/script/search/hybrid
- [Post] /api/v1/script/search/semantic
- [Post] /api/v1/script/search/lexical
- [Post] /api/v1/script/search/suggest
- [Post] /api/v1/script/search/analytics
- [Post] /api/v1/script/search/feedback

## ScriptController
Base: /api/v1/script

- [Post] /api/v1/script/list
- [Post] /api/v1/script/get
- [Post] /api/v1/script/save
- [Post] /api/v1/script/delete
- [Post] /api/v1/script/use-count
- [Post] /api/v1/script/categories
- [Post] /api/v1/script/violation/check-batch
- [Post] /api/v1/script/violation/check
- [Post] /api/v1/script/violation/public/list
- [Post] /api/v1/script/violation/suggest-replacement

## ScriptGenerationController
Base: /api/v1/script

- [Post] /api/v1/script/generate

## ScriptTemplateAdminController
Base: /api/v1/script/admin/template

- [Post] /api/v1/script/admin/template/list
- [Post] /api/v1/script/admin/template/save
- [Post] /api/v1/script/admin/template/delete

## ScriptTemplateController
Base: /api/v1/script/template

- [Post] /api/v1/script/template/search
- [Post] /api/v1/script/template/get
- [Post] /api/v1/script/template/save
- [Post] /api/v1/script/template/delete
- [Post] /api/v1/script/template/use-count
- [Post] /api/v1/script/template/by-scene

## UserViolationWordController
Base: /api/v1/script/user-violation

- [Post] /api/v1/script/user-violation/search
- [Post] /api/v1/script/user-violation/get
- [Post] /api/v1/script/user-violation/save
- [Post] /api/v1/script/user-violation/delete
- [Post] /api/v1/script/user-violation/active

## ViolationWordAdminController
Base: /api/v1/script/admin/violation

- [Post] /api/v1/script/admin/violation/list
- [Post] /api/v1/script/admin/violation/save
- [Post] /api/v1/script/admin/violation/delete
- [Post] /api/v1/script/admin/violation/active
- [Post] /api/v1/script/admin/violation/import
- [Post] /api/v1/script/admin/violation/export

## WorkflowController
Base: /api/v1/workflow

- [Post] /api/v1/workflow/execute

## DouyinAccountController
Base: /api/v1/douyin/account

- [Post] /api/v1/douyin/account/search
- [Post] /api/v1/douyin/account/get
- [Post] /api/v1/douyin/account/save
- [Post] /api/v1/douyin/account/delete
- [Post] /api/v1/douyin/account/statistics

## DouyinPersonaController
Base: /api/v1/douyin/persona

- [Post] /api/v1/douyin/persona/save
- [Post] /api/v1/douyin/persona/list
- [Post] /api/v1/douyin/persona/get
- [Post] /api/v1/douyin/persona/delete
- [Post] /api/v1/douyin/persona/set-default
- [Post] /api/v1/douyin/persona/get-default
- [Post] /api/v1/douyin/persona/templates
- [Post] /api/v1/douyin/persona/get-by-account

## DouyinVideoController
Base: /api/v1/douyin/video

- [Post] /api/v1/douyin/video/search
- [Post] /api/v1/douyin/video/get
- [Post] /api/v1/douyin/video/save
- [Post] /api/v1/douyin/video/sync

## FanProfileController
Base: /api/v1/douyin/fan-profile

- [Post] /api/v1/douyin/fan-profile/get
- [Post] /api/v1/douyin/fan-profile/stats
- [Post] /api/v1/douyin/fan-profile/sync/{accountId}

## DouyinOAuthController
Base: /api/v1/douyin/oauth

- [Get] /api/v1/douyin/oauth/authorize-url
- [Get] /api/v1/douyin/oauth/callback
- [Post] /api/v1/douyin/oauth/refresh-token
- [Post] /api/v1/douyin/oauth/revoke
- [Post] /api/v1/douyin/oauth/token-status
- [Post] /api/v1/douyin/oauth/token-refresh
- [Post] /api/v1/douyin/oauth/auth-url

## MessagingController
Base: /api/v1/messaging

- [Post] /api/v1/messaging/config/list
- [Post] /api/v1/messaging/config/get
- [Post] /api/v1/messaging/config/save
- [Post] /api/v1/messaging/config/delete

## MessagingWebhookController
Base: /api/v1/messaging/webhook

- [Post] /api/v1/messaging/webhook/feishu
- [Get] /api/v1/messaging/webhook/wecom
- [Post] /api/v1/messaging/webhook/wecom

## SmsController
Base: /api/v1/sms

- [Post] /api/v1/sms/provider/list
- [Post] /api/v1/sms/provider/get
- [Post] /api/v1/sms/provider/save
- [Post] /api/v1/sms/provider/delete
- [Post] /api/v1/sms/provider/update-status
- [Post] /api/v1/sms/provider/set-default
- [Post] /api/v1/sms/template/list
- [Post] /api/v1/sms/template/get
- [Post] /api/v1/sms/template/save
- [Post] /api/v1/sms/template/delete
- [Post] /api/v1/sms/template/update-status
- [Post] /api/v1/sms/log/list
- [Post] /api/v1/sms/log/get
- [Post] /api/v1/sms/code/send
- [Post] /api/v1/sms/code/verify
- [Post] /api/v1/sms/code/get-latest

## TianApiController
Base: /api/v1/tianapi

- [Post] /api/v1/tianapi/hot/douyin
- [Post] /api/v1/tianapi/hot/toutiao
- [Post] /api/v1/tianapi/hot/weibo
- [Post] /api/v1/tianapi/hot/network
- [Post] /api/v1/tianapi/hot/baidu
- [Post] /api/v1/tianapi/hot/tencent
- [Post] /api/v1/tianapi/material/pyq-wenan
- [Post] /api/v1/tianapi/material/dagongren
- [Post] /api/v1/tianapi/material/tuwei-qinghua
- [Post] /api/v1/tianapi/material/dujitang
- [Post] /api/v1/tianapi/material/caihongpi
- [Post] /api/v1/tianapi/material/zhanan
- [Post] /api/v1/tianapi/material/zaoan
- [Post] /api/v1/tianapi/material/wanan
- [Post] /api/v1/tianapi/material/dialogue
- [Post] /api/v1/tianapi/material/godreply
- [Post] /api/v1/tianapi/material/cangtoushi
- [Post] /api/v1/tianapi/material/hotword
- [Post] /api/v1/tianapi/material/dictum
- [Post] /api/v1/tianapi/material/mingyan
- [Post] /api/v1/tianapi/material/tiangou
- [Post] /api/v1/tianapi/material/joke
- [Post] /api/v1/tianapi/material/xiehouyu
- [Post] /api/v1/tianapi/material/moodpoetry
- [Post] /api/v1/tianapi/material/msdl
- [Post] /api/v1/tianapi/material/flmj
- [Post] /api/v1/tianapi/material/zmsc
- [Post] /api/v1/tianapi/material/gjmj
- [Post] /api/v1/tianapi/material/lzmy
- [Post] /api/v1/tianapi/material/hotreview
- [Post] /api/v1/tianapi/material/mnpara
- [Post] /api/v1/tianapi/material/skl
- [Post] /api/v1/tianapi/material/sentence
- [Post] /api/v1/tianapi/material/qingshi
- [Post] /api/v1/tianapi/material/hsjz
- [Post] /api/v1/tianapi/material/raokouling
- [Post] /api/v1/tianapi/ad-review
- [Post] /api/v1/tianapi/text-audit
- [Post] /api/v1/tianapi/ai-text
- [Post] /api/v1/tianapi/jiejiari
- [Post] /api/v1/tianapi/bulletin
- [Post] /api/v1/tianapi/status
- [Post] /api/v1/tianapi/material/import

## WecomController
Base: /api/v1/wecom

- [Post] /api/v1/wecom/robot/list
- [Post] /api/v1/wecom/robot/get
- [Post] /api/v1/wecom/robot/save
- [Post] /api/v1/wecom/robot/delete
- [Post] /api/v1/wecom/robot/update-status
- [Post] /api/v1/wecom/rule/list
- [Post] /api/v1/wecom/rule/get
- [Post] /api/v1/wecom/rule/save
- [Post] /api/v1/wecom/rule/delete
- [Post] /api/v1/wecom/rule/update-status
- [Post] /api/v1/wecom/log/list
- [Post] /api/v1/wecom/push

## AbTestController
Base: /api/v1/abtest

- [Post] /api/v1/abtest/experiment/list
- [Post] /api/v1/abtest/experiment/get
- [Post] /api/v1/abtest/experiment/save
- [Post] /api/v1/abtest/experiment/delete
- [Post] /api/v1/abtest/experiment/update-status
- [Post] /api/v1/abtest/experiment/set-winner
- [Post] /api/v1/abtest/variant/save
- [Post] /api/v1/abtest/variant/delete
- [Post] /api/v1/abtest/event/record
- [Post] /api/v1/abtest/experiment/result
- [Post] /api/v1/abtest/experiment/daily-trend
- [Post] /api/v1/abtest/script-style/assign
- [Post] /api/v1/abtest/script-style/record-conversion

## AgentController
Base: /api/v1/agent

- [Post] /api/v1/agent/list
- [Post] /api/v1/agent/get
- [Post] /api/v1/agent/save
- [Post] /api/v1/agent/delete
- [Post] /api/v1/agent/update-status
- [Post] /api/v1/agent/conversation/create
- [Post] /api/v1/agent/conversation/list
- [Post] /api/v1/agent/conversation/delete
- [Post] /api/v1/agent/message/send
- [Post] /api/v1/agent/message/list
- [Post] /api/v1/agent/message/rate
- [Post] /api/v1/agent/conversation/export
- [Post] /api/v1/agent/share/create
- [Post] /api/v1/agent/share/get
- [Post] /api/v1/agent/share/list
- [Post] /api/v1/agent/share/delete
- [Post] /api/v1/agent/share/data
- [Post] /api/v1/agenttext/event-stream;charset=UTF-8
- [Get] /api/v1/agenttext/event-stream;charset=UTF-8

## AgentReviewController
Base: /api/v1/agent/review

- [Post] /api/v1/agent/review/stats
- [Post] /api/v1/agent/review/list
- [Post] /api/v1/agent/review/submit
- [Post] /api/v1/agent/review/my

## AgentWorkflowController
Base: /api/v1/agent/workflow

- [Post] /api/v1/agent/workflow/list
- [Post] /api/v1/agent/workflow/get
- [Post] /api/v1/agent/workflow/save
- [Post] /api/v1/agent/workflow/delete
- [Post] /api/v1/agent/workflow/execution/list
- [Post] /api/v1/agent/workflow/execution/workflow-list
- [Post] /api/v1/agent/workflow/execution/get
- [Post] /api/v1/agent/workflow/execute

## UserPreferenceController
Base: /api/v1/agent

- [Post] /api/v1/agent/preference/refine-suggestions

## AiAdminCallLogController
Base: /api/v1/ai/admin/call-log

- [Post] /api/v1/ai/admin/call-log/search

## AiAdminInfraController
Base: /api/v1/ai/admin/infra

- [Post] /api/v1/ai/admin/infra/monitoring-config
- [Post] /api/v1/ai/admin/infra/health
- [Post] /api/v1/ai/admin/infra/detail
- [Post] /api/v1/ai/admin/infra/documents/pg
- [Post] /api/v1/ai/admin/infra/documents/es
- [Post] /api/v1/ai/admin/infra/milvus/stats
- [Post] /api/v1/ai/admin/infra/cache/stats
- [Post] /api/v1/ai/admin/infra/search/stats
- [Post] /api/v1/ai/admin/infra/queue

## AiCallLogController
Base: /api/v1/ai/call-log

- [Post] /api/v1/ai/call-log/link

## AiController
Base: /api/v1/ai

- [Post] /api/v1/ai/model/list
- [Post] /api/v1/ai/model/get
- [Post] /api/v1/ai/model/save
- [Post] /api/v1/ai/model/delete
- [Post] /api/v1/ai/task/list
- [Post] /api/v1/ai/task/get
- [Post] /api/v1/ai/task/create
- [Post] /api/v1/ai/task/complete
- [Post] /api/v1/ai/prompt/list
- [Post] /api/v1/ai/prompt/save
- [Post] /api/v1/ai/prompt/delete
- [Post] /api/v1/ai/knowledge/list
- [Post] /api/v1/ai/knowledge/get
- [Post] /api/v1/ai/knowledge/save
- [Post] /api/v1/ai/knowledge/delete
- [Post] /api/v1/ai/knowledge/status

## AiDashboardController
Base: /api/v1/ai/admin/dashboard

- [Post] /api/v1/ai/admin/dashboard/stats
- [Post] /api/v1/ai/admin/dashboard/call-volume-trend
- [Post] /api/v1/ai/admin/dashboard/quota-trend
- [Post] /api/v1/ai/admin/dashboard/call-type-distribution
- [Post] /api/v1/ai/admin/dashboard/cost-breakdown

## AiModelController
Base: /api/v1/ai/admin/models

- [Post] /api/v1/ai/admin/models/list
- [Post] /api/v1/ai/admin/models/test-connection
- [Post] /api/v1/ai/admin/models/save
- [Post] /api/v1/ai/admin/models/delete
- [Post] /api/v1/ai/admin/models/set-default

## AiQuotaController
Base: /api/v1/ai/admin/quota

- [Post] /api/v1/ai/admin/quota/get
- [Post] /api/v1/ai/admin/quota/update
- [Post] /api/v1/ai/admin/quota/history
- [Post] /api/v1/ai/admin/quota/info

## DigitalHumanController
Base: /api/v1/ai/digital-human

- [Post] /api/v1/ai/digital-human/status
- [Post] /api/v1/ai/digital-human/generate

## EvolutionController
Base: /api/v1/ai/evolution

- [Post] /api/v1/ai/evolution/viral/list
- [Post] /api/v1/ai/evolution/viral/get
- [Post] /api/v1/ai/evolution/viral/trigger
- [Post] /api/v1/ai/evolution/viral/complete
- [Post] /api/v1/ai/evolution/viral/delete
- [Post] /api/v1/ai/evolution/task/list
- [Post] /api/v1/ai/evolution/task/trigger
- [Post] /api/v1/ai/evolution/task/cancel
- [Post] /api/v1/ai/evolution/live-review/list
- [Post] /api/v1/ai/evolution/live-review/get
- [Post] /api/v1/ai/evolution/live-review/trigger
- [Post] /api/v1/ai/evolution/live-review/complete
- [Post] /api/v1/ai/evolution/live-review/delete
- [Post] /api/v1/ai/evolution/video/compare
- [Post] /api/v1/ai/evolution/stats
- [Post] /api/v1/ai/evolution/roi
- [Post] /api/v1/ai/evolution/score-trend
- [Post] /api/v1/ai/evolution/status
- [Post] /api/v1/ai/evolution/topic/list
- [Post] /api/v1/ai/evolution/topic/save
- [Post] /api/v1/ai/evolution/topic/delete
- [Post] /api/v1/ai/evolution/topic/import
- [Post] /api/v1/ai/evolution/execution/execute
- [Post] /api/v1/ai/evolution/execution/cancel
- [Post] /api/v1/ai/evolution/execution/list
- [Post] /api/v1/ai/evolution/execution/report
- [Post] /api/v1/ai/evolution/pending-deepen/list
- [Post] /api/v1/ai/evolution/pending-deepen/trigger
- [Post] /api/v1/ai/evolution/quality-score/history

## EvolutionReviewController
Base: /api/v1/ai/evolution-review

- [Post] /api/v1/ai/evolution-review/list
- [Post] /api/v1/ai/evolution-review/approve
- [Post] /api/v1/ai/evolution-review/reject
- [Post] /api/v1/ai/evolution-review/revise
- [Post] /api/v1/ai/evolution-review/stats

## EvolveController
Base: /api/v1/ai/admin/evolve

- [Post] /api/v1/ai/admin/evolve/trigger
- [Post] /api/v1/ai/admin/evolve/status
- [Post] /api/v1/ai/admin/evolve/topic/list
- [Post] /api/v1/ai/admin/evolve/topic/save
- [Post] /api/v1/ai/admin/evolve/topic/import-from-file
- [Post] /api/v1/ai/admin/evolve/topic/delete
- [Delete] /api/v1/ai/admin/evolve/topic/{id}
- [Post] /api/v1/ai/admin/evolve/task/list
- [Delete] /api/v1/ai/admin/evolve/task/{id}
- [Post] /api/v1/ai/admin/evolve/report/by-task

## IndustryBrainController
Base: /api/v1/ai/brain

- [Post] /api/v1/ai/brain/knowledge-graph/query
- [Post] /api/v1/ai/brain/knowledge-graph/subgraph-json
- [Post] /api/v1/ai/brain/knowledge-graph/graphrag-context
- [Post] /api/v1/ai/brain/knowledge-graph/relation-suggestions/list
- [Post] /api/v1/ai/brain/knowledge-graph/relation-suggestions/materialize
- [Post] /api/v1/ai/brain/knowledge-graph/relation-suggestions/update-status
- [Post] /api/v1/ai/brain/causal/infer
- [Post] /api/v1/ai/brain/host-personas
- [Post] /api/v1/ai/brain/trends/for-host
- [Post] /api/v1/ai/brain/trends/current
- [Post] /api/v1/ai/brain/user-profile
- [Post] /api/v1/ai/brain/user-profile/{userId}
- [Post] /api/v1/ai/brain/industry/insights
- [Post] /api/v1/ai/brain/account/diagnose
- [Post] /api/v1/ai/brain/strategic/plan
- [Post] /api/v1/ai/brain/risk/warn
- [Post] /api/v1/ai/brain/style-consistency
- [Post] /api/v1/ai/brain/synergy
- [Post] /api/v1/ai/brain/growth-path
- [Post] /api/v1/ai/brain/content-diagnosis
- [Post] /api/v1/ai/brain/product-diagnosis
- [Post] /api/v1/ai/brain/rhythm-diagnosis
- [Post] /api/v1/ai/brain/ip-growth-stage
- [Post] /api/v1/ai/brain/ip-metrics-baseline
- [Post] /api/v1/ai/brain/causal/counterfactual
- [Post] /api/v1/ai/brain/causal/explain-strategy
- [Post] /api/v1/ai/brain/trends/with-lifecycle
- [Post] /api/v1/ai/brain/trends/detect-new
- [Post] /api/v1/ai/brain/account/diagnose-batch
- [Post] /api/v1/ai/brain/risk/warn-batch
- [Post] /api/v1/ai/brain/risk/stats

## KnowledgeBaseController
Base: /api/v1/ai/knowledge-base

- [Post] /api/v1/ai/knowledge-base/create
- [Delete] /api/v1/ai/knowledge-base/{kbId:\d+}
- [Post] /api/v1/ai/knowledge-base/list
- [Post] /api/v1/ai/knowledge-base/{kbId:\d+}/document
- [Delete] /api/v1/ai/knowledge-base/document/{docId:\d+}
- [Post] /api/v1/ai/knowledge-base/{kbId:\d+}/documents
- [Post] /api/v1/ai/knowledge-base/{kbId:\d+}/import-reports
- [Post] /api/v1/ai/knowledge-base/import-requirements
- [Post] /api/v1/ai/knowledge-base/import-from-path
- [Post] /api/v1/ai/knowledge-base/import-from-path-async
- [Post] /api/v1/ai/knowledge-base/import-active-jobs
- [Post] /api/v1/ai/knowledge-base/import-status/{jobId}
- [Post] /api/v1/ai/knowledge-base/feedback
- [Post] /api/v1/ai/knowledge-base/{kbId:\d+}/search
- [Post] /api/v1/ai/knowledge-base/{kbId:\d+}/upload-file
- [Post] /api/v1/ai/knowledge-base/{kbId:\d+}/upload-files
- [Post] /api/v1/ai/knowledge-base/{kbId:\d+}/index-queue/list
- [Post] /api/v1/ai/knowledge-base/{kbId:\d+}/evolution-fitness/list
- [Post] /api/v1/ai/knowledge-base/{kbId:\d+}/dedup-preview
- [Post] /api/v1/ai/knowledge-base/{kbId:\d+}/documents/{docId:\d+}/chunks
- [Post] /api/v1/ai/knowledge-base/import-incremental

## KnowledgeEvolutionController
Base: /api/v1/ai/knowledge-evolution

- [Post] /api/v1/ai/knowledge-evolution/analyze
- [Post] /api/v1/ai/knowledge-evolution/auto-optimize
- [Post] /api/v1/ai/knowledge-evolution/report
- [Post] /api/v1/ai/knowledge-evolution/deduplicate
- [Post] /api/v1/ai/knowledge-evolution/history

## KnowledgeSourceController
Base: /api/v1/ai/admin/knowledge-source

- [Post] /api/v1/ai/admin/knowledge-source/search
- [Post] /api/v1/ai/admin/knowledge-source/get
- [Post] /api/v1/ai/admin/knowledge-source/save
- [Post] /api/v1/ai/admin/knowledge-source/delete

## MediaController
Base: /api/v1/ai/media

- [Post] /api/v1/ai/media/image/text2img
- [Post] /api/v1/ai/media/image/img2img
- [Post] /api/v1/ai/media/image/edit
- [Post] /api/v1/ai/media/image/history
- [Post] /api/v1/ai/media/tts/generate
- [Post] /api/v1/ai/media/tts/voices
- [Post] /api/v1/ai/media/tts/history
- [Post] /api/v1/ai/media/video/trim
- [Post] /api/v1/ai/media/video/merge
- [Post] /api/v1/ai/media/video/subtitle
- [Post] /api/v1/ai/media/video/music
- [Post] /api/v1/ai/media/video/transcode
- [Post] /api/v1/ai/media/video/generate-from-frames
- [Post] /api/v1/ai/media/video/auto-compose

## ModelBenchmarkController
Base: /api/v1/ai/model-benchmark

- [Post] /api/v1/ai/model-benchmark/comparison
- [Post] /api/v1/ai/model-benchmark/best-model
- [Post] /api/v1/ai/model-benchmark/record

## PromptTemplateController
Base: /api/v1/ai/prompt-template

- [Post] /api/v1/ai/prompt-template/list
- [Post] /api/v1/ai/prompt-template/get
- [Post] /api/v1/ai/prompt-template/save
- [Post] /api/v1/ai/prompt-template/delete
- [Post] /api/v1/ai/prompt-template/get-active
- [Post] /api/v1/ai/prompt-template/test-render
- [Post] /api/v1/ai/prompt-template/extract-variables
- [Post] /api/v1/ai/prompt-template/record-usage

## TaskModelConfigController
Base: /api/v1/ai/admin/task-model-config

- [Post] /api/v1/ai/admin/task-model-config/list
- [Post] /api/v1/ai/admin/task-model-config/get
- [Post] /api/v1/ai/admin/task-model-config/save
- [Post] /api/v1/ai/admin/task-model-config/delete

## AttributionController
Base: /api/v1/ai/attribution

- [Post] /api/v1/ai/attribution/trigger
- [Post] /api/v1/ai/attribution/session
- [Post] /api/v1/ai/attribution/summary
- [Post] /api/v1/ai/attribution/get
- [Delete] /api/v1/ai/attribution/session/{sessionId}

## ContentMaterialController
Base: /api/v1/live/material

- [Post] /api/v1/live/material/random
- [Post] /api/v1/live/material/by-persona
- [Post] /api/v1/live/material/categories
- [Post] /api/v1/live/material/prompt
- [Post] /api/v1/live/material/performance-prompt
- [Post] /api/v1/live/material/risk-match

## DanmakuAnalysisController
Base: /api/v1/live/danmaku

- [Post] /api/v1/live/danmaku/analyze
- [Post] /api/v1/live/danmaku/suggest

## EffectivenessScoreController
Base: /api/v1/live/effectiveness

- [Post] /api/v1/live/effectiveness/calculate
- [Post] /api/v1/live/effectiveness/session-ranking
- [Post] /api/v1/live/effectiveness/compare
- [Post] /api/v1/live/effectiveness/ranking
- [Post] /api/v1/live/effectiveness/top-scripts
- [Post] /api/v1/live/effectiveness/recommended-scripts
- [Post] /api/v1/live/effectiveness/emerged-scripts
- [Post] /api/v1/live/effectiveness/script-effectiveness

## LiveAbTestAnalysisController
Base: /api/v1/live/ab-analysis

- [Post] /api/v1/live/ab-analysis/record
- [Post] /api/v1/live/ab-analysis/recommend
- [Post] /api/v1/live/ab-analysis/summary

## LiveAnalysisController
Base: /api/v1/live/analysis

- [Post] /api/v1/live/analysis/generate
- [Post] /api/v1/live/analysis/get
- [Post] /api/v1/live/analysis/review

## LiveApprovalController
Base: /api/v1/live/approval

- [Post] /api/v1/live/approval/submit
- [Post] /api/v1/live/approval/approve
- [Post] /api/v1/live/approval/reject
- [Post] /api/v1/live/approval/history
- [Post] /api/v1/live/approval/pending

## LiveCollaborationPresenceController
Base: /api/v1/live/collaboration

- [Post] /api/v1/live/collaboration/join
- [Post] /api/v1/live/collaboration/leave
- [Post] /api/v1/live/collaboration/viewers

## LiveCompetitiveInsightController
Base: /api/v1/live/competitive-insight

- [Post] /api/v1/live/competitive-insight/search
- [Post] /api/v1/live/competitive-insight/get
- [Post] /api/v1/live/competitive-insight/save
- [Post] /api/v1/live/competitive-insight/delete

## LiveCompetitorScriptController
Base: /api/v1/live/competitor-script

- [Post] /api/v1/live/competitor-script/search
- [Post] /api/v1/live/competitor-script/get
- [Post] /api/v1/live/competitor-script/save
- [Post] /api/v1/live/competitor-script/delete

## LiveDataSyncController
Base: /api/v1/live/data

- [Post] /api/v1/live/data/session
- [Post] /api/v1/live/data/session/with-compare
- [Post] /api/v1/live/data/session/save
- [Post] /api/v1/live/data/session/sync
- [Post] /api/v1/live/data/session/sync-from-douyin
- [Post] /api/v1/live/data/product
- [Post] /api/v1/live/data/history
- [Post] /api/v1/live/data/product/save

## LiveEffectivenessConfigController
Base: /api/v1/live/effectiveness-config

- [Post] /api/v1/live/effectiveness-config/list
- [Post] /api/v1/live/effectiveness-config/save
- [Post] /api/v1/live/effectiveness-config/default
- [Post] /api/v1/live/effectiveness-config/set-default
- [Post] /api/v1/live/effectiveness-config/delete

## LiveGenerationPresetController
Base: /api/v1/live/generation-preset

- [Post] /api/v1/live/generation-preset/list
- [Post] /api/v1/live/generation-preset/save
- [Post] /api/v1/live/generation-preset/delete
- [Post] /api/v1/live/generation-preset/getDefault
- [Post] /api/v1/live/generation-preset/set-default

## LiveGenerationTaskController
Base: /api/v1/live/generation-task

- [Post] /api/v1/live/generation-task/latest
- [Post] /api/v1/live/generation-task/create
- [Post] /api/v1/live/generation-task/update-progress

## LiveMonitorController
Base: /api/v1/live/monitor

- [Post] /api/v1/live/monitor/search
- [Post] /api/v1/live/monitor/save
- [Post] /api/v1/live/monitor/by-session

## LiveMonitorSseController
Base: /api/v1/live/monitor

- [Get] /api/v1/live/monitor/stream/{sessionId}
- [Post] /api/v1/live/monitor/snapshot
- [Post] /api/v1/live/monitor/push

## LivePlatformRuleController
Base: /api/v1/live/platform

- [Post] /api/v1/live/platform/list
- [Post] /api/v1/live/platform/violation-check
- [Post] /api/v1/live/platform/prompt-template

## LiveProductController
Base: /api/v1/live/product

- [Post] /api/v1/live/product/search
- [Post] /api/v1/live/product/get
- [Post] /api/v1/live/product/save
- [Post] /api/v1/live/product/delete
- [Post] /api/v1/live/product/by-session
- [Post] /api/v1/live/product/batch-sort

## LiveRealtimePanelController
Base: /api/v1/live/realtime-panel

- [Post] /api/v1/live/realtime-panel/init
- [Get] /api/v1/live/realtime-panel/stream/{sessionId}
- [Post] /api/v1/live/realtime-panel/next-slot
- [Post] /api/v1/live/realtime-panel/prev-slot
- [Post] /api/v1/live/realtime-panel/jump-slot
- [Post] /api/v1/live/realtime-panel/complete-slot
- [Post] /api/v1/live/realtime-panel/update-data

## LiveRhythmController
Base: /api/v1/live/rhythm

- [Post] /api/v1/live/rhythm/optimize
- [Post] /api/v1/live/rhythm/product-strategy
- [Post] /api/v1/live/rhythm/batch-order
- [Post] /api/v1/live/rhythm/save-rhythm

## LiveScriptApprovalController
Base: /api/v1/live/script-approval

- [Post] /api/v1/live/script-approval/submit
- [Post] /api/v1/live/script-approval/submit-by-session
- [Post] /api/v1/live/script-approval/review
- [Post] /api/v1/live/script-approval/revoke
- [Post] /api/v1/live/script-approval/search
- [Post] /api/v1/live/script-approval/history

## LiveScriptCommentController
Base: /api/v1/live/script-comment

- [Post] /api/v1/live/script-comment/by-script
- [Post] /api/v1/live/script-comment/by-session
- [Post] /api/v1/live/script-comment/save
- [Post] /api/v1/live/script-comment/resolve
- [Post] /api/v1/live/script-comment/delete
- [Post] /api/v1/live/script-comment/unresolved-count
- [Post] /api/v1/live/script-comment/unresolved-by-script

## LiveScriptController
Base: /api/v1/live/script

- [Post] /api/v1/live/script/search
- [Post] /api/v1/live/script/get
- [Post] /api/v1/live/script/save
- [Post] /api/v1/live/script/delete
- [Post] /api/v1/live/script/by-session
- [Post] /api/v1/live/script/effectiveness
- [Post] /api/v1/live/script/executed
- [Post] /api/v1/live/script/save-to-library
- [Post] /api/v1/live/script/save-batch-to-library
- [Post] /api/v1/live/script/export

## LiveScriptCustomTemplateController
Base: /api/v1/live/script-template

- [Post] /api/v1/live/script-template/save-from-session

## LiveScriptGenerationController
Base: /api/v1/live/ai

- [Post] /api/v1/live/ai/generate-opening
- [Post] /api/v1/live/ai/generate-product
- [Post] /api/v1/live/ai/generate-transition
- [Post] /api/v1/live/ai/generate-closing
- [Post] /api/v1/live/ai/generate-slot
- [Post] /api/v1/live/ai/generate-slot-sse
- [Post] /api/v1/live/ai/generate-full
- [Post] /api/v1/live/ai/generate-full-sse
- [Post] /api/v1/live/ai/generate-full-pipelined-sse
- [Post] /api/v1/live/ai/generate-full-async
- [Post] /api/v1/live/ai/generate-full-in-progress
- [Post] /api/v1/live/ai/generation-task/active
- [Post] /api/v1/live/ai/generate-parallel
- [Post] /api/v1/live/ai/generate-skeleton
- [Post] /api/v1/live/ai/generate-skeleton-sse
- [Post] /api/v1/live/ai/generate-product-script
- [Post] /api/v1/live/ai/generate-emotional
- [Post] /api/v1/live/ai/sort-suggest

## LiveScriptNavigationController
Base: /api/v1/live/script-navigation

- [Get] /api/v1/live/script-navigation/current-slot/{sessionId}
- [Get] /api/v1/live/script-navigation/scripts/{sessionId}
- [Get] /api/v1/live/script-navigation/metrics/{sessionId}
- [Post] /api/v1/live/script-navigation/next/{sessionId}
- [Post] /api/v1/live/script-navigation/skip/{sessionId}

## LiveScriptPipelineController
Base: /api/v1/live/pipeline

- [Post] /api/v1/live/pipeline/start
- [Post] /api/v1/live/pipeline/status
- [Post] /api/v1/live/pipeline/cancel

## LiveScriptQualityController
Base: /api/v1/live/ai

- [Post] /api/v1/live/ai/check-violation-enhanced
- [Post] /api/v1/live/ai/check-violation
- [Post] /api/v1/live/ai/save-to-copy-if-passed

## LiveScriptRecommendController
Base: /api/v1/live/ai

- [Post] /api/v1/live/ai/recommend-scripts
- [Post] /api/v1/live/ai/chat-2h-strategy

## LiveScriptRefineController
Base: /api/v1/live/ai

- [Post] /api/v1/live/ai/refine-script
- [Post] /api/v1/live/ai/refine-segment
- [Post] /api/v1/live/ai/refine-script-sse
- [Post] /api/v1/live/ai/chat-for-script
- [Post] /api/v1/live/ai/chat-for-script-sse
- [Post] /api/v1/live/ai/batch-chat-for-script
- [Post] /api/v1/live/ai/suggest-improvement
- [Post] /api/v1/live/ai/check-similarity

## LiveScriptTemplateController
Base: /api/v1/live/template

- [Post] /api/v1/live/template/search
- [Post] /api/v1/live/template/save-from-script
- [Post] /api/v1/live/template/apply
- [Post] /api/v1/live/template/auto-collect

## LiveScriptVersionController
Base: /api/v1/live/script/version

- [Post] /api/v1/live/script/version/search
- [Post] /api/v1/live/script/version/get
- [Post] /api/v1/live/script/version/save
- [Post] /api/v1/live/script/version/delete
- [Post] /api/v1/live/script/version/getByScriptId
- [Post] /api/v1/live/script/version/getLatestVersion
- [Post] /api/v1/live/script/version/diff
- [Post] /api/v1/live/script/version/setRecommended
- [Post] /api/v1/live/script/version/cancelRecommended
- [Post] /api/v1/live/script/version/getRecommendedVersions
- [Post] /api/v1/live/script/version/recommend
- [Post] /api/v1/live/script/version/updateStatus
- [Post] /api/v1/live/script/version/incrementUsageCount
- [Post] /api/v1/live/script/version/like
- [Post] /api/v1/live/script/version/getLatestByScriptIds
- [Post] /api/v1/live/script/version/getUserVersionsBySession
- [Post] /api/v1/live/script/version/createFromExisting

## LiveSessionController
Base: /api/v1/live/session

- [Post] /api/v1/live/session/search
- [Post] /api/v1/live/session/get
- [Post] /api/v1/live/session/save
- [Post] /api/v1/live/session/delete
- [Post] /api/v1/live/session/overview
- [Post] /api/v1/live/session/readiness
- [Post] /api/v1/live/session/status
- [Post] /api/v1/live/session/viewers
- [Post] /api/v1/live/session/likes
- [Post] /api/v1/live/session/trend
- [Post] /api/v1/live/session/clone
- [Post] /api/v1/live/session/export-to-short-video

## LiveSessionTemplateController
Base: /api/v1/live/session-template

- [Post] /api/v1/live/session-template/search
- [Post] /api/v1/live/session-template/get
- [Post] /api/v1/live/session-template/save
- [Post] /api/v1/live/session-template/delete

## LiveStyleController
Base: /api/v1/live/style

- [Post] /api/v1/live/style/recommend

## LiveTemplateController
Base: /api/v1/live/session-template

- [Post] /api/v1/live/session-template/save-from-session

## ScriptQualityController
Base: /api/v1/live/script-quality

- [Post] /api/v1/live/script-quality/evaluate
- [Post] /api/v1/live/script-quality/score
- [Post] /api/v1/live/script-quality/score-session
- [Post] /api/v1/live/script-quality/tts-preview

## EffectivenessScoreController
Base: /api/v1/product/script-effectiveness

- [Post] /api/v1/product/script-effectiveness/ranking
- [Post] /api/v1/product/script-effectiveness/compare
- [Post] /api/v1/product/script-effectiveness/trend
- [Post] /api/v1/product/script-effectiveness/recalculate
- [Post] /api/v1/product/script-effectiveness/style-comparison
- [Post] /api/v1/product/script-effectiveness/analysis
- [Post] /api/v1/product/script-effectiveness/record-snapshot
- [Post] /api/v1/product/script-effectiveness/clear-cache

## ProductController
Base: /api/v1/product

- [Post] /api/v1/product/search
- [Post] /api/v1/product/get
- [Post] /api/v1/product/infer-product-type
- [Post] /api/v1/product/trigger-extract
- [Post] /api/v1/product/extract-from-link
- [Post] /api/v1/product/import-paiping
- [Post] /api/v1/product/save
- [Post] /api/v1/product/delete
- [Post] /api/v1/product/batch-delete
- [Post] /api/v1/product/update-inventory
- [Post] /api/v1/product/publish
- [Post] /api/v1/product/unpublish
- [Post] /api/v1/product/set-featured
- [Post] /api/v1/product/sales-history/search
- [Post] /api/v1/product/sales-history/get
- [Post] /api/v1/product/sales-history/save
- [Post] /api/v1/product/sales-history/total-sales-amount
- [Post] /api/v1/product/sales-history/total-sales-quantity

## ProductReadinessController
Base: /api/v1/product

- [Post] /api/v1/product/readiness

## ProductScriptController
Base: /api/v1/product/script

- [Post] /api/v1/product/script/save
- [Post] /api/v1/product/script/list
- [Post] /api/v1/product/script/search
- [Post] /api/v1/product/script/list-by-type
- [Post] /api/v1/product/script/list-by-style
- [Post] /api/v1/product/script/active-by-style
- [Post] /api/v1/product/script/generate-multi-style
- [Get] /api/v1/product/script/generate-multi-sse
- [Post] /api/v1/product/script/generate-batch-stream
- [Post] /api/v1/product/script/active
- [Put] /api/v1/product/script/activate/{scriptId}
- [Put] /api/v1/product/script/update/{scriptId}
- [Delete] /api/v1/product/script/{scriptId}
- [Post] /api/v1/product/script/get
- [Post] /api/v1/product/script/generate
- [Post] /api/v1/product/script/usage-list
- [Post] /api/v1/product/script/statistics
- [Post] /api/v1/product/script/detail
- [Post] /api/v1/product/script/history
- [Post] /api/v1/product/script/rollback/{historyId}
- [Post] /api/v1/product/script/preview-styles

## ProductScriptVersionController
Base: /api/v1/product/script-version

- [Post] /api/v1/product/script-version/save
- [Post] /api/v1/product/script-version/list
- [Post] /api/v1/product/script-version/search
- [Post] /api/v1/product/script-version/detail/{id}
- [Post] /api/v1/product/script-version/recommend
- [Post] /api/v1/product/script-version/update-effectiveness
- [Post] /api/v1/product/script-version/apply-from-library
- [Post] /api/v1/product/script-version/delete/{id}
- [Post] /api/v1/product/script-version/update-status
- [Post] /api/v1/product/script-version/list-by-product
- [Post] /api/v1/product/script-version/best
- [Post] /api/v1/product/script-version/increase-usage

## ScriptOptimizationController
Base: /api/v1/product/script

- [Post] /api/v1/product/script/analyze
- [Post] /api/v1/product/script/suggestions
- [Post] /api/v1/product/script/regenerate
- [Post] /api/v1/product/script/optimization-history
- [Post] /api/v1/product/script/accept-suggestion
- [Post] /api/v1/product/script/reject-suggestion
- [Post] /api/v1/product/script/apply-regenerated
- [Post] /api/v1/product/script/approve-regenerated

## StylePresetController
Base: /api/v1/product/style-preset

- [Post] /api/v1/product/style-preset/list
- [Post] /api/v1/product/style-preset/list-all
- [Post] /api/v1/product/style-preset/get
- [Post] /api/v1/product/style-preset/save
- [Post] /api/v1/product/style-preset/delete
- [Delete] /api/v1/product/style-preset/{id}
- [Post] /api/v1/product/style-preset/recommend

## StyleRecommendationController
Base: /api/v1/product/style-recommendation

- [Post] /api/v1/product/style-recommendation/recommend-hybrid
- [Post] /api/v1/product/style-recommendation/recommend-ml
- [Post] /api/v1/product/style-recommendation/train
- [Post] /api/v1/product/style-recommendation/train-async
- [Get] /api/v1/product/style-recommendation/metrics
- [Post] /api/v1/product/style-recommendation/record-feedback
- [Post] /api/v1/product/style-recommendation/similar-products
- [Post] /api/v1/product/style-recommendation/compute-features

## SlangDictController
Base: /api/v1/slangdict/entry

- [Post] /api/v1/slangdict/entry/search
- [Post] /api/v1/slangdict/entry/get
- [Post] /api/v1/slangdict/entry/save
- [Post] /api/v1/slangdict/entry/delete
- [Post] /api/v1/slangdict/entry/by-product
- [Post] /api/v1/slangdict/entry/bind-product
- [Post] /api/v1/slangdict/entry/unbind-product
- [Post] /api/v1/slangdict/entry/ai-generate

## OrderController
Base: /api/v1/payment/order

- [Post] /api/v1/payment/order/create
- [Post] /api/v1/payment/order/get
- [Post] /api/v1/payment/order/getByOrderNo
- [Post] /api/v1/payment/order/list
- [Post] /api/v1/payment/order/confirmPayment
- [Post] /api/v1/payment/order/ship
- [Post] /api/v1/payment/order/complete
- [Post] /api/v1/payment/order/cancel

## PaymentController
Base: /api/v1/payment

- [Post] /api/v1/payment/create-order
- [Get] /api/v1/payment/order/{orderNo}
- [Post] /api/v1/payment/callback
- [Post] /api/v1/payment/refund
- [Get] /api/v1/payment/orders
- [Post] /api/v1/payment/reconciliation
- [Get] /api/v1/payment/stats

## RefundController
Base: /api/v1/payment/refund

- [Post] /api/v1/payment/refund/create
- [Post] /api/v1/payment/refund/get
- [Post] /api/v1/payment/refund/listByOrder
- [Post] /api/v1/payment/refund/approve
- [Post] /api/v1/payment/refund/reject
- [Post] /api/v1/payment/refund/complete

## SubscriptionController
Base: /api/v1/payment/subscription

- [Post] /api/v1/payment/subscription/current
- [Post] /api/v1/payment/subscription/upgrade
- [Post] /api/v1/payment/subscription/check-quota
- [Post] /api/v1/payment/subscription/plans

## AuthController
Base: /api/v1/auth

- [Post] /api/v1/auth/captcha
- [Post] /api/v1/auth/login
- [Post] /api/v1/auth/sms/send
- [Post] /api/v1/auth/forgot-password
- [Post] /api/v1/auth/profile
- [Post] /api/v1/auth/profile/update
- [Post] /api/v1/auth/profile/change-password
- [Post] /api/v1/auth/menu/search
- [Post] /api/v1/auth/resource/search
- [Get] /api/v1/auth/oauth/authorize
- [Get] /api/v1/auth/oauth/callback
- [Post] /api/v1/auth/oauth/bindings
- [Post] /api/v1/auth/oauth/bind
- [Post] /api/v1/auth/oauth/unbind
- [Post] /api/v1/auth/logout

## AuthResourceController
Base: /api/v1/auth/resource

- [Post] /api/v1/auth/resource/list
- [Post] /api/v1/auth/resource/tree
- [Post] /api/v1/auth/resource/tree-full
- [Post] /api/v1/auth/resource/get
- [Post] /api/v1/auth/resource/save
- [Post] /api/v1/auth/resource/delete

## AuthRoleController
Base: /api/v1/auth/role

- [Post] /api/v1/auth/role/search
- [Post] /api/v1/auth/role/list
- [Post] /api/v1/auth/role/get
- [Post] /api/v1/auth/role/save
- [Post] /api/v1/auth/role/delete
- [Post] /api/v1/auth/role/resources
- [Post] /api/v1/auth/role/resources/save

## AuthUserController
Base: /api/v1/auth/user

- [Post] /api/v1/auth/user/search
- [Post] /api/v1/auth/user/get
- [Post] /api/v1/auth/user/save
- [Post] /api/v1/auth/user/ban
- [Post] /api/v1/auth/user/delete
- [Post] /api/v1/auth/user/login-logs
- [Post] /api/v1/auth/user/online

## OrganizationController
Base: /api/v1/organization

- [Post] /api/v1/organization/my
- [Post] /api/v1/organization/create
- [Post] /api/v1/organization/update
- [Post] /api/v1/organization/members
- [Post] /api/v1/organization/invite
- [Post] /api/v1/organization/remove
- [Post] /api/v1/organization/invitations
- [Post] /api/v1/organization/invitation/accept
- [Post] /api/v1/organization/invitation/reject
- [Post] /api/v1/organization/search-talents

## ConfigController
Base: /api/v1/config

- [Post] /api/v1/config/list
- [Post] /api/v1/config/get
- [Post] /api/v1/config/save
- [Post] /api/v1/config/delete

## LogController
Base: /api/v1/log

- [Post] /api/v1/log/operation/page
- [Post] /api/v1/log/system/page
- [Post] /api/v1/log/operation/export
- [Post] /api/v1/log/system/export

## AlertController
Base: /api/v1/system

- [Post] /api/v1/system/alert/rule/create
- [Post] /api/v1/system/alert/rule/update
- [Post] /api/v1/system/alert/rule/delete
- [Post] /api/v1/system/alert/rule/get
- [Post] /api/v1/system/alert/rule/list
- [Post] /api/v1/system/alert/rule/enable
- [Post] /api/v1/system/alert/rule/disable
- [Post] /api/v1/system/alert/record/get
- [Post] /api/v1/system/alert/record/list
- [Get] /api/v1/system/dashboard/overview
- [Get] /api/v1/system/dashboard/alerts
- [Get] /api/v1/system/dashboard/performance
- [Get] /api/v1/system/dashboard/logs
- [Get] /api/v1/system/dashboard/traces
- [Get] /api/v1/system/dashboard/health

## ExternalApiConfigController
Base: /api/v1/system/external-api

- [Post] /api/v1/system/external-api/list
- [Post] /api/v1/system/external-api/get
- [Post] /api/v1/system/external-api/save
- [Post] /api/v1/system/external-api/delete
- [Post] /api/v1/system/external-api/health-status
- [Post] /api/v1/system/external-api/by-category

## MetricsController
Base: /api/v1/system/metrics

- [Get] /api/v1/system/metrics/prometheus
- [Get] /api/v1/system/metrics/all
- [Post] /api/v1/system/metrics/get
- [Get] /api/v1/system/metrics/cpu
- [Get] /api/v1/system/metrics/memory
- [Get] /api/v1/system/metrics/disk
- [Get] /api/v1/system/metrics/jvm
- [Get] /api/v1/system/metrics/database

## MonitoringController
Base: /api/v1/monitoring

- [Post] /api/v1/monitoring/metrics/realtime
- [Post] /api/v1/monitoring/metrics/historical
- [Post] /api/v1/monitoring/metrics/trend
- [Post] /api/v1/monitoring/alert-rules/search
- [Post] /api/v1/monitoring/alert-rules/detail
- [Post] /api/v1/monitoring/alert-rules/create
- [Post] /api/v1/monitoring/alert-rules/update
- [Post] /api/v1/monitoring/alert-rules/delete
- [Post] /api/v1/monitoring/alert-rules/enable
- [Post] /api/v1/monitoring/alert-rules/disable
- [Post] /api/v1/monitoringtext/csv;charset=UTF-8
- [Post] /api/v1/monitoring/alert-rules/import
- [Post] /api/v1/monitoring/alerts/search
- [Post] /api/v1/monitoring/alerts/active
- [Post] /api/v1/monitoring/alerts/detail
- [Post] /api/v1/monitoring/alerts/acknowledge
- [Post] /api/v1/monitoring/alerts/resolve
- [Post] /api/v1/monitoring/alerts/close
- [Post] /api/v1/monitoring/alerts/batch-acknowledge
- [Post] /api/v1/monitoring/alerts/statistics
- [Post] /api/v1/monitoring/logs/search
- [Post] /api/v1/monitoring/logs/detail
- [Post] /api/v1/monitoringtext/csv;charset=UTF-8
- [Post] /api/v1/monitoring/logs/statistics
- [Post] /api/v1/monitoring/logs/error-aggregation
- [Post] /api/v1/monitoring/logs/cleanup
- [Post] /api/v1/monitoring/health/status
- [Post] /api/v1/monitoring/health/component
- [Post] /api/v1/monitoring/health/database
- [Post] /api/v1/monitoring/health/cache
- [Post] /api/v1/monitoring/dashboard/data
- [Post] /api/v1/monitoring/statistics
- [Post] /api/v1/monitoring/top-error-endpoints
- [Post] /api/v1/monitoring/top-slow-endpoints
- [Post] /api/v1/monitoring/system/resources
- [Get] /api/v1/monitoring/stream/realtime
- [Get] /api/v1/monitoring/stream/alerts

## SystemController
Base: /api/v1/system

- [Post] /api/v1/system/api-log/list
- [Post] /api/v1/system/api-log/stats
- [Post] /api/v1/system/api-log/get
- [Post] /api/v1/system/sync-log/list
- [Post] /api/v1/system/health
- [Post] /api/v1/system/info

## SystemPerformanceController
Base: /api/v1/system/performance

- [Post] /api/v1/system/performance/metrics/current
- [Post] /api/v1/system/performance/metrics/search
- [Post] /api/v1/system/performance/api/timeseries
- [Post] /api/v1/system/performance/query/analysis
- [Post] /api/v1/system/performance/query/slow
- [Post] /api/v1/system/performance/query/n-plus-one
- [Post] /api/v1/system/performance/index/suggestions
- [Post] /api/v1/system/performance/cache/statistics
- [Post] /api/v1/system/performance/cache/hot-keys
- [Post] /api/v1/system/performance/cache/trend
- [Post] /api/v1/system/performance/cache/clear
- [Post] /api/v1/system/performance/cache/rebuild
- [Post] /api/v1/system/performanceapplication/octet-stream
- [Post] /api/v1/system/performance/benchmark

## TaxonomyController
Base: /api/v1/system/taxonomy

- [Post] /api/v1/system/taxonomy/list
- [Post] /api/v1/system/taxonomy/save
- [Post] /api/v1/system/taxonomy/delete

## BenchmarkAccountController
Base: /api/v1/benchmark/account

- [Post] /api/v1/benchmark/account/list
- [Post] /api/v1/benchmark/account/get
- [Post] /api/v1/benchmark/account/save
- [Post] /api/v1/benchmark/account/delete
- [Post] /api/v1/benchmark/account/search-by-keyword
- [Post] /api/v1/benchmark/account/analyze-by-url

## BenchmarkAnalysisController
Base: /api/v1/benchmark/analysis

- [Post] /api/v1/benchmark/analysis/analyze
- [Post] /api/v1/benchmark/analysis/batch-analyze
- [Post] /api/v1/benchmark/analysis/get-by-video

## BenchmarkContentClassificationController
Base: /api/v1/benchmark/classification

- [Post] /api/v1/benchmark/classification/validate
- [Post] /api/v1/benchmark/classification/auto-classify
- [Post] /api/v1/benchmark/classification/batch-validate
- [Post] /api/v1/benchmark/classification/industries
- [Post] /api/v1/benchmark/classification/scene-types
- [Post] /api/v1/benchmark/classification/script-types
- [Post] /api/v1/benchmark/classification/mark-review
- [Post] /api/v1/benchmark/classification/confirm

## BenchmarkPromptTemplateController
Base: /api/v1/benchmark/prompt-template

- [Post] /api/v1/benchmark/prompt-template/search
- [Post] /api/v1/benchmark/prompt-template/get
- [Post] /api/v1/benchmark/prompt-template/save
- [Post] /api/v1/benchmark/prompt-template/delete
- [Post] /api/v1/benchmark/prompt-template/toggle-active
- [Post] /api/v1/benchmark/prompt-template/get-by-scene
- [Post] /api/v1/benchmark/prompt-template/get-by-industry
- [Post] /api/v1/benchmark/prompt-template/get-by-code

## BenchmarkQualityScriptController
Base: /api/v1/benchmark/quality-script

- [Post] /api/v1/benchmark/quality-script/search
- [Post] /api/v1/benchmark/quality-script/get
- [Post] /api/v1/benchmark/quality-script/save
- [Post] /api/v1/benchmark/quality-script/delete
- [Post] /api/v1/benchmark/quality-script/get-by-video
- [Post] /api/v1/benchmark/quality-script/get-by-analysis
- [Post] /api/v1/benchmark/quality-script/get-high-quality-by-industry
- [Post] /api/v1/benchmark/quality-script/get-high-quality-by-scene
- [Post] /api/v1/benchmark/quality-script/get-top-engagement
- [Post] /api/v1/benchmark/quality-script/get-top-viral
- [Post] /api/v1/benchmark/quality-script/calculate-quality-score

## BenchmarkScriptRecommendationController
Base: /api/v1/benchmark/script-recommendation

- [Post] /api/v1/benchmark/script-recommendation/recommend-by-requirement
- [Post] /api/v1/benchmark/script-recommendation/recommend-by-industry-scene
- [Post] /api/v1/benchmark/script-recommendation/recommend-by-script-type
- [Post] /api/v1/benchmark/script-recommendation/smart-recommend
- [Post] /api/v1/benchmark/script-recommendation/get-popular-scripts
- [Post] /api/v1/benchmark/script-recommendation/get-latest-quality-scripts
- [Post] /api/v1/benchmark/script-recommendation/recommend-improvement-scripts

## BenchmarkScriptSimilarityController
Base: /api/v1/benchmark/script-similarity

- [Post] /api/v1/benchmark/script-similarity/generate-embedding
- [Post] /api/v1/benchmark/script-similarity/batch-generate-embeddings
- [Post] /api/v1/benchmark/script-similarity/index-to-milvus
- [Post] /api/v1/benchmark/script-similarity/batch-index-to-milvus
- [Post] /api/v1/benchmark/script-similarity/find-similar
- [Post] /api/v1/benchmark/script-similarity/find-similar-by-text
- [Post] /api/v1/benchmark/script-similarity/calculate-similarity
- [Post] /api/v1/benchmark/script-similarity/get-unembedded-scripts
- [Post] /api/v1/benchmark/script-similarity/get-unindexed-scripts

## BenchmarkVideoController
Base: /api/v1/benchmark/video

- [Post] /api/v1/benchmark/video/list
- [Post] /api/v1/benchmark/video/get
- [Post] /api/v1/benchmark/video/save
- [Post] /api/v1/benchmark/video/delete
- [Post] /api/v1/benchmark/video/collect

## DouyinCookieController
Base: /api/v1/benchmark/cookie

- [Post] /api/v1/benchmark/cookie/list
- [Post] /api/v1/benchmark/cookie/get
- [Post] /api/v1/benchmark/cookie/save
- [Post] /api/v1/benchmark/cookie/delete
- [Post] /api/v1/benchmark/cookie/validate
- [Post] /api/v1/benchmark/cookie/qr-login/start
- [Post] /api/v1/benchmark/cookie/qr-login/poll
- [Post] /api/v1/benchmark/cookie/qr-login/cancel

## CopyAiController
Base: /api/v1/copy/ai

- [Post] /api/v1/copy/ai/generate

## GlobalSearchController
Base: /api/v1/search

- [Post] /api/v1/search/global

## AccountVideoCollectController
Base: /api/v1/short-video/account-collect

- [Post] /api/v1/short-video/account-collect/start
- [Post] /api/v1/short-video/account-collect/list
- [Post] /api/v1/short-video/account-collect/status
- [Post] /api/v1/short-video/account-collect/cancel
- [Post] /api/v1/short-video/account-collect/retry
- [Post] /api/v1/short-video/account-collect/analyze-selected
- [Post] /api/v1/short-video/account-collect/videos
- [Post] /api/v1/short-video/account-collect/delete
- [Post] /api/v1/short-video/account-collect/status-stream

## AiMusicController
Base: /api/v1/short-video/music

- [Post] /api/v1/short-video/music/generate-bgm
- [Post] /api/v1/short-video/music/providers
- [Post] /api/v1/short-video/music/generate-sfx

## CinematicOpsController
Base: /api/v1/short-video/cinematic

- [Post] /api/v1/short-video/cinematic/scene-camera-mapping/list
- [Post] /api/v1/short-video/cinematic/scene-camera-mapping/save
- [Post] /api/v1/short-video/cinematic/scene-camera-mapping/delete
- [Post] /api/v1/short-video/cinematic/generation-log/summary

## CompetitorMonitorController
Base: /api/v1/short-video/competitor

- [Post] /api/v1/short-video/competitor/add
- [Post] /api/v1/short-video/competitor/list
- [Post] /api/v1/short-video/competitor/analyze
- [Post] /api/v1/short-video/competitor/weekly-report
- [Post] /api/v1/short-video/competitor/remove

## ContrastVideoTemplateController
Base: /api/v1/short-video/contrast-template

- [Post] /api/v1/short-video/contrast-template/shot-template
- [Post] /api/v1/short-video/contrast-template/comedy-config
- [Post] /api/v1/short-video/contrast-template/bgm-strategy
- [Post] /api/v1/short-video/contrast-template/list
- [Post] /api/v1/short-video/contrast-template/preset-template

## CrossModuleContentController
Base: /api/v1/short-video/cross

- [Post] /api/v1/short-video/cross/live-to-video
- [Post] /api/v1/short-video/cross/video-preview-for-live
- [Post] /api/v1/short-video/cross/hot-topic-pool

## DailyContentController
Base: /api/v1/short-video/daily

- [Post] /api/v1/short-video/daily/generate-batch
- [Post] /api/v1/short-video/daily/batch-status
- [Post] /api/v1/short-video/daily/batch-list

## DigitalHumanWebhookController
Base: /api/v1/short-video/webhooks

- [Post] /api/v1/short-video/webhooks/heygen
- [Post] /api/v1/short-video/webhooks/did

## DramaController
Base: /api/v1/short-video/drama

- [Post] /api/v1/short-video/drama/list
- [Post] /api/v1/short-video/drama/get
- [Post] /api/v1/short-video/drama/delete
- [Post] /api/v1/short-video/drama/update
- [Post] /api/v1/short-video/drama/create
- [Post] /api/v1/short-video/drama/episodes
- [Post] /api/v1/short-video/drama/add-episode
- [Post] /api/v1/short-video/drama/link-episode
- [Post] /api/v1/short-video/drama/delete-episode
- [Post] /api/v1/short-video/drama/update-episode
- [Post] /api/v1/short-video/drama/apply-script-to-episodes
- [Post] /api/v1/short-video/drama/characters
- [Post] /api/v1/short-video/drama/update-character
- [Post] /api/v1/short-video/drama/delete-character
- [Post] /api/v1/short-video/drama/add-character
- [Post] /api/v1/short-video/drama/create-project-from-episode
- [Post] /api/v1/short-video/drama/by-project
- [Post] /api/v1/short-video/drama/generate-script

## PersonaViralFusionController
Base: /api/v1/short-video/persona-fusion

- [Post] /api/v1/short-video/persona-fusion/match-personas
- [Post] /api/v1/short-video/persona-fusion/generate-fused-script
- [Post] /api/v1/short-video/persona-fusion/generate-hotspot-fused

## QualityDashboardController
Base: /api/v1/short-video/quality-dashboard

- [Post] /api/v1/short-video/quality-dashboard/overview
- [Post] /api/v1/short-video/quality-dashboard/trend
- [Post] /api/v1/short-video/quality-dashboard/model-ranking
- [Post] /api/v1/short-video/quality-dashboard/camera-ranking
- [Post] /api/v1/short-video/quality-dashboard/ai-reflections

## RemakeTemplateController
Base: /api/v1/short-video/remake-template

- [Post] /api/v1/short-video/remake-template/list
- [Post] /api/v1/short-video/remake-template/save
- [Post] /api/v1/short-video/remake-template/delete
- [Post] /api/v1/short-video/remake-template/create-from-viral
- [Post] /api/v1/short-video/remake-template/generate

## ShortVideoAiController
Base: /api/v1/short-video/ai

- [Post] /api/v1/short-video/ai/check-violation
- [Post] /api/v1/short-video/ai/generate-copy
- [Post] /api/v1/short-video/ai/generate-script
- [Post] /api/v1/short-video/ai/generate-title
- [Post] /api/v1/short-video/ai/generate-plan

## ShortVideoController
Base: /api/v1/short-video

- [Post] /api/v1/short-video/content/search
- [Post] /api/v1/short-video/content/get
- [Post] /api/v1/short-video/content/save
- [Post] /api/v1/short-video/content/delete
- [Post] /api/v1/short-video/content/increment-view-count
- [Post] /api/v1/short-video/content/data-trend
- [Post] /api/v1/short-video/content/calendar
- [Post] /api/v1/short-video/content/calendar-stats
- [Post] /api/v1/short-video/content/publish-time-recommend
- [Post] /api/v1/short-video/category/list
- [Post] /api/v1/short-video/category/get
- [Post] /api/v1/short-video/category/save
- [Post] /api/v1/short-video/category/delete
- [Post] /api/v1/short-video/comment/search
- [Post] /api/v1/short-video/comment/get
- [Post] /api/v1/short-video/comment/save
- [Post] /api/v1/short-video/comment/delete
- [Post] /api/v1/short-video/comment/increment-like-count

## ShortVideoDashboardController
Base: /api/v1/short-video/dashboard

- [Post] /api/v1/short-video/dashboard/stats
- [Post] /api/v1/short-video/dashboard/trend
- [Post] /api/v1/short-video/dashboard/projects
- [Post] /api/v1/short-video/dashboard/cost-breakdown

## ShortVideoDataController
Base: /api/v1/short-video/data

- [Post] /api/v1/short-video/data/collect-hot-videos
- [Post] /api/v1/short-video/data/analyze-viral

## ShortVideoEditController
Base: /api/v1/short-video/edit

- [Post] /api/v1/short-video/edit/auto-compose
- [Post] /api/v1/short-video/edit/generate-subtitles

## ShortVideoFeedbackController
Base: /api/v1/short-video/feedback

- [Post] /api/v1/short-video/feedback/analyze-performance
- [Post] /api/v1/short-video/feedback/reflection-report
- [Post] /api/v1/short-video/feedback/weekly-report

## ShortVideoMaterialController
Base: /api/v1/short-video/material

- [Post] /api/v1/short-video/material/generate-keyframes-stream
- [Post] /api/v1/short-video/material/generate-keyframes
- [Post] /api/v1/short-video/material/generate-voice-batch
- [Post] /api/v1/short-video/material/img2video-batch-stream
- [Post] /api/v1/short-video/material/img2video-batch
- [Post] /api/v1/short-video/material/recommend-camera
- [Post] /api/v1/short-video/material/evaluate-video-quality
- [Post] /api/v1/short-video/material/retry-keyframe

## ShortVideoMaterialLibraryController
Base: /api/v1/short-video/library

- [Post] /api/v1/short-video/library/list
- [Post] /api/v1/short-video/library/delete

## ShortVideoProjectController
Base: /api/v1/short-video/project

- [Post] /api/v1/short-video/project/list
- [Post] /api/v1/short-video/project/get
- [Post] /api/v1/short-video/project/save
- [Post] /api/v1/short-video/project/delete
- [Post] /api/v1/short-video/project/generate-daily
- [Post] /api/v1/short-video/project/daily-list
- [Post] /api/v1/short-video/project/update-shoot-status
- [Post] /api/v1/short-video/project/export-script

## ShortVideoPublishController
Base: /api/v1/short-video/publish

- [Post] /api/v1/short-video/publish/generate-title
- [Post] /api/v1/short-video/publish/ai-review
- [Post] /api/v1/short-video/publish/douyin
- [Post] /api/v1/short-video/publish/publish

## ShortVideoQuickController
Base: /api/v1/short-video/quick

- [Post] /api/v1/short-video/quick/generate

## ShortVideoScriptController
Base: /api/v1/short-video/script

- [Post] /api/v1/short-video/script/list
- [Post] /api/v1/short-video/script/get
- [Post] /api/v1/short-video/script/save
- [Post] /api/v1/short-video/script/delete
- [Post] /api/v1/short-video/script/generate
- [Post] /api/v1/short-video/script/analyze-viral

## ShortVideoSeoController
Base: /api/v1/short-video/seo

- [Post] /api/v1/short-video/seo/suggest-tags
- [Post] /api/v1/short-video/seo/suggest-publish-time
- [Post] /api/v1/short-video/seo/suggest-cover
- [Post] /api/v1/short-video/seo/suggest-ab-titles

## ShortVideoShootingTaskController
Base: /api/v1/short-video/shooting-task

- [Post] /api/v1/short-video/shooting-task/list
- [Post] /api/v1/short-video/shooting-task/get
- [Post] /api/v1/short-video/shooting-task/save
- [Post] /api/v1/short-video/shooting-task/delete

## ShortVideoShotListController
Base: /api/v1/short-video/shot-list

- [Post] /api/v1/short-video/shot-list/list
- [Post] /api/v1/short-video/shot-list/get
- [Post] /api/v1/short-video/shot-list/get-by-script
- [Post] /api/v1/short-video/shot-list/save
- [Post] /api/v1/short-video/shot-list/generate
- [Post] /api/v1/short-video/shot-list/review

## ShortVideoUploadController
Base: /api/v1/short-video/upload

- [Post] /api/v1/short-video/upload/keyframe
- [Post] /api/v1/short-video/upload/keyframes/batch
- [Post] /api/v1/short-video/upload/video
- [Post] /api/v1/short-video/upload/audio
- [Post] /api/v1/short-video/upload/thumbnail
- [Post] /api/v1/short-video/upload/final-video
- [Post] /api/v1/short-video/upload/reference/character
- [Post] /api/v1/short-video/upload/reference/scene
- [Post] /api/v1/short-video/upload/reference/list

## SvAccountController
Base: /api/v1/short-video/account

- [Post] /api/v1/short-video/account/list
- [Post] /api/v1/short-video/account/get
- [Post] /api/v1/short-video/account/update
- [Post] /api/v1/short-video/account/delete
- [Post] /api/v1/short-video/account/videos
- [Post] /api/v1/short-video/account/analytics
- [Post] /api/v1/short-video/account/refresh-stats

## SvContentCalendarController
Base: /api/v1/short-video/content-calendar

- [Post] /api/v1/short-video/content-calendar/list
- [Post] /api/v1/short-video/content-calendar/get
- [Post] /api/v1/short-video/content-calendar/save
- [Post] /api/v1/short-video/content-calendar/delete
- [Post] /api/v1/short-video/content-calendar/date-range
- [Post] /api/v1/short-video/content-calendar/auto-generate

## SvScriptTemplateController
Base: /api/v1/short-video/script-template

- [Post] /api/v1/short-video/script-template/search
- [Post] /api/v1/short-video/script-template/get
- [Post] /api/v1/short-video/script-template/save
- [Post] /api/v1/short-video/script-template/delete
- [Post] /api/v1/short-video/script-template/use-count
- [Post] /api/v1/short-video/script-template/by-scene

## VideoGenerationTaskController
Base: /api/v1/short-video/video-task

- [Post] /api/v1/short-video/video-task/list
- [Post] /api/v1/short-video/video-task/submit
- [Post] /api/v1/short-video/video-task/status
- [Post] /api/v1/short-video/video-task/cancel
- [Post] /api/v1/short-video/video-task/retry

## ViralRemakeController
Base: /api/v1/short-video/viral-remake

- [Post] /api/v1/short-video/viral-remake/recommend
- [Post] /api/v1/short-video/viral-remake/batch-recommend
- [Post] /api/v1/short-video/viral-remake/confirm
- [Post] /api/v1/short-video/viral-remake/generate-script
- [Post] /api/v1/short-video/viral-remake/assign-task
- [Post] /api/v1/short-video/viral-remake/complete

## ViralVideoController
Base: /api/v1/short-video/viral

- [Post] /api/v1/short-video/viral/list
- [Post] /api/v1/short-video/viral/collect
- [Post] /api/v1/short-video/viral/get
- [Post] /api/v1/short-video/viral/delete
- [Post] /api/v1/short-video/viral/analyze
- [Post] /api/v1/short-video/viral/replicate
- [Post] /api/v1/short-video/viral/recommended

## ViralVideoDeepAnalysisController
Base: /api/v1/short-video/viral

- [Post] /api/v1/short-video/viral/deep-analyze
- [Post] /api/v1/short-video/viral/deep-analyze/status
- [Post] /api/v1/short-video/viral/deep-analyze-stream
- [Post] /api/v1/short-video/viral/deep-analyze/batch
- [Post] /api/v1/short-video/viral/deep-analyze/batch/status
- [Post] /api/v1/short-video/viral/deep-analyze/retry-round
- [Post] /api/v1/short-video/viral/extract-transcript
- [Post] /api/v1/short-video/viral/extract-scene-descriptions

## WorkflowController
Base: /api/v1/short-video/workflow

- [Post] /api/v1/short-video/workflow/execute
- [Post] /api/v1/short-video/workflow/status
- [Post] /api/v1/short-video/workflow/ai-assist

## WorkflowTemplateController
Base: /api/v1/short-video/workflow-template

- [Post] /api/v1/short-video/workflow-template/list
- [Post] /api/v1/short-video/workflow-template/get

