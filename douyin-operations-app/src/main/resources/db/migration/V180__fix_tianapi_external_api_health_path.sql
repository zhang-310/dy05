-- TianAPI root path returns 404. Probe a real public API path so the health
-- page reflects provider reachability instead of a missing landing page.
UPDATE external_api_config
SET extra_config = COALESCE(extra_config, '{}'::jsonb)
        || '{"healthPath":"/bulletin/index","method":"GET","apiKeyEnv":"TIANAPI_API_KEY","successJsonCode":200}'::jsonb,
    update_time = CURRENT_TIMESTAMP
WHERE provider_code = 'tianapi'
  AND deleted = 0;
