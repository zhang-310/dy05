package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.util.RequestRoleResolver;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptTemplate;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptTemplateRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 直播话术模板服务实现
 */
@Service
public class LiveTemplateServiceImpl implements LiveTemplateService {

    private static final Logger log = LoggerFactory.getLogger(LiveTemplateServiceImpl.class);

    private static final String PRODUCT_NAME_PLACEHOLDER = "{产品名}";

    @Resource
    private LiveScriptRepository liveScriptRepository;
    @Resource
    private LiveProductRepository liveProductRepository;
    @Resource
    private LiveScriptTemplateRepository templateRepository;
    @Resource
    private LiveSessionRepository liveSessionRepository;

    // P0-3: 保存模板时清除缓存
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "live:template", allEntries = true)
    public Long saveSessionAsTemplate(Long sessionId, String templateName, List<String> scriptTypes, Long userId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "场次 ID 无效");
        }
        if (templateName == null || templateName.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "模板名称不能为空");
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        // 校验场次归属（管理员跳过）
        LiveSession session;
        if (RequestRoleResolver.isAdmin()) {
            session = liveSessionRepository.findByIdAndDeleted(sessionId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        } else {
            session = liveSessionRepository.findByIdAndUserIdAndDeleted(sessionId, userId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "无权访问该直播场次"));
        }

        // 获取场次下所有话术
        List<LiveScript> scripts = liveScriptRepository.findBySessionIdAndDeleted(sessionId, 0);
        if (scripts.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "该场次下没有话术");
        }

        // 按 scriptTypes 过滤
        if (scriptTypes != null && !scriptTypes.isEmpty()) {
            Set<String> typeSet = new HashSet<>(scriptTypes);
            scripts = scripts.stream()
                    .filter(s -> s.getScriptType() != null && typeSet.contains(s.getScriptType()))
                    .collect(Collectors.toList());
        }
        if (scripts.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "过滤后没有可保存的话术");
        }

        // 按 sequenceNo 排序
        scripts.sort(Comparator.comparing(LiveScript::getSequenceNo, Comparator.nullsLast(Comparator.naturalOrder())));

        // 构建 productId -> productName 映射（用于占位符替换）
        Map<Long, String> productNameMap = buildProductNameMap(sessionId);

        // 创建模板记录
        Long firstTemplateId = null;
        List<LiveScriptTemplate> templates = new ArrayList<>();
        for (LiveScript script : scripts) {
            LiveScriptTemplate template = new LiveScriptTemplate();
            template.setTemplateName(templateName);
            template.setScriptType(script.getScriptType() != null ? script.getScriptType() : "custom");
            template.setContent(replaceProductNames(script.getScriptContent(), script.getProductId(), productNameMap));
            template.setSourceScriptId(script.getId());
            template.setSourceSessionId(sessionId);
            template.setOwnerId(userId);
            template.setAutoCollected(0);
            template.setStatus(1);

            // 设置 variables 字段，标记占位符信息
            if (script.getProductId() != null && productNameMap.containsKey(script.getProductId())) {
                template.setVariables(PRODUCT_NAME_PLACEHOLDER);
            }

            // 继承效果分数（如果有的话）
            if (script.getEffectivenessScore() != null) {
                template.setEffectivenessScore(script.getEffectivenessScore());
            }

            templates.add(template);
        }

        List<LiveScriptTemplate> saved = templateRepository.saveAll(templates);
        if (!saved.isEmpty()) {
            firstTemplateId = saved.get(0).getId();
        }

        log.info("从场次创建模板: userId={}, sessionId={}, templateName={}, 模板数量={}",
                userId, sessionId, templateName, saved.size());

        return firstTemplateId;
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
     * 将话术内容中的产品名替换为占位符
     */
    private String replaceProductNames(String content, Long productId, Map<Long, String> productNameMap) {
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
        // 替换产品名为占位符
        return content.replace(productName, PRODUCT_NAME_PLACEHOLDER);
    }
}
