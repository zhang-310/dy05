package cn.gaifan.douyinOperations.module.payment.service;

import cn.gaifan.douyinOperations.common.tenant.TenantOrgResolutionHelper;
import cn.gaifan.douyinOperations.module.payment.entity.Subscription;
import cn.gaifan.douyinOperations.module.payment.entity.UsageRecord;
import cn.gaifan.douyinOperations.module.payment.repository.UsageRecordRepository;
import cn.gaifan.douyinOperations.module.payment.service.impl.UsageQuotaServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsageQuotaServiceImpl 单元测试")
class UsageQuotaServiceImplTest {

    @Mock
    private UsageRecordRepository usageRecordRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private TenantOrgResolutionHelper tenantOrgResolutionHelper;

    @InjectMocks
    private UsageQuotaServiceImpl usageQuotaService;

    @Test
    @DisplayName("recordUsage 应写入用量记录")
    void recordUsage_shouldPersistUsageRecord() {
        when(tenantOrgResolutionHelper.organizationIdForUser(100L)).thenReturn(200L);

        usageQuotaService.recordUsage(100L, "aiGenerations", 3);

        verify(usageRecordRepository).save(org.mockito.ArgumentMatchers.argThat((UsageRecord record) ->
                record.getUserId().equals(100L)
                        && record.getOrgId().equals(200L)
                        && "aiGenerations".equals(record.getMetric())
                        && record.getDelta() == 3
        ));
    }

    @Test
    @DisplayName("isWithinQuota 遇到无限额度时不应继续查询当月用量")
    void isWithinQuota_shouldSkipUsageLookupForUnlimitedPlan() {
        Subscription subscription = new Subscription();
        subscription.setMaxAiGenerations(-1);
        when(subscriptionService.getActiveSubscription(100L)).thenReturn(subscription);

        assertTrue(usageQuotaService.isWithinQuota(100L, "aiGenerations"));
        verify(usageRecordRepository, never()).sumUsageSince(anyLong(), anyString(), any(Timestamp.class));
    }

    @Test
    @DisplayName("isWithinQuota 当免费额度耗尽时应返回 false")
    void isWithinQuota_shouldReturnFalseWhenFreeQuotaIsExhausted() {
        when(subscriptionService.getActiveSubscription(100L)).thenReturn(null);
        when(usageRecordRepository.sumUsageSince(eq(100L), eq("aiGenerations"), any(Timestamp.class))).thenReturn(50);

        assertFalse(usageQuotaService.isWithinQuota(100L, "aiGenerations"));
    }
}
