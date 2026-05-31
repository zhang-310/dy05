package cn.gaifan.douyinOperations.module.platform.controller;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.contract.credit.*;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.module.platform.credit.CreditLedgerService;
import cn.gaifan.douyinOperations.module.platform.identity.CommercialIdentityBridge;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
@Tag(name = "积分治理 / Credits")
public class CreditController {

    private final CreditLedgerService creditLedgerService;

    public CreditController(CreditLedgerService creditLedgerService) {
        this.creditLedgerService = creditLedgerService;
    }

    @GetMapping("/overview")
    @Operation(summary = "积分治理总览")
    public RESTResult<CreditGovernanceOverview> overview(
            @RequestParam(name = "tenantId", required = false) String tenantId) {
        return RESTResult.success(creditLedgerService.overview(resolveTenant(tenantId)));
    }

    @GetMapping("/account")
    @Operation(summary = "积分账户")
    public RESTResult<CreditAccountSummary> account(
            @RequestParam(name = "tenantId", required = false) String tenantId) {
        return RESTResult.success(creditLedgerService.account(resolveTenant(tenantId)));
    }

    @PostMapping("/quote")
    @Operation(summary = "积分报价")
    public RESTResult<CreditQuoteResult> quote(@RequestBody CreditQuoteRequest request) {
        return RESTResult.success(creditLedgerService.quote(withIdentity(request)));
    }

    @PostMapping("/consume")
    @Operation(summary = "积分消费")
    public RESTResult<CreditConsumeResult> consume(@RequestBody CreditConsumeRequest request) {
        return RESTResult.success(creditLedgerService.consume(withIdentity(request)));
    }

    @PostMapping("/reserve")
    @Operation(summary = "积分冻结")
    public RESTResult<CreditReservationResult> reserve(@RequestBody CreditReserveRequest request) {
        return RESTResult.success(creditLedgerService.reserve(withIdentity(request)));
    }

    @PostMapping("/reservations/{reservationId}/commit")
    public RESTResult<CreditReservationResult> commit(
            @PathVariable String reservationId,
            @RequestBody CreditReservationActionRequest request) {
        return RESTResult.success(creditLedgerService.commit(reservationId, withIdentity(request)));
    }

    @PostMapping("/reservations/{reservationId}/release")
    public RESTResult<CreditReservationResult> release(
            @PathVariable String reservationId,
            @RequestBody CreditReservationActionRequest request) {
        return RESTResult.success(creditLedgerService.release(reservationId, withIdentity(request)));
    }

    private String resolveTenant(String tenantId) {
        IdentityContext ctx = RequestIdentityHolder.current();
        if (ctx != null && ctx.authenticated()) {
            return CommercialIdentityBridge.resolveTenantId(ctx);
        }
        return tenantId != null && !tenantId.isBlank() ? tenantId : "demo-tenant";
    }

    private CreditQuoteRequest withIdentity(CreditQuoteRequest request) {
        IdentityContext ctx = RequestIdentityHolder.current();
        return new CreditQuoteRequest(
                resolveTenant(request.tenantId()),
                CommercialIdentityBridge.resolveUserId(ctx) != null ? CommercialIdentityBridge.resolveUserId(ctx) : request.userId(),
                request.agentId(),
                request.productCode(),
                request.featureCode(),
                ctx != null && ctx.channel() != null ? ctx.channel() : request.channel(),
                request.requestedAmount(),
                request.pricingTier(),
                CommercialIdentityBridge.resolveTraceId(ctx)
        );
    }

    private CreditConsumeRequest withIdentity(CreditConsumeRequest request) {
        IdentityContext ctx = RequestIdentityHolder.current();
        return new CreditConsumeRequest(
                resolveTenant(request.tenantId()),
                CommercialIdentityBridge.resolveUserId(ctx) != null ? CommercialIdentityBridge.resolveUserId(ctx) : request.userId(),
                request.agentId(),
                request.productCode(),
                request.featureCode(),
                ctx != null && ctx.channel() != null ? ctx.channel() : request.channel(),
                request.requestedAmount(),
                request.pricingTier(),
                CommercialIdentityBridge.resolveTraceId(ctx),
                request.reason()
        );
    }

    private CreditReserveRequest withIdentity(CreditReserveRequest request) {
        IdentityContext ctx = RequestIdentityHolder.current();
        return new CreditReserveRequest(
                resolveTenant(request.tenantId()),
                CommercialIdentityBridge.resolveUserId(ctx) != null ? CommercialIdentityBridge.resolveUserId(ctx) : request.userId(),
                request.agentId(),
                request.productCode(),
                request.featureCode(),
                ctx != null && ctx.channel() != null ? ctx.channel() : request.channel(),
                request.requestedAmount(),
                request.pricingTier(),
                CommercialIdentityBridge.resolveTraceId(ctx),
                request.businessKey(),
                request.reason()
        );
    }

    private CreditReservationActionRequest withIdentity(CreditReservationActionRequest request) {
        IdentityContext ctx = RequestIdentityHolder.current();
        return new CreditReservationActionRequest(
                resolveTenant(request.tenantId()),
                CommercialIdentityBridge.resolveUserId(ctx) != null ? CommercialIdentityBridge.resolveUserId(ctx) : request.userId(),
                request.agentId(),
                ctx != null && ctx.channel() != null ? ctx.channel() : request.channel(),
                CommercialIdentityBridge.resolveTraceId(ctx),
                request.reason()
        );
    }
}
