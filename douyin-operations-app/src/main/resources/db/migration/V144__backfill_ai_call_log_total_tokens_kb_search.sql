-- 回填知识检索类调用未写入的 total_tokens，与 AiCallLogServiceImpl 估算口径一致（仪表盘「本月 Token」）
-- 仅处理 kb_search；其它类型仍依赖运行时日志或另行治理

UPDATE ai_call_log
SET total_tokens = LEAST(
        500000,
        GREATEST(1, COALESCE(LENGTH(input_summary), 0) / 2)
            + CASE
                  WHEN COALESCE(output_length, 0) > 0
                      THEN LEAST(output_length * 150, 50000)
                  ELSE 0
              END
    )
WHERE status = 1
  AND call_type = 'kb_search'
  AND total_tokens IS NULL
  AND (prompt_tokens IS NULL OR completion_tokens IS NULL);
