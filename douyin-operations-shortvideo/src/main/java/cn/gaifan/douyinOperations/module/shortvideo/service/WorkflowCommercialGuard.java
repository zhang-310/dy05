package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationActionRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditReserveRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationResult;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialCreditHelper;
import cn.gaifan.douyinOperations.module.platform.identity.CommercialIdentityBridge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流商业化：入口 reserve、成功 commit、失败 release。
 */
@Component
public class WorkflowCommercialGuard {

    @Autowired(required = false)
    private CommercialCreditHelper commercialCreditHelper;

    private final Map<String, String> taskReservations = new ConcurrentHashMap<>();
    private final Map<String, String> taskTraces = new ConcurrentHashMap<>();

    public void reserveForTask(String taskId, Long projectId, Long userId) {
        if (commercialCreditHelper == null || !commercialCreditHelper.isEnforced()) {
            return;
        }
        IdentityContext ctx = RequestIdentityHolder.current();
        String tenantId = resolveTenant(ctx);
        String gfUser = CommercialIdentityBridge.resolveUserId(ctx);
        if (gfUser == null || gfUser.isBlank()) {
            gfUser = userId != null ? String.valueOf(userId) : "1";
        }
        String traceId = "wf-reserve-" + taskId;
        CreditReservationResult reserved = commercialCreditHelper.reserve(new CreditReserveRequest(
                tenantId,
                gfUser,
                null,
                ProductCode.SHORTVIDEO_MAKER,
                FeatureCode.SHORTVIDEO_SCRIPT_GENERATE,
                ctx != null && ctx.channel() != null ? ctx.channel() : "WEB",
                BigDecimal.ONE,
                "standard",
                traceId,
                "workflow-" + taskId,
                "工作流积分冻结 projectId=" + projectId
        ));
        if (reserved != null && reserved.reservationId() != null) {
            taskReservations.put(taskId, reserved.reservationId());
            taskTraces.put(taskId, traceId);
        }
    }

    public void commitTask(String taskId, Long projectId) {
        if (commercialCreditHelper == null) {
            return;
        }
        String reservationId = taskReservations.remove(taskId);
        if (reservationId == null) {
            return;
        }
        String traceId = taskTraces.getOrDefault(taskId, "wf-commit-" + taskId);
        IdentityContext ctx = RequestIdentityHolder.current();
        String tenantId = resolveTenant(ctx);
        commercialCreditHelper.commit(reservationId, new CreditReservationActionRequest(
                tenantId,
                CommercialIdentityBridge.resolveUserId(ctx),
                null,
                ctx != null && ctx.channel() != null ? ctx.channel() : "WEB",
                traceId,
                "工作流 commit projectId=" + projectId
        ));
        taskTraces.remove(taskId);
    }

    public void releaseTask(String taskId, Long projectId, String reason) {
        if (commercialCreditHelper == null) {
            return;
        }
        String reservationId = taskReservations.remove(taskId);
        if (reservationId == null) {
            return;
        }
        String traceId = taskTraces.getOrDefault(taskId, "wf-release-" + taskId);
        IdentityContext ctx = RequestIdentityHolder.current();
        String tenantId = resolveTenant(ctx);
        commercialCreditHelper.release(reservationId, new CreditReservationActionRequest(
                tenantId,
                CommercialIdentityBridge.resolveUserId(ctx),
                null,
                ctx != null && ctx.channel() != null ? ctx.channel() : "WEB",
                traceId,
                reason != null ? reason : "工作流 release projectId=" + projectId
        ));
        taskTraces.remove(taskId);
    }

    private static String resolveTenant(IdentityContext ctx) {
        String tenantId = CommercialIdentityBridge.resolveTenantId(ctx);
        if (tenantId == null || tenantId.isBlank() || "default".equals(tenantId)) {
            return "demo-tenant";
        }
        return tenantId;
    }
}
