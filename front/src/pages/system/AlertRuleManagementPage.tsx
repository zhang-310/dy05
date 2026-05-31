import AlertRulesPage from './AlertRulesPage'

export default function AlertRuleManagementPage() {
  return (
    <div
      data-testid="alert-rule-management-wrapper"
      data-contract-scope="legacy-system-alert-rule-management-wrapper"
      data-child-page="AlertRulesPage"
      data-ready-endpoints="/monitoring/alert-rules/search|/monitoring/alert-rules/create|/monitoring/alert-rules/update|/monitoring/alert-rules/delete|/monitoring/alert-rules/enable|/monitoring/alert-rules/disable"
      data-unsupported-actions="legacy-system-alert-rule-endpoints|local-rule-fallback"
      data-no-legacy-system-alert-rule-endpoints="true"
      data-no-local-rule-fallback="true"
    >
      <AlertRulesPage />
    </div>
  )
}
