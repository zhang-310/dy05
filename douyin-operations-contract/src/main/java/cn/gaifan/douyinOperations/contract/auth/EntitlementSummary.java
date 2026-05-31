package cn.gaifan.douyinOperations.contract.auth;

import java.time.LocalDate;
import java.util.List;

public record EntitlementSummary(
        String entitlementId,
        EntitlementScope scope,
        EntitlementGrantType grantType,
        String tenantId,
        String userId,
        String productCode,
        String featureCode,
        LocalDate validFrom,
        LocalDate validTo,
        List<String> channels
) {
}
