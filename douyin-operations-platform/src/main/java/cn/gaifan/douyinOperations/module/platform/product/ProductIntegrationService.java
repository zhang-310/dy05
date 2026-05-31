package cn.gaifan.douyinOperations.module.platform.product;

import cn.gaifan.douyinOperations.common.id.Ids;
import cn.gaifan.douyinOperations.contract.auth.EntitlementCheckRequest;
import cn.gaifan.douyinOperations.contract.auth.CommercialEntitlementDecision;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeResult;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationResult;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationBillingMode;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationCheckRequest;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationDecision;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationInvocationRequest;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationInvocationResult;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationInvocationSummary;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationRuleSummary;
import cn.gaifan.douyinOperations.module.platform.auth.PlatformEntitlementService;
import cn.gaifan.douyinOperations.module.platform.credit.CreditLedgerService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 产品间能力调用治理服务。
 *
 * <p>该服务只负责“能不能调用”和“调用按哪个产品计费”的平台决策，
 * 不执行真实业务动作。业务产品要调用其他产品能力时，必须先经过这里，
 * 再进入目标产品 API、AI 网关、MCP 网关或工作流任务。</p>
 */
@Service
public class ProductIntegrationService {
    private final PlatformEntitlementService entitlementService;
    private final CreditLedgerService creditLedgerService;
    private final JdbcTemplate jdbcTemplate;

    public ProductIntegrationService(
            PlatformEntitlementService entitlementService,
            CreditLedgerService creditLedgerService,
            JdbcTemplate jdbcTemplate
    ) {
        this.entitlementService = entitlementService;
        this.creditLedgerService = creditLedgerService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 返回当前平台允许的产品互调矩阵。
     *
     * <p>每条规则都把发起产品、目标产品、目标功能、计费归属和审计要求显式写出。
     * 这样产品可以联动，但不会在商业模型上互相吞并。</p>
     */
    public List<ProductIntegrationRuleSummary> listRules() {
        return List.of(
                rule("douyin-to-video-insight", ProductCode.DOUYIN_OPS, ProductCode.VIDEO_INSIGHT,
                        "video.analyze.standard", "抖音运营调用短视频洞察",
                        "把合规提交的视频链接拆成选题、钩子、脚本结构和风险提示",
                        "按 video-insight 目标功能扣费", true),
                rule("douyin-to-shortvideo-maker", ProductCode.DOUYIN_OPS, ProductCode.SHORTVIDEO_MAKER,
                        "shortvideo.script.generate", "抖音运营生成短视频脚本",
                        "把账号定位和内容日历转成可执行短视频脚本",
                        "按 shortvideo-maker 目标功能扣费", true),
                rule("shortvideo-maker-to-video-insight", ProductCode.SHORTVIDEO_MAKER, ProductCode.VIDEO_INSIGHT,
                        "video.analyze.standard", "短视频成片复用爆款拆解",
                        "用短视频洞察结果驱动脚本结构、节奏和视觉参考",
                        "按 video-insight 目标功能扣费", true),
                rule("shortvideo-maker-to-digital-human", ProductCode.SHORTVIDEO_MAKER, ProductCode.DIGITAL_HUMAN,
                        "digital-human.video.synthesize", "短视频成片调用数字人",
                        "将脚本交给数字人生成口播片段，再进入成片工作流",
                        "按 digital-human 目标功能扣费", true),
                rule("shortvideo-maker-to-photo-avatar", ProductCode.SHORTVIDEO_MAKER, ProductCode.PHOTO_AVATAR_VIDEO,
                        "photo-avatar.video.synthesize", "短视频成片调用真人照片口播",
                        "使用真人照片、服装、背景和文案生成口播片段",
                        "按 photo-avatar-video 目标功能扣费", true),
                rule("drama-to-shortvideo-maker", ProductCode.DRAMA_AI, ProductCode.SHORTVIDEO_MAKER,
                        FeatureCode.SHORTVIDEO_EXPORT, "短剧制作调用成片导出",
                        "把短剧分镜、字幕和素材片段合成为短视频成片",
                        "按 shortvideo-maker 目标功能扣费", true),
                rule("drama-to-digital-human", ProductCode.DRAMA_AI, ProductCode.DIGITAL_HUMAN,
                        "digital-human.video.synthesize", "短剧制作调用数字人角色",
                        "把短剧角色台词转成数字人口播或角色片段",
                        "按 digital-human 目标功能扣费", true),
                rule("digital-human-to-photo-avatar", ProductCode.DIGITAL_HUMAN, ProductCode.PHOTO_AVATAR_VIDEO,
                        "photo-avatar.identity.verify", "数字人复用真人授权校验",
                        "在真人照片驱动或形象复用前校验素材授权",
                        "按 photo-avatar-video 目标功能扣费", true),
                rule("mcp-to-video-insight", "mcp-service", ProductCode.VIDEO_INSIGHT,
                        "video.mcp.invoke", "MCP 智能体调用短视频洞察",
                        "个人或企业智能体通过 MCP 工具调用视频洞察能力",
                        "按 video-insight API/MCP 功能扣费", true),
                rule("agent-to-knowledge-base", ProductCode.DOUYIN_OPS, ProductCode.KNOWLEDGE_BASE,
                        FeatureCode.KB_RAG, "智能体工具调用知识库 RAG",
                        "Agent Function Calling 检索知识库前经平台互调治理",
                        "按 knowledge-base.rag 扣费", true)
        );
    }

    /**
     * 检查一次产品间调用是否允许。
     *
     * <p>判定顺序：先匹配互调规则，再检查发起产品授权，最后检查目标产品授权。
     * 只要任一环节失败，本次联动就不能执行。</p>
     */
    public ProductIntegrationDecision check(ProductIntegrationCheckRequest request) {
        Optional<ProductIntegrationRuleSummary> ruleOptional = findRule(request);
        if (ruleOptional.isEmpty()) {
            return denied("NO_INTEGRATION_RULE", "当前产品间调用未登记规则", request, null, null, null);
        }

        ProductIntegrationRuleSummary rule = ruleOptional.get();
        if (!rule.enabled()) {
            return denied("RULE_DISABLED", "当前产品间调用规则已停用", request, rule, null, null);
        }

        CommercialEntitlementDecision sourceDecision = entitlementService.check(new EntitlementCheckRequest(
                request.tenantId(),
                request.userId(),
                request.sourceProductCode(),
                normalizeSourceFeature(request.sourceFeatureCode(), request.sourceProductCode()),
                normalizeChannel(request.channel()),
                normalizeAmount(request.requestedAmount()),
                null,
                request.traceId()
        ));
        if (!sourceDecision.allowed()) {
            return denied("SOURCE_DENIED", "发起产品未授权：" + sourceDecision.reason(), request, rule, sourceDecision, null);
        }

        CommercialEntitlementDecision targetDecision = entitlementService.check(new EntitlementCheckRequest(
                request.tenantId(),
                request.userId(),
                rule.targetProductCode(),
                rule.targetFeatureCode(),
                normalizeChannel(request.channel()),
                normalizeAmount(request.requestedAmount()),
                null,
                request.traceId()
        ));
        if (!targetDecision.allowed()) {
            return denied("TARGET_DENIED", "目标产品未授权：" + targetDecision.reason(), request, rule, sourceDecision, targetDecision);
        }

        return new ProductIntegrationDecision(
                true,
                "ALLOW",
                "允许产品间调用，后续执行必须按目标产品功能计费并写入审计",
                rule,
                sourceDecision,
                targetDecision,
                rule.targetProductCode(),
                rule.targetFeatureCode(),
                rule.auditRequired()
        );
    }

    /**
     * 执行一次产品间商业化调用记账。
     *
     * <p>当前阶段不在这里执行真实视频拆解、数字人合成或短剧渲染，只完成上线前最关键的商业闭环：
     * 互调规则检查、双端授权、积分扣费、用量台账、审计事件和互调流水。后续目标产品服务真正执行任务时，
     * 必须复用同一个 traceId，把业务任务结果继续挂到这条商业链路上。</p>
     */
    @Transactional
    public ProductIntegrationInvocationResult invoke(ProductIntegrationInvocationRequest request) {
        String invocationId = Ids.compactUuid("prod_invoke");
        String traceId = normalizeTraceId(request.traceId());
        BigDecimal amount = normalizeAmount(request.requestedAmount());
        ProductIntegrationCheckRequest checkRequest = checkRequest(request, amount, traceId);
        ProductIntegrationDecision decision = check(checkRequest);
        if (!decision.allowed()) {
            persistInvocation(invocationId, request, decision, "DENIED", ProductIntegrationBillingMode.IMMEDIATE,
                    amount, BigDecimal.ZERO, BigDecimal.ZERO, null, null, traceId);
            return new ProductIntegrationInvocationResult(
                    invocationId,
                    false,
                    "DENIED",
                    decision.reason(),
                    decision,
                    null,
                    null,
                    null,
                    null,
                    ProductIntegrationBillingMode.IMMEDIATE,
                    null,
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    traceId
            );
        }

        CreditConsumeResult creditResult = creditLedgerService.consume(new CreditConsumeRequest(
                normalizeTenant(request.tenantId()),
                normalizeUser(request.userId()),
                normalizeAgent(request.agentId()),
                decision.billingProductCode(),
                decision.billingFeatureCode(),
                normalizeChannel(request.channel()),
                amount,
                normalizeTier(request.pricingTier(), decision.billingFeatureCode()),
                traceId,
                "产品互调扣费：" + safeSummary(request.invocationInputSummary())
        ));
        if (!creditResult.allowed()) {
            persistInvocation(invocationId, request, decision, "CREDIT_DENIED", ProductIntegrationBillingMode.IMMEDIATE,
                    amount, BigDecimal.ZERO, BigDecimal.ZERO, null, null, traceId);
            return new ProductIntegrationInvocationResult(
                    invocationId,
                    false,
                    "CREDIT_DENIED",
                    creditResult.message(),
                    decision,
                    creditResult,
                    null,
                    null,
                    null,
                    ProductIntegrationBillingMode.IMMEDIATE,
                    null,
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    traceId
            );
        }

        BigDecimal revenue = money(creditResult.quote().retailAmountCny());
        BigDecimal providerCost = money(creditResult.quote().estimatedProviderCostCny());
        String usageId = persistUsageLedger(request, decision, amount, revenue, providerCost, traceId);
        String auditId = decision.auditRequired() ? persistAuditEvent(request, decision, traceId) : null;
        persistInvocation(invocationId, request, decision, "RECORDED", ProductIntegrationBillingMode.IMMEDIATE,
                amount, revenue, providerCost, null, null, traceId);

        return new ProductIntegrationInvocationResult(
                invocationId,
                true,
                "RECORDED",
                "产品互调已完成授权、扣费、用量和审计入账",
                decision,
                creditResult,
                null,
                usageId,
                auditId,
                ProductIntegrationBillingMode.IMMEDIATE,
                null,
                null,
                revenue,
                providerCost,
                revenue.subtract(providerCost),
                traceId
        );
    }

    /**
     * 为异步产品互调创建一条授权后的流水占位。
     *
     * <p>平台模块不能依赖 asset/workflow，因此这里只完成双产品授权和互调流水占位。
     * App 编排层随后创建目标产品工作流任务，拿到 taskId 和 reservationId 后再调用
     * {@link #completeWorkflowReservation(ProductIntegrationInvocationRequest, String, ProductIntegrationDecision, String, String, CreditReservationResult)}
     * 回填账本字段。</p>
     */
    @Transactional
    public ProductIntegrationInvocationResult prepareWorkflowInvocation(ProductIntegrationInvocationRequest request) {
        String invocationId = Ids.compactUuid("prod_invoke");
        String traceId = normalizeTraceId(request.traceId());
        BigDecimal amount = normalizeAmount(request.requestedAmount());
        ProductIntegrationDecision decision = check(checkRequest(request, amount, traceId));
        if (!decision.allowed()) {
            persistInvocation(invocationId, request, decision, "DENIED", ProductIntegrationBillingMode.WORKFLOW_RESERVED,
                    amount, BigDecimal.ZERO, BigDecimal.ZERO, null, null, traceId);
            return new ProductIntegrationInvocationResult(
                    invocationId,
                    false,
                    "DENIED",
                    decision.reason(),
                    decision,
                    null,
                    null,
                    null,
                    null,
                    ProductIntegrationBillingMode.WORKFLOW_RESERVED,
                    null,
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    traceId
            );
        }

        persistInvocation(invocationId, request, decision, "WORKFLOW_AUTHORIZED", ProductIntegrationBillingMode.WORKFLOW_RESERVED,
                amount, BigDecimal.ZERO, BigDecimal.ZERO, null, null, traceId);
        return new ProductIntegrationInvocationResult(
                invocationId,
                true,
                "WORKFLOW_AUTHORIZED",
                "产品互调已通过双端授权，等待创建目标产品工作流任务并冻结积分",
                decision,
                null,
                null,
                null,
                null,
                ProductIntegrationBillingMode.WORKFLOW_RESERVED,
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                traceId
        );
    }

    /**
     * 将互调流水绑定到目标产品工作流任务和积分冻结单。
     *
     * <p>绑定成功后，互调流水、工作流任务、积分冻结单、用量台账和审计事件都共享同一 traceId。
     * 后续工作流执行器完成任务时提交冻结积分，失败或超时时释放冻结积分。</p>
     */
    @Transactional
    public ProductIntegrationInvocationResult completeWorkflowReservation(
            ProductIntegrationInvocationRequest request,
            String invocationId,
            ProductIntegrationDecision decision,
            String workflowTaskId,
            String reservationId,
            CreditReservationResult reservationResult
    ) {
        String traceId = normalizeTraceId(request.traceId());
        BigDecimal amount = normalizeAmount(request.requestedAmount());
        boolean reserved = reservationResult != null && reservationResult.allowed();
        BigDecimal revenue = reserved ? money(reservationResult.quote().retailAmountCny()) : BigDecimal.ZERO;
        BigDecimal providerCost = reserved ? money(reservationResult.quote().estimatedProviderCostCny()) : BigDecimal.ZERO;
        String status = reserved ? "WORKFLOW_RESERVED" : "CREDIT_DENIED";
        String usageId = reserved ? persistUsageLedger(request, decision, amount, revenue, providerCost, traceId) : null;
        String auditId = reserved && decision.auditRequired() ? persistAuditEvent(request, decision, traceId) : null;

        jdbcTemplate.update(
                """
                        update gf_product_integration_invocation
                        set status = ?, retail_revenue_cny = ?, provider_cost_cny = ?,
                            workflow_task_id = ?, reservation_id = ?, decision_code = ?, decision_reason = ?
                        where invocation_id = ?
                        """,
                status,
                revenue,
                providerCost,
                normalizeNullable(workflowTaskId),
                normalizeNullable(reservationId),
                reserved ? "WORKFLOW_RESERVED" : "CREDIT_DENIED",
                reserved ? "目标产品工作流已创建并完成积分冻结" : reservationFailureMessage(reservationResult),
                invocationId
        );

        return new ProductIntegrationInvocationResult(
                invocationId,
                reserved,
                status,
                reserved ? "产品互调已绑定目标工作流任务，积分等待任务成功后提交扣减" : reservationFailureMessage(reservationResult),
                decision,
                null,
                reservationResult,
                usageId,
                auditId,
                ProductIntegrationBillingMode.WORKFLOW_RESERVED,
                workflowTaskId,
                reservationId,
                revenue,
                providerCost,
                revenue.subtract(providerCost),
                traceId
        );
    }

    /**
     * 查询最近产品互调流水。
     *
     * <p>该查询用于控制台确认六个独立产品之间的调用是否进入统一商业账本。
     * 它只读互调流水表，不反向读取业务任务明细，保证平台治理层不会依赖具体产品实现。</p>
     */
    public List<ProductIntegrationInvocationSummary> recentInvocations(String tenantId) {
        return jdbcTemplate.query(
                """
                        select invocation_id, tenant_id, user_id, agent_id, source_product_code, source_feature_code,
                               target_product_code, target_feature_code, billing_product_code, billing_feature_code,
                               channel, status, billing_mode, workflow_task_id, reservation_id,
                               requested_amount, retail_revenue_cny, provider_cost_cny, trace_id, created_at
                        from gf_product_integration_invocation
                        where tenant_id = ?
                        order by created_at desc
                        limit 50
                        """,
                (rs, rowNum) -> {
                    BigDecimal revenue = money(rs.getBigDecimal("retail_revenue_cny"));
                    BigDecimal cost = money(rs.getBigDecimal("provider_cost_cny"));
                    return new ProductIntegrationInvocationSummary(
                            rs.getString("invocation_id"),
                            rs.getString("tenant_id"),
                            rs.getString("user_id"),
                            rs.getString("agent_id"),
                            rs.getString("source_product_code"),
                            rs.getString("source_feature_code"),
                            rs.getString("target_product_code"),
                            rs.getString("target_feature_code"),
                            rs.getString("billing_product_code"),
                            rs.getString("billing_feature_code"),
                            rs.getString("channel"),
                            rs.getString("status"),
                            ProductIntegrationBillingMode.valueOf(rs.getString("billing_mode")),
                            rs.getString("workflow_task_id"),
                            rs.getString("reservation_id"),
                            rs.getBigDecimal("requested_amount"),
                            revenue,
                            cost,
                            revenue.subtract(cost),
                            rs.getString("trace_id"),
                            rs.getObject("created_at", OffsetDateTime.class)
                    );
                },
                normalizeTenant(tenantId)
        );
    }

    private Optional<ProductIntegrationRuleSummary> findRule(ProductIntegrationCheckRequest request) {
        return listRules().stream()
                .filter(rule -> rule.sourceProductCode().equals(request.sourceProductCode()))
                .filter(rule -> rule.targetProductCode().equals(request.targetProductCode()))
                .filter(rule -> rule.targetFeatureCode().equals(request.targetFeatureCode()))
                .findFirst();
    }

    private ProductIntegrationCheckRequest checkRequest(ProductIntegrationInvocationRequest request, BigDecimal amount, String traceId) {
        return new ProductIntegrationCheckRequest(
                normalizeTenant(request.tenantId()),
                normalizeUser(request.userId()),
                request.sourceProductCode(),
                request.sourceFeatureCode(),
                request.targetProductCode(),
                request.targetFeatureCode(),
                normalizeChannel(request.channel()),
                amount,
                traceId
        );
    }

    private ProductIntegrationDecision denied(
            String code,
            String reason,
            ProductIntegrationCheckRequest request,
            ProductIntegrationRuleSummary rule,
            CommercialEntitlementDecision sourceDecision,
            CommercialEntitlementDecision targetDecision
    ) {
        String billingProductCode = rule == null ? request.targetProductCode() : rule.targetProductCode();
        String billingFeatureCode = rule == null ? request.targetFeatureCode() : rule.targetFeatureCode();
        return new ProductIntegrationDecision(
                false,
                code,
                reason,
                rule,
                sourceDecision,
                targetDecision,
                billingProductCode,
                billingFeatureCode,
                rule != null && rule.auditRequired()
        );
    }

    private ProductIntegrationRuleSummary rule(
            String ruleCode,
            String sourceProductCode,
            String targetProductCode,
            String targetFeatureCode,
            String invocationName,
            String scenario,
            String billingPolicy,
            boolean auditRequired
    ) {
        return new ProductIntegrationRuleSummary(
                ruleCode,
                sourceProductCode,
                targetProductCode,
                targetFeatureCode,
                invocationName,
                scenario,
                billingPolicy,
                auditRequired,
                true
        );
    }

    private String normalizeSourceFeature(String sourceFeatureCode, String sourceProductCode) {
        if (sourceFeatureCode != null && !sourceFeatureCode.isBlank()) {
            return sourceFeatureCode.trim();
        }
        return switch (sourceProductCode) {
            case ProductCode.DOUYIN_OPS -> "douyin.content.calendar";
            case ProductCode.SHORTVIDEO_MAKER -> "shortvideo.script.generate";
            case ProductCode.DRAMA_AI -> "drama.storyboard.generate";
            case ProductCode.DIGITAL_HUMAN -> "digital-human.script.generate";
            case ProductCode.PHOTO_AVATAR_VIDEO -> "photo-avatar.identity.verify";
            case ProductCode.VIDEO_INSIGHT -> "video.analyze.standard";
            default -> "";
        };
    }

    private String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return "WEB";
        }
        return channel.trim().toUpperCase();
    }

    private BigDecimal normalizeAmount(BigDecimal requestedAmount) {
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return requestedAmount;
    }

    private void persistInvocation(
            String invocationId,
            ProductIntegrationInvocationRequest request,
            ProductIntegrationDecision decision,
            String status,
            ProductIntegrationBillingMode billingMode,
            BigDecimal amount,
            BigDecimal revenue,
            BigDecimal providerCost,
            String workflowTaskId,
            String reservationId,
            String traceId
    ) {
        jdbcTemplate.update(
                """
                        insert into gf_product_integration_invocation
                        (invocation_id, tenant_id, user_id, agent_id, source_product_code, source_feature_code,
                         target_product_code, target_feature_code, billing_product_code, billing_feature_code,
                         channel, status, requested_amount, retail_revenue_cny, provider_cost_cny, trace_id,
                         decision_code, decision_reason, billing_mode, workflow_task_id, reservation_id, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                invocationId,
                normalizeTenant(request.tenantId()),
                normalizeUser(request.userId()),
                normalizeAgent(request.agentId()),
                request.sourceProductCode(),
                normalizeNullable(request.sourceFeatureCode()),
                request.targetProductCode(),
                request.targetFeatureCode(),
                decision.billingProductCode(),
                decision.billingFeatureCode(),
                normalizeChannel(request.channel()),
                status,
                amount,
                money(revenue),
                money(providerCost),
                traceId,
                decision.decisionCode(),
                decision.reason(),
                billingMode.name(),
                normalizeNullable(workflowTaskId),
                normalizeNullable(reservationId),
                OffsetDateTime.now()
        );
    }

    private String persistUsageLedger(
            ProductIntegrationInvocationRequest request,
            ProductIntegrationDecision decision,
            BigDecimal amount,
            BigDecimal revenue,
            BigDecimal providerCost,
            String traceId
    ) {
        String usageId = Ids.compactUuid("usage");
        jdbcTemplate.update(
                """
                        insert into gf_usage_ledger (usage_id, tenant_id, user_id, agent_id, product_code, feature_code,
                                                     channel, unit, amount, retail_revenue_cny, provider_cost_cny, trace_id, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                usageId,
                normalizeTenant(request.tenantId()),
                normalizeUser(request.userId()),
                normalizeAgent(request.agentId()),
                decision.billingProductCode(),
                decision.billingFeatureCode(),
                normalizeChannel(request.channel()),
                "integration_call",
                amount,
                revenue,
                providerCost,
                traceId,
                OffsetDateTime.now()
        );
        return usageId;
    }

    private String persistAuditEvent(
            ProductIntegrationInvocationRequest request,
            ProductIntegrationDecision decision,
            String traceId
    ) {
        String auditId = Ids.compactUuid("audit");
        jdbcTemplate.update(
                """
                        insert into gf_audit_event (audit_id, tenant_id, user_id, agent_id, product_code, feature_code,
                                                    channel, risk_level, status, trace_id, decision_note, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                auditId,
                normalizeTenant(request.tenantId()),
                normalizeUser(request.userId()),
                normalizeAgent(request.agentId()),
                decision.billingProductCode(),
                decision.billingFeatureCode(),
                normalizeChannel(request.channel()),
                "MEDIUM",
                "RECORDED",
                traceId,
                "产品互调审计：" + safeSummary(request.invocationInputSummary()),
                OffsetDateTime.now()
        );
        return auditId;
    }

    private String normalizeTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return "demo-tenant";
        }
        return tenantId.trim();
    }

    private String normalizeUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return "demo-user";
        }
        return userId.trim();
    }

    private String normalizeAgent(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return null;
        }
        return agentId.trim();
    }

    private String normalizeTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return Ids.compactUuid("trace_integration");
        }
        return traceId.trim();
    }

    private String normalizeTier(String pricingTier, String billingFeatureCode) {
        if (pricingTier != null && !pricingTier.isBlank()) {
            return pricingTier.trim();
        }
        if (billingFeatureCode != null && billingFeatureCode.contains("video.synthesize")) {
            return "video";
        }
        if (billingFeatureCode != null && billingFeatureCode.contains("storyboard")) {
            return "storyboard";
        }
        return "standard";
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String safeSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return "未提交输入摘要";
        }
        String trimmed = summary.trim();
        // 互调账本只保存短摘要，避免把用户素材、脚本原文或商业机密扩散到平台治理表。
        if (trimmed.length() > 180) {
            return trimmed.substring(0, 180);
        }
        return trimmed;
    }

    private String reservationFailureMessage(CreditReservationResult reservationResult) {
        if (reservationResult == null) {
            return "目标工作流未返回积分冻结结果";
        }
        return reservationResult.message();
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
