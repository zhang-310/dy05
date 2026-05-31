import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { aiApi, getModelsByTaskCode, listAiModels } from '../ai'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('ai API', () => {
  const mockPost = vi.mocked(request.default.post)
  const mockDelete = vi.mocked(request.default.delete)

  beforeEach(() => {
    mockPost.mockReset()
    mockDelete.mockReset()
  })

  it('kbList posts knowledge base query', async () => {
    mockPost.mockResolvedValue([])
    await aiApi.kbList({ page: 0, rows: 20, name: '护肤' })
    expect(mockPost).toHaveBeenCalledWith('/ai/knowledge-base/list', { page: 0, rows: 20, name: '护肤' })
  })

  it('kbDelete uses delete with id path', async () => {
    mockDelete.mockResolvedValue(undefined)
    await aiApi.kbDelete(12)
    expect(mockDelete).toHaveBeenCalledWith('/ai/knowledge-base/12')
  })

  it('kbSearch posts typed search payload', async () => {
    mockPost.mockResolvedValue([])
    await aiApi.kbSearch(7, { query: '护肤话术', topK: 5, queryRewrite: false })
    expect(mockPost).toHaveBeenCalledWith('/ai/knowledge-base/7/search', {
      query: '护肤话术',
      topK: 5,
      queryRewrite: false,
    })
  })

  it('kbImportFromPath posts sourcePath payload', async () => {
    mockPost.mockResolvedValue({ successCount: 1 })
    await aiApi.kbImportFromPath({
      kbId: 7,
      sourcePath: '/data/knowledge-base-imports/demo.md',
      autoClassify: true,
    })
    expect(mockPost).toHaveBeenCalledWith('/ai/knowledge-base/import-from-path', {
      kbId: 7,
      sourcePath: '/data/knowledge-base-imports/demo.md',
      autoClassify: true,
    })
  })

  it('kbFeedback posts feedback payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await aiApi.kbFeedback({ docId: 9, query: '面膜', rating: 1, searchMode: 'knowledge-search' })
    expect(mockPost).toHaveBeenCalledWith('/ai/knowledge-base/feedback', {
      docId: 9,
      query: '面膜',
      rating: 1,
      searchMode: 'knowledge-search',
    })
  })

  it('normalizes wrapped knowledge base list response', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [{
          kb_id: '7',
          kb_name: '知识库A',
          total_docs: '12',
          status: '1',
          createdAt: '2026-05-22',
          embedding_model: 'bge-m3',
        }],
      },
    })

    await expect(aiApi.kbList({ page: 0, rows: 20 })).resolves.toEqual([
      {
        id: 7,
        kbName: '知识库A',
        description: '',
        totalDocuments: 12,
        status: 1,
        createTime: '2026-05-22',
        totalTokens: undefined,
        embeddingModel: 'bge-m3',
        kbType: undefined,
        updateTime: undefined,
      },
    ])
  })

  it('normalizes wrapped knowledge document, search hit and index queue responses', async () => {
    mockPost
      .mockResolvedValueOnce({
        data: {
          records: [{
            doc_id: '11',
            kb_id: '7',
            fileName: '面膜 FAQ',
            text: '# 补水',
            ext: 'md',
            syncStatus: '1',
            chunks: '3',
            tokens: '220',
            quality_score: '92',
            retryCount: '0',
            createdAt: '2026-05-22',
          }],
          total: '1',
          page: '0',
          rows: '20',
        },
      })
      .mockResolvedValueOnce({
        data: {
          items: [{
            documentId: '11',
            docTitle: '面膜 FAQ',
            chunkText: '补水后锁水',
            similarity: '0.91',
            matchType: 'hybrid',
            chunk_id: '110001',
            tags: ['护肤'],
            explanation: '向量与全文均命中',
          }],
        },
      })
      .mockResolvedValueOnce({
        data: {
          content: [{
            queue_id: '33',
            source_type: 'document',
            source_id: '11',
            target_kb_id: '7',
            state: 'failed',
            retry_count: '2',
            error_msg: '生成嵌入向量失败',
            create_time: '2026-05-22',
            preview: '面膜 FAQ',
          }],
          totalElements: '1',
        },
      })

    await expect(aiApi.docList(7, { page: 0, rows: 20 })).resolves.toMatchObject({
      total: 1,
      list: [
        {
          id: 11,
          kbId: 7,
          title: '面膜 FAQ',
          content: '# 补水',
          fileType: 'md',
          status: 1,
          chunkCount: 3,
          tokenCount: 220,
          qualityHeuristicScore: 92,
          syncRetryCount: 0,
          createTime: '2026-05-22',
        },
      ],
    })
    await expect(aiApi.kbSearch(7, { query: '面膜', topK: 5 })).resolves.toEqual([
      {
        docId: 11,
        title: '面膜 FAQ',
        content: '补水后锁水',
        score: 0.91,
        source: 'hybrid',
        chunkId: 110001,
        labels: ['护肤'],
        explain: '向量与全文均命中',
      },
    ])
    await expect(aiApi.indexQueueList(7, { page: 0, rows: 50 })).resolves.toMatchObject({
      total: 1,
      list: [
        {
          id: 33,
          sourceType: 'document',
          sourceId: 11,
          targetKbId: 7,
          status: 'failed',
          retryCount: 2,
          errorMsg: '生成嵌入向量失败',
          contentPreview: '面膜 FAQ',
        },
      ],
    })
  })

  it('getModelsByTaskCode posts task code payload', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [{
          model_id: '11',
          displayName: 'DeepSeek-R1',
          provider: 'local',
          version: 'deepseek-r1',
          max_tokens: '4096',
          temp: '0.3',
          enabled: '1',
        }],
      },
    })

    await expect(getModelsByTaskCode('live_script')).resolves.toEqual([
      expect.objectContaining({
        id: 11,
        modelName: 'DeepSeek-R1',
        modelProvider: 'local',
        modelVersion: 'deepseek-r1',
        maxTokens: 4096,
        temperature: 0.3,
        status: 1,
      }),
    ])
    expect(mockPost).toHaveBeenCalledWith('/ai/model/list-by-task', { taskCode: 'live_script' })
  })

  it('listAiModels requests fixed page size', async () => {
    mockPost.mockResolvedValue({
      records: [{
        id: '12',
        name: 'Qwen-Max',
        vendor: 'dashscope',
        modelCode: 'qwen-max',
        quotaMax: '10000',
        usedQuota: '256',
      }],
    })

    await expect(listAiModels(2)).resolves.toEqual([
      expect.objectContaining({
        id: 12,
        modelName: 'Qwen-Max',
        modelProvider: 'dashscope',
        modelVersion: 'qwen-max',
        quotaLimit: 10000,
        quotaUsed: 256,
      }),
    ])
    expect(mockPost).toHaveBeenCalledWith('/ai/model/list', { page: 2, rows: 100 })
  })

  it('normalizes wrapped media voice, image history and quality history responses', async () => {
    mockPost
      .mockResolvedValueOnce({
        data: {
          records: [{
            voiceId: 'v1',
            voiceName: '女声',
            lang: 'zh-CN',
            sex: 'female',
            desc: '清晰自然',
          }],
        },
      })
      .mockResolvedValueOnce({
        items: [{
          id: '2',
          url: 'https://cdn.example.com/image-2.png',
          prompt: '包装历史',
          style: 'realistic',
          params: { seed: 7 },
          timestamp: '1770000000000',
        }],
      })
      .mockResolvedValueOnce({
        content: [{
          day: '2026-05-22',
          avgScore: '8.6',
          total: '3',
        }],
      })

    await expect(aiApi.mediaTtsVoices()).resolves.toEqual([{
      id: 'v1',
      name: '女声',
      language: 'zh-CN',
      gender: 'female',
      description: '清晰自然',
    }])
    expect(mockPost).toHaveBeenLastCalledWith('/ai/media/tts/voices', {})

    await expect(aiApi.mediaImageHistory({ page: 0, size: 10 })).resolves.toEqual([{
      id: 2,
      imageUrl: 'https://cdn.example.com/image-2.png',
      prompt: '包装历史',
      type: 'realistic',
      parameters: { seed: 7 },
      createTime: 1770000000000,
    }])
    expect(mockPost).toHaveBeenLastCalledWith('/ai/media/image/history', { page: 0, size: 10 })

    await expect(aiApi.qualityScoreHistory({ kbId: 7, days: 90 })).resolves.toEqual([{
      date: '2026-05-22',
      score: 8.6,
      count: 3,
    }])
    expect(mockPost).toHaveBeenLastCalledWith('/ai/evolution/quality-score/history', { kbId: 7, days: 90 })
  })

  it('evolveTaskTrigger posts evolution task request', async () => {
    mockPost.mockResolvedValue({ taskId: 'abc12def' })
    await aiApi.evolveTaskTrigger({ taskType: 'quality', targetId: 88, priority: 5 })
    expect(mockPost).toHaveBeenCalledWith('/ai/evolution/task/trigger', {
      taskType: 'quality',
      targetId: 88,
      priority: 5,
    })
  })

  it('normalizes wrapped roi, trend and knowledge evolution responses', async () => {
    mockPost
      .mockResolvedValueOnce({
        data: {
          generatedCount: '9',
          avgScore: '72.5',
          completed: '6',
          coveredKbCount: '3',
          total: '10',
          success_rate: '60%',
        },
      })
      .mockResolvedValueOnce({
        data: {
          items: [{ day: '2026-05-22', avgScore: '80', count: '4' }],
        },
      })
      .mockResolvedValueOnce({
        data: {
          analysis_id: 'evol_1',
          period_start: '2026-05-15',
          period_end: '2026-05-22',
          inclusionCandidates: [{
            script_id: '101',
            script_name: '脚本A',
            quality_score: '91',
            usage_count: '3',
            desc: '高分',
          }],
          optimizeCandidates: [{
            script_id: '102',
            script_name: '脚本B',
            quality_score: '55',
            consecutive_low_score: '2',
            reason: '需要优化',
            reference_script_id: '101',
          }],
          duplicateGroups: [{
            master_id: '201',
            title: '主脚本',
            duplicateIds: '[202,203]',
            similarity: '0.94',
            recommendation: '合并',
          }],
          archiveCandidates: [{
            script_id: '301',
            script_name: '脚本C',
            score: '18',
            description: '低分',
            months_since_deprecation: '6',
          }],
          expectedImpact: {
            new_inclusions_count: '1',
            dedup_count: '2',
            improvement_rate: '12.5',
          },
          created_at: '2026-05-22',
          degraded: '0',
        },
      })
      .mockResolvedValueOnce({
        data: {
          execution_id: 'exec_1',
          state: 'COMPLETED',
          results: {
            included: { count: '1', script_ids: ['101'] },
            merged: { count: '2', ids: '[201,202]' },
            archived: { count: '0', script_ids: [] },
          },
          summary: {
            total_processed: '3',
            quality_improvement: '1.5',
            estimated_user_benefit: '收益 3 项',
          },
          executed_at: '2026-05-22',
          degraded: 'false',
        },
      })
      .mockResolvedValueOnce({
        data: {
          report_id: 'report_1',
          period: '2026-05',
          overview: {
            totalScriptsInLibrary: '8',
            new_added_count: '2',
            archived: '1',
            deduplicated_count: '1',
            average_score: '83.2',
          },
          top_scripts: [{
            rank: '1',
            script_id: '101',
            script_name: '脚本A',
            score: '91',
            usage_count: '3',
            adoption_rate: '0.35',
          }],
          styleAnalysis: {
            direct: { count: '2', avgScore: '81', direction: 'UP' },
          },
          recommendations: [{
            code: 'DEDUP',
            description: '整理重复脚本',
            level: 'HIGH',
          }],
          generated_at: '2026-05-22',
        },
      })

    await expect(aiApi.evolveRoi({ kbId: 7 })).resolves.toMatchObject({
      newKnowledge: 9,
      avgScore: 72.5,
      totalRuns: 6,
      coveredDocs: 3,
      totalTasks: 10,
      successRate: 60,
    })
    await expect(aiApi.scoreTrend({ days: 7, kbId: 7 })).resolves.toEqual([
      { date: '2026-05-22', score: 80, count: 4 },
    ])
    await expect(aiApi.knowledgeEvolutionAnalyze({ analysisScope: 'LAST_7_DAYS' })).resolves.toMatchObject({
      analysisId: 'evol_1',
      periodStart: '2026-05-15',
      periodEnd: '2026-05-22',
      readyForInclusion: [
        expect.objectContaining({
          scriptVersionId: 101,
          title: '脚本A',
          score: 91,
          usageCount: 3,
          reason: '高分',
        }),
      ],
      needsOptimization: [
        expect.objectContaining({
          scriptVersionId: 102,
          referenceScriptId: 101,
        }),
      ],
      duplicatesDetected: [
        expect.objectContaining({
          masterScriptId: 201,
          duplicateScriptIds: [202, 203],
        }),
      ],
      readyForArchival: [
        expect.objectContaining({
          scriptVersionId: 301,
          monthsSinceDeprecation: 6,
        }),
      ],
      expectedImpact: {
        newInclusionsCount: 1,
        deduplicationCount: 2,
        improvementRate: 12.5,
      },
      createdAt: '2026-05-22',
      degraded: false,
    })
    await expect(aiApi.knowledgeEvolutionAutoOptimize({
      analysisId: 'evol_1',
      actions: { autoInclude: true, autoMerge: true, autoArchive: false },
      approvalRequired: false,
    })).resolves.toMatchObject({
      executionId: 'exec_1',
      status: 'COMPLETED',
      results: {
        included: { count: 1, scriptIds: [101] },
        merged: { count: 2, scriptIds: [201, 202] },
        archived: { count: 0, scriptIds: [] },
      },
      summary: {
        totalProcessed: 3,
        qualityImprovement: 1.5,
        estimatedUserBenefit: '收益 3 项',
      },
      executedAt: '2026-05-22',
      degraded: false,
    })
    await expect(aiApi.knowledgeEvolutionReport({
      reportType: 'WEEKLY',
      includeTopScripts: true,
      includeStyleAnalysis: true,
    })).resolves.toMatchObject({
      reportId: 'report_1',
      period: '2026-05',
      overview: {
        totalScriptsInLibrary: 8,
        newAddedCount: 2,
        archivedCount: 1,
        deduplicatedCount: 1,
        averageScore: 83.2,
      },
      topScripts: [
        expect.objectContaining({
          rank: 1,
          scriptId: 101,
          title: '脚本A',
          score: 91,
          usageCount: 3,
          adoptionRate: 0.35,
        }),
      ],
      styleAnalysis: {
        direct: {
          count: 2,
          averageScore: 81,
          trend: 'UP',
        },
      },
      recommendations: [
        {
          type: 'DEDUP',
          description: '整理重复脚本',
          priority: 'HIGH',
        },
      ],
      generatedAt: '2026-05-22',
    })
  })

  it('evolutionReviewList posts evolution-review path', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 20 })
    await aiApi.evolutionReviewList({ page: 0, rows: 20, status: 'PENDING' })
    expect(mockPost).toHaveBeenCalledWith('/ai/evolution-review/list', { page: 0, rows: 20, status: 'PENDING' })
  })

  it('evolutionReviewReject sends reason and comment for backend compatibility', async () => {
    mockPost.mockResolvedValue(undefined)
    await aiApi.evolutionReviewReject(2, '内容不符合要求')
    expect(mockPost).toHaveBeenCalledWith('/ai/evolution-review/reject', {
      taskId: 2,
      reason: '内容不符合要求',
      comment: '内容不符合要求',
    })
  })

  it('normalizes evolution review wrapped page response', async () => {
    mockPost.mockResolvedValue({
      records: [{ id: 21, reviewStatus: 'PENDING', contentPreview: '包装审核任务' }],
      totalElements: 1,
      page: 0,
      size: 20,
    })
    const result = await aiApi.evolutionReviewList({ page: 0, rows: 20, status: 'PENDING' })
    expect(result).toMatchObject({
      total: 1,
      pageNum: 0,
      pageSize: 20,
      list: [{ id: 21, reviewStatus: 'PENDING', contentPreview: '包装审核任务' }],
    })
  })

  it('normalizes evolution task wrapped page response', async () => {
    mockPost.mockResolvedValue({
      data: {
        items: [{ id: 31, taskType: 'deepen', status: 1, progress: 50 }],
        total: 1,
      },
    })
    const result = await aiApi.evolveTaskList({ page: 0, rows: 50 })
    expect(result.total).toBe(1)
    expect(result.list).toEqual([{ id: 31, taskType: 'deepen', status: 1, progress: 50 }])
  })

  it('normalizes evolution topic wrapped list response', async () => {
    mockPost.mockResolvedValue({
      rows: [{ id: 41, topicName: '包装主题', priority: 1 }],
    })
    const result = await aiApi.topicList({ scopeGlobal: true })
    expect(mockPost).toHaveBeenCalledWith('/ai/evolution/topic/list', { scopeGlobal: true })
    expect(result).toEqual([{ id: 41, topicName: '包装主题', priority: 1 }])
  })

  it('normalizes evolution review stats aliases', async () => {
    mockPost.mockResolvedValue({
      pending: 2,
      approvedCount: 3,
      rejected: 1,
      revisedCount: 4,
      approvalRate7d: 75,
    })
    const result = await aiApi.evolutionReviewStats()
    expect(result).toMatchObject({
      pending: 2,
      pendingCount: 2,
      approved: 3,
      approvedCount: 3,
      rejected: 1,
      rejectedCount: 1,
      revised: 4,
      revisedCount: 4,
      approvalRate7d: 75,
    })
  })

  it('evolveTaskReport posts admin task report request', async () => {
    mockPost.mockResolvedValue({ id: 9 })
    await aiApi.evolveTaskReport(9)
    expect(mockPost).toHaveBeenCalledWith('/ai/admin/evolve/report/by-task', { taskId: 9 })
  })

  it('kbSourceList posts admin knowledge source search contract', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await aiApi.kbSourceList({ page: 0, rows: 20, keyword: '产品', sourceType: 'local', status: 1 })
    expect(mockPost).toHaveBeenCalledWith('/ai/admin/knowledge-source/search', {
      page: 0,
      rows: 20,
      keyword: '产品',
      sourceType: 'local',
      status: 1,
    })
  })

  it('kbSourceSave posts backend sourcePath payload', async () => {
    mockPost.mockResolvedValue(12)
    await aiApi.kbSourceSave({
      sourceName: '产品资料目录',
      sourcePath: '/data/knowledge/product',
      sourceType: 'local',
      status: 1,
    })
    expect(mockPost).toHaveBeenCalledWith('/ai/admin/knowledge-source/save', {
      sourceName: '产品资料目录',
      sourcePath: '/data/knowledge/product',
      sourceType: 'local',
      status: 1,
    })
  })

  it('kbSourceDelete uses request param id on admin endpoint', async () => {
    mockPost.mockResolvedValue(undefined)
    await aiApi.kbSourceDelete(12)
    expect(mockPost).toHaveBeenCalledWith('/ai/admin/knowledge-source/delete?id=12', {})
  })

  it('infraHealth posts empty object payload', async () => {
    mockPost.mockResolvedValue({ status: 'ok' })
    await aiApi.infraHealth()
    expect(mockPost).toHaveBeenCalledWith('/ai/admin/infra/health', {})
  })

  it('adminCallLogList normalizes wrapped records and numeric aliases', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [{
          log_id: '101',
          owner_id: '7',
          call_type: 'kb_search',
          model_code: 'hybrid',
          input_summary: '包装调用日志',
          prompt_tokens: '10',
          completion_tokens: '20',
          tokens_used: '30',
          latency_ms: '180',
          status: '1',
          is_fallback: '1',
          video_id: '9',
          session_id: '11',
          effect_score: '1.25',
          stage_timings: '{"cache_lookup_ms":2}',
          create_time: '2026-05-22 20:00:00',
        }],
        totalElements: '6',
        page: '2',
        size: '50',
      },
    })

    await expect(aiApi.adminCallLogList({ page: 2, rows: 50 })).resolves.toMatchObject({
      total: 6,
      pageNum: 2,
      pageSize: 50,
      list: [{
        id: 101,
        userId: 7,
        promptTokens: 10,
        completionTokens: 20,
        totalTokens: 30,
        durationMs: 180,
        status: 1,
        isFallback: 1,
        linkedVideoId: 9,
        linkedSessionId: 11,
        effectScore: 1.25,
        inputSummary: '包装调用日志',
      }],
    })
    expect(mockPost).toHaveBeenCalledWith('/ai/admin/call-log/search', { page: 2, rows: 50 })
  })

  it('adminModelsList normalizes wrappers, aliases and numeric fields', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [{
          modelId: '12',
          name: 'Ollama Qwen',
          provider: 'ollama',
          endpoint: 'qwen2.5:7b',
          baseUrl: 'http://host.docker.internal:11434',
          maskedKey: '',
          maxToken: '4096',
          temp: '0.6',
          enabled: '1',
          defaultModel: '1',
          cost: '0.002',
          quotaMax: '10000',
          usedQuota: '9300',
          createdAt: '2026-05-22',
          updatedAt: '2026-05-22 20:00:00',
        }],
      },
    })

    await expect(aiApi.adminModelsList()).resolves.toEqual([
      expect.objectContaining({
        id: 12,
        modelName: 'Ollama Qwen',
        modelProvider: 'ollama',
        modelVersion: 'qwen2.5:7b',
        resolvedBaseUrl: 'http://host.docker.internal:11434',
        maxTokens: 4096,
        temperature: 0.6,
        status: 1,
        isDefault: 1,
        costPer1kTokens: 0.002,
        quotaLimit: 10000,
        quotaUsed: 9300,
      }),
    ])
    expect(mockPost).toHaveBeenCalledWith('/ai/admin/models/list', {})
  })

  it('adminModelsTestConnection normalizes success aliases and numeric tokens', async () => {
    mockPost.mockResolvedValue({ data: { ok: 'true', totalTokens: '8' } })
    await expect(aiApi.adminModelsTestConnection(12)).resolves.toEqual({
      success: true,
      errorMsg: null,
      tokensUsed: 8,
    })
    expect(mockPost).toHaveBeenCalledWith('/ai/admin/models/test-connection', { id: 12 })
  })

  it('adminModelsTestConnection normalizes failure message aliases', async () => {
    mockPost.mockResolvedValue({ connected: false, reason: 'ollama connection refused' })
    await expect(aiApi.adminModelsTestConnection(12)).resolves.toEqual({
      success: false,
      errorMsg: 'ollama connection refused',
      tokensUsed: undefined,
    })
  })

  it('taskModelConfigList normalizes wrapped rows and numeric aliases', async () => {
    mockPost.mockResolvedValue({
      data: {
        rows: [{
          id: '21',
          code: 'script_gen',
          name: '话术生成',
          group: 'content',
          modelId: '10',
          backupModelId: '11',
          backup2ModelId: '12',
          modelName: 'Ollama Qwen',
          backupModelName: 'DeepSeek',
          backup2ModelName: 'GPT-4o',
          timeoutSec: '45',
          retries: '2',
          sort: '9',
          enabled: '1',
          createdAt: '2026-05-22',
        }],
      },
    })

    await expect(aiApi.taskModelConfigList({ taskCode: 'script_gen' })).resolves.toEqual([
      expect.objectContaining({
        id: 21,
        taskCode: 'script_gen',
        taskName: '话术生成',
        taskGroup: 'content',
        primaryModelId: 10,
        fallbackModelId: 11,
        fallback2ModelId: 12,
        primaryModelName: 'Ollama Qwen',
        fallbackModelName: 'DeepSeek',
        fallback2ModelName: 'GPT-4o',
        timeoutSeconds: 45,
        maxRetries: 2,
        sortOrder: 9,
        status: 1,
      }),
    ])
    expect(mockPost).toHaveBeenCalledWith('/ai/admin/task-model-config/list', { taskCode: 'script_gen' })
  })

  it('taskModelConfigGet normalizes wrapped detail row', async () => {
    mockPost.mockResolvedValue({
      data: {
        id: '22',
        taskCode: 'kb_search',
        taskName: '知识库检索',
        primaryModelId: '10',
        timeoutSeconds: '30',
      },
    })

    await expect(aiApi.taskModelConfigGet(22)).resolves.toMatchObject({
      id: 22,
      taskCode: 'kb_search',
      taskName: '知识库检索',
      primaryModelId: 10,
      timeoutSeconds: 30,
    })
    expect(mockPost).toHaveBeenCalledWith('/ai/admin/task-model-config/get', { id: 22 })
  })

  it('normalizes wrapped dashboard and infra responses', async () => {
    mockPost
      .mockResolvedValueOnce({ data: { todayCalls: '12', monthTokens: '3456', successRate: '98.5', avgQualityScore: '30.8' } })
      .mockResolvedValueOnce({ data: { records: [{ date: '2026-05-22', count: '7' }] } })
      .mockResolvedValueOnce({ data: { items: [{ component: 'redis', ok: true, message: null }] } })
      .mockResolvedValueOnce({ data: { hit: '22', miss: '78', total: '100', hitRate: '22', keyCount: '18' } })
      .mockResolvedValueOnce({
        data: {
          dbSize: '18',
          kbCacheTtlSeconds: '21600',
          redisStats: { globalHitRate: '22.28', keyspaceHits: '2228', keyspaceMisses: '7772' },
          businessStats: { hitRate: '22', hit: '22', total: '100', keyCount: '18', scope: 'stats:kb:cache:*' },
          scan: { scanned: '18', prefixCounts: { 'cache:kb': '18' }, ttlBuckets: { lt1d: '18' } },
          suggestions: ['检查热点 key 或增加缓存 TTL'],
        },
      })
      .mockResolvedValueOnce({ data: { total_requests: '10', qps: '0.5', p95_ms: '220', p99_latency_ms: '480' } })
      .mockResolvedValueOnce({ data: { grafanaUrl: 'http://grafana.local/d/ai' } })
      .mockResolvedValueOnce({ data: { updated: '15', completed: true } })

    await expect(aiApi.dashboardStats()).resolves.toMatchObject({
      todayCalls: 12,
      monthTokens: 3456,
      successRate: 98.5,
      avgQualityScore: 30.8,
    })
    await expect(aiApi.callVolumeTrend({ days: 1 })).resolves.toEqual([{ date: '2026-05-22', count: '7' }])
    await expect(aiApi.infraHealth()).resolves.toEqual([{ component: 'redis', ok: true, message: null }])
    await expect(aiApi.cacheStats()).resolves.toMatchObject({ hitRate: 22, keyCount: 18 })
    await expect(aiApi.cacheDiagnostics()).resolves.toMatchObject({
      dbSize: 18,
      redisStats: { globalHitRate: 22.28, keyspaceHits: 2228 },
      businessStats: { hitRate: 22, scope: 'stats:kb:cache:*' },
      scan: { scanned: 18, prefixCounts: { 'cache:kb': 18 }, ttlBuckets: { lt1d: 18 } },
      suggestions: ['检查热点 key 或增加缓存 TTL'],
    })
    await expect(aiApi.searchStats()).resolves.toMatchObject({ totalRequests: 10, qps: 0.5, p95Ms: 220, p99Ms: 480 })
    await expect(aiApi.monitoringConfig()).resolves.toEqual({ grafanaUrl: 'http://grafana.local/d/ai' })
    await expect(aiApi.dashboardKbQualityRescan({ limit: 100 })).resolves.toEqual({ updated: 15, completed: true })
  })

  it('quotaUpdate posts admin quota update payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await aiApi.quotaUpdate({ userId: 7, dailyMax: 500 })
    expect(mockPost).toHaveBeenCalledWith('/ai/admin/quota/update', { userId: 7, dailyMax: 500 })
  })

  it('normalizes quota overview and history wrappers', async () => {
    mockPost
      .mockResolvedValueOnce({
        data: {
          dailyMax: '1000',
          items: [
            { feature: 'overall', usedCount: '750', maxCount: '1000', unit: '次', period: '今日' },
            { feature: 'script_gen', used: '80', limit: '100' },
          ],
        },
      })
      .mockResolvedValueOnce({
        data: {
          records: [{ id: '1', feature: 'script_gen', usedCount: '80', maxCount: '100', period: 'daily', createTime: '2026-05-22' }],
          totalElements: '5',
          page: '0',
          size: '20',
        },
      })

    await expect(aiApi.quotaGet()).resolves.toMatchObject({
      dailyMax: 1000,
      items: [
        { feature: 'overall', used: 750, limit: 1000 },
        { feature: 'script_gen', used: 80, limit: 100 },
      ],
    })
    await expect(aiApi.quotaHistory({ page: 0, rows: 20, feature: 'script_gen' })).resolves.toMatchObject({
      total: 5,
      list: [{ id: 1, feature: 'script_gen', used: 80, limit: 100 }],
    })
  })

  it('modelBenchmarkBestModel posts task code request', async () => {
    mockPost.mockResolvedValue({ modelId: 3 })
    await aiApi.modelBenchmarkBestModel('copy_generate')
    expect(mockPost).toHaveBeenCalledWith('/ai/model-benchmark/best-model', {
      taskCode: 'copy_generate',
      priority: 'latency',
    })
  })

  it('normalizes model benchmark comparison wrappers and numeric aliases', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [{
          model: '9',
          name: 'Ollama Qwen',
          code: 'script_gen',
          avgLatency: '850',
          success_rate: '92%',
          tokens: '320.5',
          calls: '12',
        }],
      },
    })

    await expect(aiApi.modelBenchmarkComparison({ taskCode: 'script_gen' })).resolves.toEqual([
      expect.objectContaining({
        modelId: 9,
        modelName: 'Ollama Qwen',
        taskCode: 'script_gen',
        avgLatencyMs: 850,
        successRate: 0.92,
        avgTokens: 320.5,
        totalCalls: 12,
      }),
    ])
    expect(mockPost).toHaveBeenCalledWith('/ai/model-benchmark/comparison', { taskCode: 'script_gen' })
  })

  it('normalizes model benchmark best-model wrappers and aliases', async () => {
    mockPost.mockResolvedValue({
      data: {
        item: 'ignored',
        model: '0',
        name: '',
        code: 'script_gen',
        strategy: 'success',
      },
    })

    await expect(aiApi.modelBenchmarkBestModel('script_gen', 'success')).resolves.toEqual(
      expect.objectContaining({
        modelId: 0,
        modelName: '',
        taskCode: 'script_gen',
        priority: 'success',
      }),
    )
    expect(mockPost).toHaveBeenCalledWith('/ai/model-benchmark/best-model', {
      taskCode: 'script_gen',
      priority: 'success',
    })
  })

  it('promptTemplateList maps legacy templateType to backend templateCode', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await aiApi.promptTemplateList({ templateType: 'script_generate', rows: 50, isActive: 1 })
    expect(mockPost).toHaveBeenCalledWith('/ai/prompt-template/list', {
      templateType: undefined,
      templateCode: 'script_generate',
      rows: 50,
      isActive: true,
    })
  })

  it('normalizes wrapped prompt template page and render responses', async () => {
    mockPost
      .mockResolvedValueOnce({
        data: {
          records: [{
            id: '11',
            templateType: 'agent_chat',
            templateName: '包装模板',
            content: '你好 {{name}}',
            is_active: '1',
            is_default: '0',
            usage_count: '7',
            max_tokens: '2048',
            temp: '0.6',
            owner_id: '0',
            createTime: '2026-05-22',
          }],
          totalElements: 1,
        },
      })
      .mockResolvedValueOnce({ data: { result: '你好 张三', extractedVariables: ['name'], missing: [] } })
      .mockResolvedValueOnce({ data: { items: ['name', 'style'] } })

    await expect(aiApi.promptTemplateList({ page: 0, rows: 20 })).resolves.toMatchObject({
      total: 1,
      list: [expect.objectContaining({
        id: 11,
        templateCode: 'agent_chat',
        templateName: '包装模板',
        templateContent: '你好 {{name}}',
        isActive: 1,
        isDefault: 0,
        usageCount: 7,
        maxTokens: 2048,
        temperature: 0.6,
        ownerId: 0,
      })],
    })
    await expect(aiApi.promptTemplateTestRender({ templateContent: '你好 {{name}}' })).resolves.toEqual({
      rendered: '你好 张三',
      variables: ['name'],
      missingVariables: [],
    })
    await expect(aiApi.promptTemplateExtractVariables('你好 {{name}}')).resolves.toEqual(['name', 'style'])
  })

  it('promptTemplateSave maps numeric active flags to booleans', async () => {
    mockPost.mockResolvedValue({ id: 9 })
    await aiApi.promptTemplateSave({
      templateType: 'agent_chat',
      templateName: '客服模板',
      templateContent: '你好 {{name}}',
      isActive: 1,
      isDefault: 0,
    })
    expect(mockPost).toHaveBeenCalledWith('/ai/prompt-template/save', {
      templateType: undefined,
      templateCode: 'agent_chat',
      templateName: '客服模板',
      templateContent: '你好 {{name}}',
      isActive: true,
      isDefault: false,
    })
  })

  it('promptTemplateGetActive posts templateCode and variantName', async () => {
    mockPost.mockResolvedValue({ id: 1 })
    await aiApi.promptTemplateGetActive('agent_chat', 'default')
    expect(mockPost).toHaveBeenCalledWith('/ai/prompt-template/get-active', {
      templateCode: 'agent_chat',
      variantName: 'default',
    })
  })

  it('promptTemplateExtractVariables posts template content', async () => {
    mockPost.mockResolvedValue(['name'])
    await aiApi.promptTemplateExtractVariables('你好 {{name}}')
    expect(mockPost).toHaveBeenCalledWith('/ai/prompt-template/extract-variables', {
      templateContent: '你好 {{name}}',
    })
  })

  it('promptTemplateRecordUsage posts template id', async () => {
    mockPost.mockResolvedValue(undefined)
    await aiApi.promptTemplateRecordUsage(11)
    expect(mockPost).toHaveBeenCalledWith('/ai/prompt-template/record-usage', { id: 11 })
  })
})
