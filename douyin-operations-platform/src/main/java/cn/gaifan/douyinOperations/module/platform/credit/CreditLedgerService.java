package cn.gaifan.douyinOperations.module.platform.credit;

import cn.gaifan.douyinOperations.common.id.Ids;
import cn.gaifan.douyinOperations.contract.credit.CreditAccountSummary;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeResult;
import cn.gaifan.douyinOperations.contract.credit.CreditGovernanceOverview;
import cn.gaifan.douyinOperations.contract.credit.CreditLedgerEntrySummary;
import cn.gaifan.douyinOperations.contract.credit.CreditQuoteRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditQuoteResult;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationActionRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationResult;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationStatus;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationTimeoutCandidateSummary;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationTimeoutReleaseExecutionSummary;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationTimeoutReleaseRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationTimeoutReleaseResult;
import cn.gaifan.douyinOperations.contract.credit.CreditReserveRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditTransactionType;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 平台积分账本服务。
 *
 * <p>积分中心是 Gaifan Ops 商业化的统一扣费入口。Web、App、小程序、OpenAPI、MCP、
 * 企业智能体和后续行业 AI 应用都不能自己直接扣费，必须先经过本服务完成报价、余额校验、
 * 冻结、扣减和审计字段归集。</p>
 *
 * <p>MVP 阶段使用确定性的 mock 余额和 mock 流水，便于本地 Docker 环境完成端到端验证。
 * 真实上线时应把账户、流水、冻结记录和支付订单写入数据库，并用事务或事件溯源保证余额一致。</p>
 */
@Service
public class CreditLedgerService {
    private static final String DEFAULT_TENANT = "demo-tenant";
    private static final String CURRENCY = "CREDIT";
    private static final BigDecimal DEMO_TOTAL_GRANTED = new BigDecimal("20000");
    private static final BigDecimal DEMO_USED = new BigDecimal("2860");
    private static final BigDecimal DEMO_FROZEN = new BigDecimal("120");
    private static final BigDecimal DEMO_EXPIRED = new BigDecimal("0");
    private static final long DEFAULT_TIMEOUT_MINUTES = 60;

    private final JdbcTemplate jdbcTemplate;

    public CreditLedgerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询租户积分治理总览。
     *
     * <p>该总览给控制台、开放 API 页面和 MCP 页面复用，确保所有商业通道展示同一套余额、
     * 定价规则和账本治理说明。</p>
     */
    public CreditGovernanceOverview overview(String tenantId) {
        String normalizedTenant = normalizeTenant(tenantId);
        return new CreditGovernanceOverview(
                normalizedTenant,
                account(normalizedTenant),
                pricingRules(normalizedTenant),
                recentLedger(normalizedTenant),
                List.of(
                        "所有 Web、App、小程序、OpenAPI、MCP、企业智能体调用共用租户积分账户",
                        "业务产品只声明 productCode 和 featureCode，不直接实现支付或扣费",
                        "调用链必须携带 tenantId、userId、agentId、channel、traceId",
                        "真实长任务必须先冻结积分，成功后扣减，失败后释放",
                        "支付中心负责充值和订阅发放，积分中心负责消费和账本追溯"
                )
        );
    }

    /**
     * 查询当前租户积分账户。
     *
     * <p>当前使用演示余额，非 demo 租户返回 0 余额，避免误判任意租户都有可消费额度。</p>
     */
    public CreditAccountSummary account(String tenantId) {
        String normalizedTenant = normalizeTenant(tenantId);
        List<CreditAccountSummary> databaseAccounts = jdbcTemplate.query(
                """
                        select tenant_id, total_granted, available_credits, frozen_credits, used_credits, expired_credits
                        from gf_credit_account
                        where tenant_id = ?
                        """,
                (rs, rowNum) -> new CreditAccountSummary(
                        rs.getString("tenant_id"),
                        rs.getBigDecimal("total_granted"),
                        rs.getBigDecimal("available_credits"),
                        rs.getBigDecimal("frozen_credits"),
                        rs.getBigDecimal("used_credits"),
                        rs.getBigDecimal("expired_credits"),
                        CURRENCY,
                        "monthly"
                ),
                normalizedTenant
        );
        if (!databaseAccounts.isEmpty()) {
            return databaseAccounts.get(0);
        }
        if (!DEFAULT_TENANT.equals(normalizedTenant)) {
            return new CreditAccountSummary(
                    normalizedTenant,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    CURRENCY,
                    "monthly"
            );
        }
        BigDecimal available = DEMO_TOTAL_GRANTED
                .subtract(DEMO_USED)
                .subtract(DEMO_FROZEN)
                .subtract(DEMO_EXPIRED);
        return new CreditAccountSummary(
                normalizedTenant,
                DEMO_TOTAL_GRANTED,
                available,
                DEMO_FROZEN,
                DEMO_USED,
                DEMO_EXPIRED,
                CURRENCY,
                "monthly"
        );
    }

    /**
     * 对一次商业化调用进行积分报价。
     *
     * <p>报价根据产品、功能、通道和请求数量计算。OPENAPI 与 MCP 会有轻微通道系数，
     * 用于覆盖签名、网关、审计、限流和企业 Agent 调用治理成本。</p>
     */
    public CreditQuoteResult quote(CreditQuoteRequest request) {
        String tenantId = normalizeTenant(request.tenantId());
        String channel = normalizeChannel(request.channel());
        BigDecimal amount = normalizeAmount(request.requestedAmount());
        PricingRule rule = pricingRule(request.productCode(), request.featureCode(), channel, request.pricingTier());
        BigDecimal totalCredits = rule.unitCredits().multiply(amount).setScale(2, RoundingMode.HALF_UP);
        CreditAccountSummary account = account(tenantId);

        return new CreditQuoteResult(
                Ids.compactUuid("quote"),
                tenantId,
                request.productCode(),
                request.featureCode(),
                channel,
                amount,
                rule.unitCredits(),
                totalCredits,
                totalCredits.multiply(rule.retailCnyPerCredit()).setScale(2, RoundingMode.HALF_UP),
                totalCredits.multiply(rule.providerCostCnyPerCredit()).setScale(2, RoundingMode.HALF_UP),
                account.availableCredits(),
                account.availableCredits().compareTo(totalCredits) >= 0,
                rule.description(),
                normalizeTraceId(request.traceId())
        );
    }

    /**
     * 消费积分。
     *
     * <p>该方法保留给现有同步调用方。内部已经改为先 reserve 再 commit，保证和长任务三段式使用
     * 同一张冻结单、同一套流水和同一套余额更新逻辑。</p>
     */
    @Transactional
    public CreditConsumeResult consume(CreditConsumeRequest request) {
        CreditReservationResult reserved = reserve(new CreditReserveRequest(
                request.tenantId(),
                request.userId(),
                request.agentId(),
                request.productCode(),
                request.featureCode(),
                request.channel(),
                request.requestedAmount(),
                request.pricingTier(),
                request.traceId(),
                null,
                request.reason()
        ));
        if (!reserved.allowed()) {
            return new CreditConsumeResult(
                    Ids.compactUuid("credit_txn"),
                    false,
                    reserved.decisionCode(),
                    reserved.message(),
                    reserved.quote(),
                    BigDecimal.ZERO,
                    reserved.balanceAfter(),
                    reserved.reserveEntry(),
                    reserved.commitEntry()
            );
        }
        CreditReservationResult committed = commit(
                reserved.reservationId(),
                new CreditReservationActionRequest(
                        request.tenantId(),
                        request.userId(),
                        request.agentId(),
                        request.channel(),
                        request.traceId(),
                        request.reason()
                )
        );

        return new CreditConsumeResult(
                Ids.compactUuid("credit_txn"),
                committed.allowed(),
                committed.decisionCode(),
                committed.message(),
                committed.quote(),
                committed.allowed() ? committed.reservedCredits() : BigDecimal.ZERO,
                committed.balanceAfter(),
                committed.reserveEntry(),
                committed.commitEntry()
        );
    }

    /**
     * 冻结积分。
     *
     * <p>这是长任务的第一阶段：只减少可用余额并增加冻结余额，不增加 usedCredits。
     * 如果调用方提供 businessKey，同一租户下重复 reserve 会返回既有冻结单，避免前端重试或队列重投
     * 造成重复冻结。</p>
     */
    @Transactional
    public CreditReservationResult reserve(CreditReserveRequest request) {
        String tenantId = normalizeTenant(request.tenantId());
        String businessKey = normalizeOptional(request.businessKey());
        if (businessKey != null) {
            Optional<CreditReservationRecord> existing = findReservationByBusinessKey(tenantId, businessKey);
            if (existing.isPresent()) {
                return toReservationResult(existing.get(), true, "IDEMPOTENT_RESERVATION", "已存在相同业务键的冻结单");
            }
        }

        CreditQuoteResult quote = quote(new CreditQuoteRequest(
                tenantId,
                request.userId(),
                request.agentId(),
                request.productCode(),
                request.featureCode(),
                request.channel(),
                request.requestedAmount(),
                request.pricingTier(),
                request.traceId()
        ));
        if (!quote.sufficient()) {
            return new CreditReservationResult(
                    null,
                    null,
                    false,
                    "INSUFFICIENT_CREDITS",
                    "积分余额不足，无法冻结长任务额度",
                    quote,
                    BigDecimal.ZERO,
                    quote.availableBefore(),
                    null,
                    null,
                    null
            );
        }

        String reservationId = Ids.compactUuid("credit_reserve");
        BigDecimal balanceAfterReserve = quote.availableBefore().subtract(quote.totalCredits());
        CreditLedgerEntrySummary reserveEntry = persistLedgerEntry(
                new CreditConsumeRequest(
                        tenantId,
                        request.userId(),
                        request.agentId(),
                        request.productCode(),
                        request.featureCode(),
                        request.channel(),
                        request.requestedAmount(),
                        request.pricingTier(),
                        quote.traceId(),
                        request.reason()
                ),
                CreditTransactionType.RESERVE,
                quote.totalCredits(),
                balanceAfterReserve,
                "长任务预冻结积分：" + normalizeReason(request.reason())
        );

        try {
            insertReservation(request, reservationId, quote, balanceAfterReserve, reserveEntry);
        } catch (DuplicateKeyException duplicate) {
            // 并发重试命中同一个业务键时，回滚本次冻结流水和余额变动，把已有冻结单作为幂等结果返回。
            throw duplicate;
        }

        return toReservationResult(findReservation(reservationId).orElseThrow(), true, "RESERVED", "积分已冻结，等待业务任务提交或释放");
    }

    /**
     * 提交冻结积分。
     *
     * <p>这是长任务成功后的第二阶段：冻结余额转入已用余额，并写入 COMMIT 流水。
     * 已提交的冻结单重复提交会返回原结果，保证回调重试不会重复扣费。</p>
     */
    @Transactional
    public CreditReservationResult commit(String reservationId, CreditReservationActionRequest request) {
        CreditReservationRecord reservation = requireReservation(reservationId);
        if (reservation.status() == CreditReservationStatus.COMMITTED) {
            return toReservationResult(reservation, true, "ALREADY_COMMITTED", "冻结单已提交，重复请求未再次扣费");
        }
        if (reservation.status() == CreditReservationStatus.RELEASED) {
            return toReservationResult(reservation, false, "ALREADY_RELEASED", "冻结单已释放，不能再提交扣减");
        }

        CreditLedgerEntrySummary commitEntry = persistLedgerEntry(
                toConsumeRequest(reservation, request),
                CreditTransactionType.COMMIT,
                reservation.reservedCredits(),
                reservation.balanceAfterReserve(),
                "长任务执行成功后扣减积分：" + normalizeReason(request.reason())
        );
        jdbcTemplate.update(
                """
                        update gf_credit_reservation
                        set status = ?, commit_entry_id = ?, updated_at = ?
                        where reservation_id = ? and status = ?
                        """,
                CreditReservationStatus.COMMITTED.name(),
                commitEntry.entryId(),
                OffsetDateTime.now(),
                reservationId,
                CreditReservationStatus.RESERVED.name()
        );
        return toReservationResult(findReservation(reservationId).orElseThrow(), true, "COMMITTED", "冻结积分已正式扣减");
    }

    /**
     * 释放冻结积分。
     *
     * <p>这是长任务失败、取消或超时后的第二阶段：冻结余额返还可用余额，并写入 RELEASE 流水。
     * 已释放的冻结单重复释放会返回原结果，避免队列重试导致余额重复返还。</p>
     */
    @Transactional
    public CreditReservationResult release(String reservationId, CreditReservationActionRequest request) {
        CreditReservationRecord reservation = requireReservation(reservationId);
        if (reservation.status() == CreditReservationStatus.RELEASED) {
            return toReservationResult(reservation, true, "ALREADY_RELEASED", "冻结单已释放，重复请求未再次返还");
        }
        if (reservation.status() == CreditReservationStatus.COMMITTED) {
            return toReservationResult(reservation, false, "ALREADY_COMMITTED", "冻结单已提交扣减，不能再释放");
        }

        BigDecimal balanceAfterRelease = reservation.balanceAfterReserve().add(reservation.reservedCredits());
        CreditLedgerEntrySummary releaseEntry = persistLedgerEntry(
                toConsumeRequest(reservation, request),
                CreditTransactionType.RELEASE,
                reservation.reservedCredits(),
                balanceAfterRelease,
                "长任务失败或取消释放积分：" + normalizeReason(request.reason())
        );
        jdbcTemplate.update(
                """
                        update gf_credit_reservation
                        set status = ?, release_entry_id = ?, updated_at = ?
                        where reservation_id = ? and status = ?
                        """,
                CreditReservationStatus.RELEASED.name(),
                releaseEntry.entryId(),
                OffsetDateTime.now(),
                reservationId,
                CreditReservationStatus.RESERVED.name()
        );
        return toReservationResult(findReservation(reservationId).orElseThrow(), true, "RELEASED", "冻结积分已释放回可用余额");
    }

    /**
     * 查询长时间未提交或释放的冻结单候选。
     *
     * <p>候选查询只读取 RESERVED 状态，不修改余额。数字人、真人口播、短剧、批量拆解和供应商异步任务
     * 都可以使用该队列发现“供应商未回调/工作流未终止”造成的永久冻结风险。</p>
     */
    public List<CreditReservationTimeoutCandidateSummary> timeoutReleaseCandidates(String tenantId, long timeoutMinutes) {
        String normalizedTenant = normalizeTenant(tenantId);
        long thresholdMinutes = timeoutMinutes <= 0 ? DEFAULT_TIMEOUT_MINUTES : timeoutMinutes;
        OffsetDateTime deadline = OffsetDateTime.now().minusMinutes(thresholdMinutes);
        return jdbcTemplate.query(
                """
                        select reservation_id, tenant_id, user_id, agent_id, product_code, feature_code, channel,
                               reserved_credits, status, trace_id, business_key, reason, created_at, updated_at
                        from gf_credit_reservation
                        where tenant_id = ? and status = ? and updated_at <= ?
                        order by updated_at asc
                        limit 20
                        """,
                (rs, rowNum) -> timeoutCandidate(
                        rs.getString("reservation_id"),
                        rs.getString("tenant_id"),
                        rs.getString("user_id"),
                        rs.getString("agent_id"),
                        rs.getString("product_code"),
                        rs.getString("feature_code"),
                        rs.getString("channel"),
                        rs.getBigDecimal("reserved_credits"),
                        rs.getString("status"),
                        rs.getString("trace_id"),
                        rs.getString("business_key"),
                        rs.getString("reason"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)
                ),
                normalizedTenant,
                CreditReservationStatus.RESERVED.name(),
                deadline
        );
    }

    /**
     * 对超时冻结单执行受控释放。
     *
     * <p>该方法不会跳过冻结单生命周期，内部复用标准 release 逻辑更新余额和写 RELEASE 流水。
     * 额外的执行台账和审计事件只用于证明这次释放来自超时治理队列，而不是产品业务回调。</p>
     */
    @Transactional
    public CreditReservationTimeoutReleaseResult executeTimeoutRelease(
            String reservationId,
            CreditReservationTimeoutReleaseRequest request,
            long timeoutMinutes
    ) {
        String tenantId = normalizeTenant(request.tenantId());
        String normalizedReservationId = normalizeRequired(reservationId, "缺少 reservationId");
        String idempotencyKey = valueOrDefault(request.idempotencyKey(), "credit-timeout-release:" + normalizedReservationId);
        Optional<CreditReservationTimeoutReleaseExecutionSummary> existing = findTimeoutReleaseExecution(tenantId, idempotencyKey);
        if (existing.isPresent()) {
            return new CreditReservationTimeoutReleaseResult(
                    existing.get(),
                    findReservation(normalizedReservationId)
                            .map(row -> toReservationResult(row, true, "TIMEOUT_RELEASE_REPLAYED", "超时释放幂等重放，返回既有执行结果"))
                            .orElse(null),
                    true,
                    "积分冻结超时释放幂等命中，未重复释放"
            );
        }
        CreditReservationRecord reservation = requireReservation(normalizedReservationId);
        if (!tenantId.equals(reservation.tenantId())) {
            throw new IllegalArgumentException("冻结单不属于当前租户");
        }
        if (reservation.status() != CreditReservationStatus.RESERVED) {
            throw new IllegalArgumentException("只有 RESERVED 状态冻结单才能执行超时释放：" + reservation.status());
        }
        long thresholdMinutes = timeoutMinutes <= 0 ? DEFAULT_TIMEOUT_MINUTES : timeoutMinutes;
        long ageMinutes = Duration.between(reservation.updatedAt(), OffsetDateTime.now()).toMinutes();
        if (ageMinutes < thresholdMinutes) {
            throw new IllegalArgumentException("冻结单尚未达到超时释放阈值：" + ageMinutes + " 分钟");
        }
        String traceId = firstNonBlank(request.traceId(), reservation.traceId(), Ids.compactUuid("trace_credit_timeout"));
        String operatorUserId = firstNonBlank(request.operatorUserId(), "system-timeout-release");
        String reason = firstNonBlank(request.reason(), "长任务冻结超时未收到成功或失败回调，受控释放");
        String auditId = recordTimeoutReleaseAudit(reservation, operatorUserId, traceId, reason, ageMinutes);
        CreditReservationResult released = release(normalizedReservationId, new CreditReservationActionRequest(
                tenantId,
                reservation.userId(),
                reservation.agentId(),
                reservation.channel(),
                traceId,
                reason
        ));
        CreditReservationTimeoutReleaseExecutionSummary execution = recordTimeoutReleaseExecution(
                reservation,
                released,
                idempotencyKey,
                auditId,
                traceId,
                operatorUserId,
                reason
        );
        return new CreditReservationTimeoutReleaseResult(
                execution,
                released,
                false,
                "积分冻结超时释放已受控执行，并写入 RELEASE 流水、执行台账和审计事件"
        );
    }

    /**
     * 展示演示账本最近流水。
     *
     * <p>这些流水覆盖订阅发放、MCP、OPENAPI、Web 和数字人等场景，方便验证未来多产品、
     * 多端、多 Agent 的统一余额设计。</p>
     */
    public List<CreditLedgerEntrySummary> recentLedger(String tenantId) {
        String normalizedTenant = normalizeTenant(tenantId);
        List<CreditLedgerEntrySummary> databaseLedger = jdbcTemplate.query(
                """
                        select entry_id, tenant_id, user_id, agent_id, product_code, feature_code, channel,
                               transaction_type, credits, balance_after, trace_id, reason, created_at
                        from gf_credit_ledger
                        where tenant_id = ?
                        order by created_at desc
                        limit 20
                        """,
                (rs, rowNum) -> new CreditLedgerEntrySummary(
                        rs.getString("entry_id"),
                        rs.getString("tenant_id"),
                        rs.getString("user_id"),
                        rs.getString("agent_id"),
                        rs.getString("product_code"),
                        rs.getString("feature_code"),
                        rs.getString("channel"),
                        CreditTransactionType.valueOf(rs.getString("transaction_type")),
                        rs.getBigDecimal("credits"),
                        rs.getBigDecimal("balance_after"),
                        rs.getString("trace_id"),
                        rs.getString("reason"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                normalizedTenant
        );
        if (!databaseLedger.isEmpty()) {
            return databaseLedger;
        }
        OffsetDateTime now = OffsetDateTime.now();
        return List.of(
                ledger("credit_ledger_grant_001", normalizedTenant, "system", null, "platform",
                        "subscription.monthly.grant", "PAYMENT", CreditTransactionType.GRANT,
                        "20000", "20000", "sub_demo_video_insight_pro", "订阅发放月度积分", now.minusDays(15)),
                ledger("credit_ledger_mcp_002", normalizedTenant, "demo-user", "agent_demo_ops",
                        ProductCode.VIDEO_INSIGHT, "video.analyze.standard", "MCP", CreditTransactionType.COMMIT,
                        "19.90", "17120.10", "front-mcp-1001", "MCP 单条视频标准拆解扣减", now.minusHours(2)),
                ledger("credit_ledger_api_003", normalizedTenant, "demo-operator", "agent_demo_partner",
                        ProductCode.VIDEO_INSIGHT, "video.mcp.invoke", "OPENAPI", CreditTransactionType.COMMIT,
                        "99.00", "17021.10", "api-video-2002", "开放 API 批量能力调用扣减", now.minusHours(5)),
                ledger("credit_ledger_web_004", normalizedTenant, "demo-user", null,
                        ProductCode.DOUYIN_OPS, "douyin.content.calendar", "WEB", CreditTransactionType.COMMIT,
                        "5.00", "17016.10", "front-ai-3003", "抖音内容日历生成扣减", now.minusDays(1)),
                ledger("credit_ledger_dh_005", normalizedTenant, "demo-user", null,
                        ProductCode.DIGITAL_HUMAN, "digital-human.video.synthesize", "WEB", CreditTransactionType.RESERVE,
                        "120.00", "16896.10", "front-dh-4004", "数字人视频长任务预冻结", now.minusMinutes(40))
        );
    }

    private List<CreditQuoteResult> pricingRules(String tenantId) {
        String normalizedTenant = normalizeTenant(tenantId);
        return List.of(
                quote(exampleQuote(normalizedTenant, ProductCode.VIDEO_INSIGHT, "video.analyze.standard", "MCP", "standard")),
                quote(exampleQuote(normalizedTenant, ProductCode.VIDEO_INSIGHT, "video.analyze.deep", "OPENAPI", "deep")),
                quote(exampleQuote(normalizedTenant, ProductCode.VIDEO_INSIGHT, "video.mcp.invoke", "OPENAPI", "api")),
                quote(exampleQuote(normalizedTenant, ProductCode.DIGITAL_HUMAN, "digital-human.video.synthesize", "WEB", "video")),
                quote(exampleQuote(normalizedTenant, ProductCode.DRAMA_AI, "drama.storyboard.generate", "WEB", "storyboard"))
        );
    }

    private CreditQuoteRequest exampleQuote(
            String tenantId,
            String productCode,
            String featureCode,
            String channel,
            String tier
    ) {
        return new CreditQuoteRequest(
                tenantId,
                "demo-user",
                null,
                productCode,
                featureCode,
                channel,
                BigDecimal.ONE,
                tier,
                "pricing-preview"
        );
    }

    private PricingRule pricingRule(String productCode, String featureCode, String channel, String pricingTier) {
        BigDecimal channelFactor = switch (channel) {
            case "OPENAPI" -> new BigDecimal("1.10");
            case "MCP" -> new BigDecimal("1.05");
            default -> BigDecimal.ONE;
        };

        BigDecimal baseCredits = baseCredits(productCode, featureCode, pricingTier);
        BigDecimal unitCredits = baseCredits.multiply(channelFactor).setScale(2, RoundingMode.HALF_UP);
        return new PricingRule(
                unitCredits,
                new BigDecimal("0.10"),
                new BigDecimal("0.025"),
                "base=" + baseCredits + ", channelFactor=" + channelFactor + ", tier=" + normalizeTier(pricingTier)
        );
    }

    private BigDecimal baseCredits(String productCode, String featureCode, String pricingTier) {
        if (ProductCode.VIDEO_INSIGHT.equals(productCode) && "video.analyze.deep".equals(featureCode)) {
            return new BigDecimal("69.90");
        }
        if (ProductCode.VIDEO_INSIGHT.equals(productCode) && "video.analyze.standard".equals(featureCode)) {
            return new BigDecimal("19.90");
        }
        if (ProductCode.VIDEO_INSIGHT.equals(productCode) && "video.mcp.invoke".equals(featureCode)) {
            return new BigDecimal("3.30");
        }
        if (ProductCode.VIDEO_INSIGHT.equals(productCode) && "video.search.fulltext".equals(featureCode)) {
            return new BigDecimal("1.90");
        }
        if (ProductCode.VIDEO_INSIGHT.equals(productCode) && "video.find_similar".equals(featureCode)) {
            return new BigDecimal("3.30");
        }
        if (ProductCode.DIGITAL_HUMAN.equals(productCode) && "digital-human.video.synthesize".equals(featureCode)) {
            return new BigDecimal("120.00");
        }
        if (ProductCode.PHOTO_AVATAR_VIDEO.equals(productCode) && "photo-avatar.video.synthesize".equals(featureCode)) {
            return new BigDecimal("180.00");
        }
        if (ProductCode.DRAMA_AI.equals(productCode) && "drama.storyboard.generate".equals(featureCode)) {
            return new BigDecimal("49.90");
        }
        if (ProductCode.SHORTVIDEO_MAKER.equals(productCode) && "shortvideo.render.export".equals(featureCode)) {
            return new BigDecimal("39.90");
        }
        return switch (normalizeTier(pricingTier)) {
            case "deep" -> new BigDecimal("69.90");
            case "api" -> new BigDecimal("3.30");
            case "video" -> new BigDecimal("120.00");
            case "storyboard" -> new BigDecimal("49.90");
            case "render" -> new BigDecimal("39.90");
            default -> new BigDecimal("9.90");
        };
    }

    private CreditLedgerEntrySummary ledgerEntry(
            CreditConsumeRequest request,
            CreditTransactionType transactionType,
            BigDecimal credits,
            BigDecimal balanceAfter,
            String reason
    ) {
        return ledger(
                Ids.compactUuid("credit_ledger"),
                normalizeTenant(request.tenantId()),
                normalizeUser(request.userId()),
                normalizeAgent(request.agentId()),
                request.productCode(),
                request.featureCode(),
                normalizeChannel(request.channel()),
                transactionType,
                credits.toPlainString(),
                balanceAfter.toPlainString(),
                normalizeTraceId(request.traceId()),
                reason,
                OffsetDateTime.now()
        );
    }

    /**
     * 写入真实积分流水并更新账户余额。
     *
     * <p>RESERVE、COMMIT、RELEASE 都在这里统一更新账户和流水，避免同步调用、长任务和产品互调出现
     * 多套余额计算口径。调用方必须通过冻结单状态保证幂等。</p>
     */
    private CreditLedgerEntrySummary persistLedgerEntry(
            CreditConsumeRequest request,
            CreditTransactionType transactionType,
            BigDecimal credits,
            BigDecimal balanceAfter,
            String reason
    ) {
        CreditLedgerEntrySummary entry = ledgerEntry(request, transactionType, credits, balanceAfter, reason);
        if (transactionType == CreditTransactionType.RESERVE) {
            jdbcTemplate.update(
                    """
                            update gf_credit_account
                            set available_credits = available_credits - ?,
                                frozen_credits = frozen_credits + ?,
                                updated_at = ?
                            where tenant_id = ?
                            """,
                    credits,
                    credits,
                    OffsetDateTime.now(),
                    entry.tenantId()
            );
        }
        if (transactionType == CreditTransactionType.COMMIT) {
            jdbcTemplate.update(
                    """
                            update gf_credit_account
                            set frozen_credits = frozen_credits - ?,
                                used_credits = used_credits + ?,
                                updated_at = ?
                            where tenant_id = ?
                            """,
                    credits,
                    credits,
                    OffsetDateTime.now(),
                    entry.tenantId()
            );
        }
        if (transactionType == CreditTransactionType.RELEASE) {
            jdbcTemplate.update(
                    """
                            update gf_credit_account
                            set frozen_credits = frozen_credits - ?,
                                available_credits = available_credits + ?,
                                updated_at = ?
                            where tenant_id = ?
                            """,
                    credits,
                    credits,
                    OffsetDateTime.now(),
                    entry.tenantId()
            );
        }
        jdbcTemplate.update(
                """
                        insert into gf_credit_ledger (entry_id, tenant_id, user_id, agent_id, product_code, feature_code, channel,
                                                      transaction_type, credits, balance_after, trace_id, reason, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                entry.entryId(),
                entry.tenantId(),
                entry.userId(),
                entry.agentId(),
                entry.productCode(),
                entry.featureCode(),
                entry.channel(),
                entry.transactionType().name(),
                entry.credits(),
                entry.balanceAfter(),
                entry.traceId(),
                entry.reason(),
                entry.createdAt()
        );
        return entry;
    }

    private void insertReservation(
            CreditReserveRequest request,
            String reservationId,
            CreditQuoteResult quote,
            BigDecimal balanceAfterReserve,
            CreditLedgerEntrySummary reserveEntry
    ) {
        jdbcTemplate.update(
                """
                        insert into gf_credit_reservation (reservation_id, tenant_id, user_id, agent_id, product_code,
                                                           feature_code, channel, requested_amount, pricing_tier,
                                                           reserved_credits, balance_after_reserve, status, trace_id,
                                                           business_key, reason, reserve_entry_id, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                reservationId,
                quote.tenantId(),
                normalizeUser(request.userId()),
                normalizeAgent(request.agentId()),
                request.productCode(),
                request.featureCode(),
                normalizeChannel(request.channel()),
                normalizeAmount(request.requestedAmount()),
                normalizeTier(request.pricingTier()),
                quote.totalCredits(),
                balanceAfterReserve,
                CreditReservationStatus.RESERVED.name(),
                quote.traceId(),
                normalizeOptional(request.businessKey()),
                normalizeReason(request.reason()),
                reserveEntry.entryId(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    private CreditConsumeRequest toConsumeRequest(CreditReservationRecord reservation, CreditReservationActionRequest request) {
        return new CreditConsumeRequest(
                reservation.tenantId(),
                firstNonBlank(request.userId(), reservation.userId(), "demo-user"),
                firstNonBlank(request.agentId(), reservation.agentId(), null),
                reservation.productCode(),
                reservation.featureCode(),
                firstNonBlank(request.channel(), reservation.channel(), "WEB"),
                reservation.requestedAmount(),
                reservation.pricingTier(),
                firstNonBlank(request.traceId(), reservation.traceId(), "trace_credit"),
                firstNonBlank(request.reason(), reservation.reason(), "长任务积分冻结单")
        );
    }

    private CreditReservationRecord requireReservation(String reservationId) {
        return findReservation(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("积分冻结单不存在：" + reservationId));
    }

    private CreditReservationTimeoutCandidateSummary timeoutCandidate(
            String reservationId,
            String tenantId,
            String userId,
            String agentId,
            String productCode,
            String featureCode,
            String channel,
            BigDecimal reservedCredits,
            String status,
            String traceId,
            String businessKey,
            String reason,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new CreditReservationTimeoutCandidateSummary(
                reservationId,
                tenantId,
                userId,
                agentId,
                productCode,
                featureCode,
                channel,
                reservedCredits,
                CreditReservationStatus.valueOf(status),
                traceId,
                businessKey,
                reason,
                Duration.between(updatedAt, OffsetDateTime.now()).toMinutes(),
                createdAt,
                updatedAt
        );
    }

    private Optional<CreditReservationTimeoutReleaseExecutionSummary> findTimeoutReleaseExecution(String tenantId, String idempotencyKey) {
        List<CreditReservationTimeoutReleaseExecutionSummary> rows = jdbcTemplate.query(
                """
                        select release_id, reservation_id, tenant_id, old_status, new_status, reserved_credits,
                               idempotency_key, release_entry_id, audit_id, trace_id, executed_by_user_id, executed_at
                        from gf_credit_reservation_timeout_release_execution
                        where tenant_id = ? and idempotency_key = ?
                        limit 1
                        """,
                (rs, rowNum) -> timeoutReleaseExecution(
                        rs.getString("release_id"),
                        rs.getString("reservation_id"),
                        rs.getString("tenant_id"),
                        rs.getString("old_status"),
                        rs.getString("new_status"),
                        rs.getBigDecimal("reserved_credits"),
                        rs.getString("idempotency_key"),
                        rs.getString("release_entry_id"),
                        rs.getString("audit_id"),
                        rs.getString("trace_id"),
                        rs.getString("executed_by_user_id"),
                        rs.getObject("executed_at", OffsetDateTime.class)
                ),
                tenantId,
                idempotencyKey
        );
        return rows.stream().findFirst();
    }

    private CreditReservationTimeoutReleaseExecutionSummary recordTimeoutReleaseExecution(
            CreditReservationRecord reservation,
            CreditReservationResult released,
            String idempotencyKey,
            String auditId,
            String traceId,
            String operatorUserId,
            String reason
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        String releaseId = Ids.compactUuid("credit_timeout_release");
        String releaseEntryId = released.releaseEntry() == null ? "" : released.releaseEntry().entryId();
        jdbcTemplate.update(
                """
                        insert into gf_credit_reservation_timeout_release_execution (release_id, reservation_id, tenant_id,
                                                                                     old_status, new_status, reserved_credits,
                                                                                     idempotency_key, release_entry_id, audit_id,
                                                                                     trace_id, reason, executed_by_user_id,
                                                                                     executed_at, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                releaseId,
                reservation.reservationId(),
                reservation.tenantId(),
                reservation.status().name(),
                released.status() == null ? CreditReservationStatus.RELEASED.name() : released.status().name(),
                reservation.reservedCredits(),
                idempotencyKey,
                releaseEntryId,
                auditId,
                traceId,
                truncate(reason, 512),
                operatorUserId,
                now,
                now
        );
        return timeoutReleaseExecution(
                releaseId,
                reservation.reservationId(),
                reservation.tenantId(),
                reservation.status().name(),
                released.status() == null ? CreditReservationStatus.RELEASED.name() : released.status().name(),
                reservation.reservedCredits(),
                idempotencyKey,
                releaseEntryId,
                auditId,
                traceId,
                operatorUserId,
                now
        );
    }

    private CreditReservationTimeoutReleaseExecutionSummary timeoutReleaseExecution(
            String releaseId,
            String reservationId,
            String tenantId,
            String oldStatus,
            String newStatus,
            BigDecimal reservedCredits,
            String idempotencyKey,
            String releaseEntryId,
            String auditId,
            String traceId,
            String executedByUserId,
            OffsetDateTime executedAt
    ) {
        return new CreditReservationTimeoutReleaseExecutionSummary(
                releaseId,
                reservationId,
                tenantId,
                CreditReservationStatus.valueOf(oldStatus),
                CreditReservationStatus.valueOf(newStatus),
                reservedCredits,
                idempotencyKey,
                releaseEntryId,
                auditId,
                traceId,
                executedByUserId,
                executedAt
        );
    }

    private String recordTimeoutReleaseAudit(
            CreditReservationRecord reservation,
            String operatorUserId,
            String traceId,
            String reason,
            long ageMinutes
    ) {
        String auditId = Ids.compactUuid("audit_credit_timeout");
        jdbcTemplate.update(
                """
                        insert into gf_audit_event (audit_id, tenant_id, user_id, agent_id, product_code, feature_code, channel,
                                                    risk_level, status, trace_id, decision_note, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                auditId,
                reservation.tenantId(),
                operatorUserId,
                reservation.agentId(),
                reservation.productCode(),
                reservation.featureCode(),
                reservation.channel(),
                "MEDIUM",
                "RECORDED",
                traceId,
                truncate("积分冻结超时受控释放；reservationId=" + reservation.reservationId()
                        + "，ageMinutes=" + ageMinutes
                        + "，reservedCredits=" + reservation.reservedCredits()
                        + "，reason=" + reason, 1000),
                OffsetDateTime.now()
        );
        return auditId;
    }

    private Optional<CreditReservationRecord> findReservation(String reservationId) {
        if (reservationId == null || reservationId.isBlank()) {
            return Optional.empty();
        }
        List<CreditReservationRecord> rows = jdbcTemplate.query(
                """
                        select reservation_id, tenant_id, user_id, agent_id, product_code, feature_code, channel,
                               requested_amount, pricing_tier, reserved_credits, balance_after_reserve, status,
                               trace_id, business_key, reason, reserve_entry_id, commit_entry_id, release_entry_id,
                               created_at, updated_at
                        from gf_credit_reservation
                        where reservation_id = ?
                        """,
                (rs, rowNum) -> reservationRecord(rs.getString("reservation_id"), rs.getString("tenant_id"),
                        rs.getString("user_id"), rs.getString("agent_id"), rs.getString("product_code"),
                        rs.getString("feature_code"), rs.getString("channel"), rs.getBigDecimal("requested_amount"),
                        rs.getString("pricing_tier"), rs.getBigDecimal("reserved_credits"),
                        rs.getBigDecimal("balance_after_reserve"),
                        CreditReservationStatus.valueOf(rs.getString("status")), rs.getString("trace_id"),
                        rs.getString("business_key"), rs.getString("reason"), rs.getString("reserve_entry_id"),
                        rs.getString("commit_entry_id"), rs.getString("release_entry_id"),
                        rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class)),
                reservationId.trim()
        );
        return rows.stream().findFirst();
    }

    private Optional<CreditReservationRecord> findReservationByBusinessKey(String tenantId, String businessKey) {
        List<CreditReservationRecord> rows = jdbcTemplate.query(
                """
                        select reservation_id, tenant_id, user_id, agent_id, product_code, feature_code, channel,
                               requested_amount, pricing_tier, reserved_credits, balance_after_reserve, status,
                               trace_id, business_key, reason, reserve_entry_id, commit_entry_id, release_entry_id,
                               created_at, updated_at
                        from gf_credit_reservation
                        where tenant_id = ? and business_key = ?
                        """,
                (rs, rowNum) -> reservationRecord(rs.getString("reservation_id"), rs.getString("tenant_id"),
                        rs.getString("user_id"), rs.getString("agent_id"), rs.getString("product_code"),
                        rs.getString("feature_code"), rs.getString("channel"), rs.getBigDecimal("requested_amount"),
                        rs.getString("pricing_tier"), rs.getBigDecimal("reserved_credits"),
                        rs.getBigDecimal("balance_after_reserve"),
                        CreditReservationStatus.valueOf(rs.getString("status")), rs.getString("trace_id"),
                        rs.getString("business_key"), rs.getString("reason"), rs.getString("reserve_entry_id"),
                        rs.getString("commit_entry_id"), rs.getString("release_entry_id"),
                        rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class)),
                tenantId,
                businessKey
        );
        return rows.stream().findFirst();
    }

    private CreditReservationResult toReservationResult(
            CreditReservationRecord reservation,
            boolean allowed,
            String decisionCode,
            String message
    ) {
        CreditQuoteResult quote = new CreditQuoteResult(
                Ids.compactUuid("quote"),
                reservation.tenantId(),
                reservation.productCode(),
                reservation.featureCode(),
                reservation.channel(),
                reservation.requestedAmount(),
                reservation.reservedCredits().divide(reservation.requestedAmount(), 2, RoundingMode.HALF_UP),
                reservation.reservedCredits(),
                reservation.reservedCredits().multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP),
                reservation.reservedCredits().multiply(new BigDecimal("0.025")).setScale(2, RoundingMode.HALF_UP),
                reservation.balanceAfterReserve().add(reservation.reservedCredits()),
                true,
                "reservation=" + reservation.reservationId() + ", tier=" + normalizeTier(reservation.pricingTier()),
                reservation.traceId()
        );
        return new CreditReservationResult(
                reservation.reservationId(),
                reservation.status(),
                allowed,
                decisionCode,
                message,
                quote,
                reservation.reservedCredits(),
                reservation.status() == CreditReservationStatus.RELEASED
                        ? reservation.balanceAfterReserve().add(reservation.reservedCredits())
                        : reservation.balanceAfterReserve(),
                findCreditLedgerEntry(reservation.reserveEntryId()).orElse(null),
                findCreditLedgerEntry(reservation.commitEntryId()).orElse(null),
                findCreditLedgerEntry(reservation.releaseEntryId()).orElse(null)
        );
    }

    private Optional<CreditLedgerEntrySummary> findCreditLedgerEntry(String entryId) {
        if (entryId == null || entryId.isBlank()) {
            return Optional.empty();
        }
        List<CreditLedgerEntrySummary> entries = jdbcTemplate.query(
                """
                        select entry_id, tenant_id, user_id, agent_id, product_code, feature_code, channel,
                               transaction_type, credits, balance_after, trace_id, reason, created_at
                        from gf_credit_ledger
                        where entry_id = ?
                        """,
                (rs, rowNum) -> new CreditLedgerEntrySummary(
                        rs.getString("entry_id"),
                        rs.getString("tenant_id"),
                        rs.getString("user_id"),
                        rs.getString("agent_id"),
                        rs.getString("product_code"),
                        rs.getString("feature_code"),
                        rs.getString("channel"),
                        CreditTransactionType.valueOf(rs.getString("transaction_type")),
                        rs.getBigDecimal("credits"),
                        rs.getBigDecimal("balance_after"),
                        rs.getString("trace_id"),
                        rs.getString("reason"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                entryId
        );
        return entries.stream().findFirst();
    }

    private CreditReservationRecord reservationRecord(
            String reservationId,
            String tenantId,
            String userId,
            String agentId,
            String productCode,
            String featureCode,
            String channel,
            BigDecimal requestedAmount,
            String pricingTier,
            BigDecimal reservedCredits,
            BigDecimal balanceAfterReserve,
            CreditReservationStatus status,
            String traceId,
            String businessKey,
            String reason,
            String reserveEntryId,
            String commitEntryId,
            String releaseEntryId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new CreditReservationRecord(reservationId, tenantId, userId, agentId, productCode, featureCode,
                channel, requestedAmount, pricingTier, reservedCredits, balanceAfterReserve, status, traceId,
                businessKey, reason, reserveEntryId, commitEntryId, releaseEntryId, createdAt, updatedAt);
    }

    private CreditLedgerEntrySummary ledger(
            String entryId,
            String tenantId,
            String userId,
            String agentId,
            String productCode,
            String featureCode,
            String channel,
            CreditTransactionType transactionType,
            String credits,
            String balanceAfter,
            String traceId,
            String reason,
            OffsetDateTime createdAt
    ) {
        return new CreditLedgerEntrySummary(
                entryId,
                tenantId,
                userId,
                agentId,
                productCode,
                featureCode,
                channel,
                transactionType,
                new BigDecimal(credits),
                new BigDecimal(balanceAfter),
                traceId,
                reason,
                createdAt
        );
    }

    private BigDecimal normalizeAmount(BigDecimal requestedAmount) {
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return requestedAmount;
    }

    private String normalizeTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return DEFAULT_TENANT;
        }
        return tenantId.trim();
    }

    private String normalizeUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return "unknown-user";
        }
        return userId.trim();
    }

    private String normalizeAgent(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return null;
        }
        return agentId.trim();
    }

    private String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return "WEB";
        }
        return channel.trim().toUpperCase();
    }

    private String normalizeTier(String pricingTier) {
        if (pricingTier == null || pricingTier.isBlank()) {
            return "standard";
        }
        return pricingTier.trim().toLowerCase();
    }

    private String normalizeTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return Ids.compactUuid("trace_credit");
        }
        return traceId.trim();
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "商业化能力调用";
        }
        return reason.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record PricingRule(
            BigDecimal unitCredits,
            BigDecimal retailCnyPerCredit,
            BigDecimal providerCostCnyPerCredit,
            String description
    ) {
    }

    private record CreditReservationRecord(
            String reservationId,
            String tenantId,
            String userId,
            String agentId,
            String productCode,
            String featureCode,
            String channel,
            BigDecimal requestedAmount,
            String pricingTier,
            BigDecimal reservedCredits,
            BigDecimal balanceAfterReserve,
            CreditReservationStatus status,
            String traceId,
            String businessKey,
            String reason,
            String reserveEntryId,
            String commitEntryId,
            String releaseEntryId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }
}
