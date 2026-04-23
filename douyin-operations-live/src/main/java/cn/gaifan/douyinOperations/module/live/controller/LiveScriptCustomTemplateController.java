package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptTemplate;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptTemplateRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.vo.SaveFromSessionVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 话术模板管理 Controller（从场次创建自定义模板）
 * 与 LiveScriptTemplateController / LiveTemplateController 互补
 * 路径: /api/v1/live/script-template
 */
@RestController
@RequestMapping("/api/v1/live/script-template")
@Tag(name = "话术自定义模板 / Script Custom Template", description = "从直播场次话术创建自定义模板（需登录）")
public class LiveScriptCustomTemplateController {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptCustomTemplateController.class);
    private static final String PRODUCT_NAME_PLACEHOLDER = "{产品名}";

    @Resource
    private LiveScriptRepository liveScriptRepository;
    @Resource
    private LiveProductRepository liveProductRepository;
    @Resource
    private LiveScriptTemplateRepository templateRepository;
    @Resource
    private LiveSessionRepository liveSessionRepository;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 从场次话术创建自定义模板
     * 1. 加载场次下所有话术
     * 2. 剥离产品专属内容，保留结构（scriptType, sequenceNo, durationLimitSec）
     * 3. 以 JSON 形式保存到 template_content 字段
     */
    @PostMapping("/save-from-session")
    @Operation(
            summary = "从场次话术创建模板 / Save Template from Session",
            description = "将指定场次的话术结构保存为可复用模板，产品名替换为占位符，结构存入 template_content（需登录）")
    public RESTResult<Long> saveFromSession(HttpServletRequest request,
            @Valid @RequestBody SaveFromSessionVO vo) {
        Long userId = requireUserId(request);
        String roleCode = AuthTokenFilter.getRoleCode(request);

        // 校验场次归属（管理员跳过）
        LiveSession session;
        if ("admin".equals(roleCode)) {
            session = liveSessionRepository.findByIdAndDeleted(vo.getSessionId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        } else {
            session = liveSessionRepository.findByIdAndUserIdAndDeleted(vo.getSessionId(), userId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "无权访问该直播场次"));
        }

        // 获取场次下所有话术
        List<LiveScript> scripts = liveScriptRepository
                .findBySessionIdAndDeletedOrderBySequenceNoAsc(vo.getSessionId(), 0);
        if (scripts.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "该场次下没有话术");
        }

        // 构建 productId -> productName 映射
        Map<Long, String> productNameMap = buildProductNameMap(vo.getSessionId());

        // 构建模板结构 JSON
        List<Map<String, Object>> templateStructure = scripts.stream().map(script -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("scriptType", script.getScriptType() != null ? script.getScriptType() : "custom");
            item.put("sequenceNo", script.getSequenceNo());
            item.put("durationLimitSec", script.getDurationLimitSec());
            item.put("requirement", script.getRequirement());
            // 替换产品名为占位符
            String content = stripProductContent(script.getScriptContent(), script.getProductId(), productNameMap);
            item.put("content", content);
            item.put("style", script.getStyle());
            return item;
        }).collect(Collectors.toList());

        String templateContentJson;
        try {
            templateContentJson = objectMapper.writeValueAsString(templateStructure);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "序列化模板内容失败");
        }

        // 保存为一条模板记录
        LiveScriptTemplate template = new LiveScriptTemplate();
        template.setTemplateName(vo.getTemplateName());
        template.setScriptType("composite"); // 复合模板，包含多段话术结构
        template.setCategory(session.getScriptStyle());
        template.setContent(vo.getDescription() != null ? vo.getDescription() : "从场次【" + session.getLiveTitle() + "】创建的模板");
        template.setTemplateContent(templateContentJson);
        template.setSourceSessionId(vo.getSessionId());
        template.setOwnerId(userId);
        template.setAutoCollected(0);
        template.setStatus(1);

        // 设置 variables 占位符信息
        if (!productNameMap.isEmpty()) {
            template.setVariables(PRODUCT_NAME_PLACEHOLDER);
        }

        template = templateRepository.save(template);

        log.info("从场次创建自定义模板: userId={}, sessionId={}, templateId={}, templateName={}, scriptCount={}",
                userId, vo.getSessionId(), template.getId(), vo.getTemplateName(), scripts.size());

        return withTraceId(RESTResult.addSuccess(template.getId()));
    }

    /**
     * 构建 productId -> productName 映射
     */
    private Map<Long, String> buildProductNameMap(Long sessionId) {
        List<LiveProduct> products = liveProductRepository.findBySessionIdOrderByPositionAscIdAsc(sessionId);
        Map<Long, String> map = new HashMap<>();
        for (LiveProduct p : products) {
            if (p.getProductId() != null && p.getProductName() != null && !p.getProductName().isBlank()) {
                map.put(p.getProductId(), p.getProductName());
            }
        }
        return map;
    }

    /**
     * 将话术内容中的产品名替换为占位符，剥离产品专属信息
     */
    private String stripProductContent(String content, Long productId, Map<Long, String> productNameMap) {
        if (content == null || content.isBlank()) {
            return content;
        }
        if (productId == null) {
            return content;
        }
        String productName = productNameMap.get(productId);
        if (productName == null || productName.isBlank()) {
            return content;
        }
        return content.replace(productName, PRODUCT_NAME_PLACEHOLDER);
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }

    private <T> RESTResult<T> withTraceId(RESTResult<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
