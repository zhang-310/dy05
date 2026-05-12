package cn.gaifan.douyinOperations.module.wecom.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.wecom.entity.*;
import cn.gaifan.douyinOperations.module.wecom.repository.*;
import cn.gaifan.douyinOperations.module.wecom.vo.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.net.URI;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WecomServiceImpl implements cn.gaifan.douyinOperations.module.wecom.service.WecomService {

    private static final Set<String> ROBOT_SORTABLE = Set.of("id", "ownerId", "robotName", "status", "createTime");
    private static final Set<String> LOG_SORTABLE = Set.of("id", "ownerId", "robotId", "status", "sendTime", "createTime");

    // P1-1: SSRF 防护 - 企业微信 webhook URL 白名单
    private static final Set<String> ALLOWED_WEBHOOK_HOSTS = Set.of(
        "qyapi.weixin.qq.com",
        "qyapi.wechat.com"
    );

    @Resource
    private WcRobotConfigRepository robotConfigRepository;
    @Resource
    private WcPushRuleRepository pushRuleRepository;
    @Resource
    private WcMessageLogRepository messageLogRepository;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private ObjectMapper objectMapper;

    // ==================== 机器人管理 ====================

    public PageResultVO<WcRobotConfigVO> searchRobots(WcRobotSearchVO vo) {
        vo.validateParams();
        String sortName = ROBOT_SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<WcRobotConfig> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getOwnerId() != null) predicates.add(cb.equal(root.get("ownerId"), vo.getOwnerId()));
            if (vo.getRobotType() != null && !vo.getRobotType().isBlank()) {
                predicates.add(cb.equal(root.get("robotType"), vo.getRobotType().trim()));
            }
            if (vo.getStatus() != null) predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<WcRobotConfig> page = robotConfigRepository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toRobotVO).collect(Collectors.toList()),
                vo.getPage(), vo.getRows());
    }

    @Cacheable(value = "wecom:robot", key = "#id", unless = "#result == null")
    public WcRobotConfigVO getRobotById(Long id) {
        return toRobotVO(robotConfigRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "机器人配置不存在")));
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "wecom:robot", key = "#result")
    public long saveRobot(WcRobotConfigSaveVO vo) {
        // P1-1: SSRF 防护 - 验证 webhook URL
        validateWebhookUrl(vo.getWebhookUrl());

        WcRobotConfig entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = robotConfigRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "机器人配置不存在"));
        } else {
            entity = new WcRobotConfig();
            entity.setOwnerId(vo.getOwnerId());
        }
        entity.setRobotName(vo.getRobotName());
        entity.setWebhookUrl(vo.getWebhookUrl());
        if (vo.getRobotType() != null) entity.setRobotType(vo.getRobotType());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        if (vo.getDescription() != null) entity.setDescription(vo.getDescription());
        return robotConfigRepository.save(entity).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "wecom:robot", key = "#id")
    public void deleteRobot(Long id) {
        WcRobotConfig entity = robotConfigRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "机器人配置不存在"));
        entity.setDeleted(1);
        robotConfigRepository.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "wecom:robot", key = "#id")
    public void updateRobotStatus(Long id, Integer status) {
        robotConfigRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "机器人配置不存在"));
        robotConfigRepository.updateStatus(id, status);
    }

    // ==================== 推送规则 ====================

    @Cacheable(value = "wecom:rules", key = "#ownerId", unless = "#result == null || #result.isEmpty()")
    public List<WcPushRuleVO> listRules(Long ownerId) {
        return pushRuleRepository.findByOwnerIdAndDeleted(ownerId, 0)
                .stream().map(this::toRuleVO).collect(Collectors.toList());
    }

    public WcPushRuleVO getRuleById(Long id) {
        return toRuleVO(pushRuleRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "推送规则不存在")));
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "wecom:rules", key = "#vo.ownerId")
    public long saveRule(WcPushRuleSaveVO vo) {
        WcPushRule entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = pushRuleRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "推送规则不存在"));
        } else {
            entity = new WcPushRule();
            entity.setOwnerId(vo.getOwnerId());
            entity.setRobotId(vo.getRobotId());
        }
        entity.setRuleName(vo.getRuleName());
        entity.setTriggerType(vo.getTriggerType());
        entity.setTriggerConfig(vo.getTriggerConfig());
        entity.setMessageTemplate(vo.getMessageTemplate());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        return pushRuleRepository.save(entity).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        WcPushRule entity = pushRuleRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "推送规则不存在"));
        entity.setDeleted(1);
        pushRuleRepository.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRuleStatus(Long id, Integer status) {
        pushRuleRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "推送规则不存在"));
        pushRuleRepository.updateStatus(id, status);
    }

    // ==================== 消息日志 ====================

    public PageResultVO<WcMessageLogVO> searchLogs(WcMessageLogSearchVO vo) {
        vo.validateParams();
        String sortName = LOG_SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "sendTime";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by(Sort.Direction.DESC, sortName));

        Specification<WcMessageLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (vo.getOwnerId() != null) predicates.add(cb.equal(root.get("ownerId"), vo.getOwnerId()));
            if (vo.getRobotId() != null) predicates.add(cb.equal(root.get("robotId"), vo.getRobotId()));
            if (vo.getRuleId() != null) predicates.add(cb.equal(root.get("ruleId"), vo.getRuleId()));
            if (vo.getStatus() != null) predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<WcMessageLog> page = messageLogRepository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toLogVO).collect(Collectors.toList()),
                vo.getPage(), vo.getRows());
    }

    // ==================== 发送消息 ====================

    @Transactional(rollbackFor = Exception.class)
    @Retry(name = "wecomPush")
    public void sendMessage(WcSendMessageVO vo, Long ownerId) {
        WcRobotConfig robot = robotConfigRepository.findByIdAndDeleted(vo.getRobotId(), 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "机器人配置不存在"));
        if (robot.getStatus() != 1) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "机器人已禁用");
        }

        // P1-1: SSRF 防护 - 验证 webhook URL（双重保险）
        validateWebhookUrl(robot.getWebhookUrl());

        WcMessageLog log = new WcMessageLog();
        log.setOwnerId(ownerId);
        log.setRobotId(vo.getRobotId());
        log.setRuleId(vo.getRuleId());
        log.setMessageType(vo.getMessageType() != null ? vo.getMessageType() : "text");
        log.setMessageContent(vo.getMessageContent());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = buildWecomPayload(vo.getMessageType(), vo.getMessageContent());
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(robot.getWebhookUrl(), request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.setStatus(1);
            } else {
                log.setStatus(0);
                log.setErrorMessage("HTTP " + response.getStatusCodeValue());
            }
        } catch (Exception e) {
            log.setStatus(0);
            log.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 512)) : "未知错误");
        }

        messageLogRepository.save(log);

        if (vo.getRuleId() != null) {
            pushRuleRepository.updateLastTriggerTime(vo.getRuleId(), new Timestamp(System.currentTimeMillis()));
        }
    }

    private String buildWecomPayload(String messageType, String content) {
        // P1-2: JSON 注入防护 - 使用 Jackson ObjectMapper 替代手动转义
        String type = messageType != null ? messageType : "text";

        Map<String, Object> payload = new HashMap<>();
        payload.put("msgtype", type);

        if ("markdown".equals(type)) {
            payload.put("markdown", Map.of("content", content));
        } else {
            payload.put("text", Map.of("content", content));
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "消息序列化失败: " + e.getMessage());
        }
    }

    // P1-2: 移除不安全的手动 JSON 转义方法（已被 Jackson 替代）
    // private String escapeJson(String s) { ... }

    // P1-1: SSRF 防护 - 验证企业微信 webhook URL
    private void validateWebhookUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "Webhook URL 不能为空");
        }

        try {
            URI uri = new URI(url);

            // 检查协议必须是 HTTPS
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "Webhook URL 必须使用 HTTPS 协议");
            }

            // 检查域名必须在白名单中
            String host = uri.getHost();
            if (host == null || !ALLOWED_WEBHOOK_HOSTS.contains(host.toLowerCase())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                    "Webhook URL 域名不在白名单中，仅允许: " + String.join(", ", ALLOWED_WEBHOOK_HOSTS));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "Webhook URL 格式错误: " + e.getMessage());
        }
    }

    // ==================== toVO ====================

    private WcRobotConfigVO toRobotVO(WcRobotConfig e) {
        WcRobotConfigVO vo = new WcRobotConfigVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setRobotName(e.getRobotName());
        vo.setWebhookUrl(e.getWebhookUrl());
        vo.setRobotType(e.getRobotType());
        vo.setStatus(e.getStatus());
        vo.setDescription(e.getDescription());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    private WcPushRuleVO toRuleVO(WcPushRule e) {
        WcPushRuleVO vo = new WcPushRuleVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setRobotId(e.getRobotId());
        vo.setRuleName(e.getRuleName());
        vo.setTriggerType(e.getTriggerType());
        vo.setTriggerConfig(e.getTriggerConfig());
        vo.setMessageTemplate(e.getMessageTemplate());
        vo.setStatus(e.getStatus());
        vo.setLastTriggerTime(e.getLastTriggerTime());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    private WcMessageLogVO toLogVO(WcMessageLog e) {
        WcMessageLogVO vo = new WcMessageLogVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setRobotId(e.getRobotId());
        vo.setRuleId(e.getRuleId());
        vo.setMessageType(e.getMessageType());
        vo.setMessageContent(e.getMessageContent());
        vo.setStatus(e.getStatus());
        vo.setErrorMessage(e.getErrorMessage());
        vo.setSendTime(e.getSendTime());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }
}
