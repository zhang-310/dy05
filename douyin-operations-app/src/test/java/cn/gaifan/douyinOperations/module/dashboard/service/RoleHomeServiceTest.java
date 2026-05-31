package cn.gaifan.douyinOperations.module.dashboard.service;

import cn.gaifan.douyinOperations.common.tenant.TenantOrgResolutionHelper;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.module.ai.service.AiDashboardService;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.service.LiveApprovalService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleHomeServiceTest {

    @Mock
    private DashboardService dashboardService;
    @Mock
    private DataScopeResolver dataScopeResolver;
    @Mock
    private TenantOrgResolutionHelper tenantOrgResolutionHelper;
    @Mock
    private ShortVideoDashboardService shortVideoDashboardService;
    @Mock
    private AiDashboardService aiDashboardService;
    @Mock
    private LiveApprovalService liveApprovalService;

    private RoleHomeService service;

    @BeforeEach
    void setUp() {
        service = new RoleHomeService();
        ReflectionTestUtils.setField(service, "dashboardService", dashboardService);
        ReflectionTestUtils.setField(service, "dataScopeResolver", dataScopeResolver);
        ReflectionTestUtils.setField(service, "tenantOrgResolutionHelper", tenantOrgResolutionHelper);
        ReflectionTestUtils.setField(service, "shortVideoDashboardService", shortVideoDashboardService);
        ReflectionTestUtils.setField(service, "aiDashboardService", aiDashboardService);
        ReflectionTestUtils.setField(service, "liveApprovalService", liveApprovalService);
    }

    @Test
    void adminHome_marksBoundaryAsUnrestrictedOnlyForAdminScope() {
        when(dataScopeResolver.getVisibleUserIds(1L, "admin")).thenReturn(null);
        when(dashboardService.getAdminStats()).thenReturn(Map.of("totalUsers", 12));
        when(aiDashboardService.getDashboardStats()).thenReturn(Map.of("kbDocumentCount", 7));

        Map<String, Object> result = service.buildHome("admin", 1L, "admin", null);

        @SuppressWarnings("unchecked")
        Map<String, Object> boundary = (Map<String, Object>) result.get("boundary");
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) result.get("metrics");
        assertThat(boundary.get("unrestricted")).isEqualTo(true);
        assertThat(boundary.get("visibleOwnerIds")).isNull();
        assertThat(metrics.get("totalUsers")).isEqualTo(12);
        assertThat(metrics.get("kbDocumentCount")).isEqualTo(7);
        assertThat(result.get("businessChains")).asList().hasSize(3);
        @SuppressWarnings("unchecked")
        Map<String, Object> guard = (Map<String, Object>) result.get("knowledgeGuard");
        assertThat(guard.get("officialReferenceRequired")).isEqualTo(true);
    }

    @Test
    void orgHome_usesRequestOrgIdAndInstitutionVisibleOwners() {
        when(dataScopeResolver.getVisibleUserIds(10L, "institution")).thenReturn(List.of(10L, 21L, 22L));
        when(dashboardService.getOrgStats(10L)).thenReturn(Map.of("totalLiveSessions", 4));
        when(liveApprovalService.getPendingApprovals(10L)).thenReturn(List.of(new LiveSession(), new LiveSession()));

        Map<String, Object> result = service.buildHome("org", 10L, "institution", 88L);

        @SuppressWarnings("unchecked")
        Map<String, Object> boundary = (Map<String, Object>) result.get("boundary");
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) result.get("metrics");
        assertThat(result.get("organizationId")).isEqualTo(88L);
        assertThat(boundary.get("unrestricted")).isEqualTo(false);
        assertThat(boundary.get("visibleOwnerIds")).isEqualTo(List.of(10L, 21L, 22L));
        assertThat(metrics.get("pendingApprovalCount")).isEqualTo(2);
        @SuppressWarnings("unchecked")
        Map<String, Object> guard = (Map<String, Object>) result.get("knowledgeGuard");
        assertThat(guard.get("blockWithoutOfficialReference")).isEqualTo(true);
    }

    @Test
    void talentHome_keepsOwnerBoundaryAndRecentProject() {
        when(dataScopeResolver.getVisibleUserIds(33L, "talent")).thenReturn(List.of(33L));
        when(dashboardService.getOrgStats(33L)).thenReturn(Map.of("totalLiveSessions", 1));
        when(shortVideoDashboardService.getStats(33L)).thenReturn(Map.of("totalVideoCount", 3));
        when(shortVideoDashboardService.getProjectsWithProgress(33L, null, 0, 5))
                .thenReturn(List.of(Map.of("title", "达人爆款复盘")));

        Map<String, Object> result = service.buildHome("talent", 33L, "talent", null);

        @SuppressWarnings("unchecked")
        Map<String, Object> boundary = (Map<String, Object>) result.get("boundary");
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) result.get("metrics");
        assertThat(boundary.get("visibleOwnerIds")).isEqualTo(List.of(33L));
        assertThat(boundary.get("requiresOwnerFilter")).isEqualTo(true);
        assertThat(metrics.get("totalVideoCount")).isEqualTo(3);
        assertThat(metrics.get("latestProjectTitle")).isEqualTo("达人爆款复盘");
        assertThat(result.get("learningLoops")).asList()
                .anySatisfy(loop -> assertThat(loop.toString()).contains("viral-pattern-kb"));
    }

    @Test
    void userHome_onlyExposesPersonalShortvideoChainsAndBlocksWithoutOfficialReferences() {
        when(dataScopeResolver.getVisibleUserIds(44L, "user")).thenReturn(List.of(44L));
        when(shortVideoDashboardService.getStats(44L)).thenReturn(Map.of("totalVideoCount", 2));
        when(shortVideoDashboardService.getProjectsWithProgress(44L, null, 0, 5))
                .thenReturn(List.of(Map.of("title", "个人口播草稿")));

        Map<String, Object> result = service.buildHome("user", 44L, "user", null);

        @SuppressWarnings("unchecked")
        Map<String, Object> boundary = (Map<String, Object>) result.get("boundary");
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) result.get("metrics");
        @SuppressWarnings("unchecked")
        Map<String, Object> guard = (Map<String, Object>) result.get("knowledgeGuard");
        assertThat(boundary.get("visibleOwnerIds")).isEqualTo(List.of(44L));
        assertThat(metrics.get("latestProjectTitle")).isEqualTo("个人口播草稿");
        assertThat(guard.get("blockWithoutOfficialReference")).isEqualTo(true);
        assertThat(result.get("businessChains")).asList()
                .anySatisfy(chain -> assertThat(chain.toString()).contains("personal-shortvideo"));
        assertThat(result.get("businessChains")).asList()
                .noneSatisfy(chain -> assertThat(chain.toString()).contains("/admin/"));
    }
}
