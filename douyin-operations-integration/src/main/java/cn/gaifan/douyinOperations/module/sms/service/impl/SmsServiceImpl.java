package cn.gaifan.douyinOperations.module.sms.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.sms.entity.*;
import cn.gaifan.douyinOperations.module.sms.repository.*;
import cn.gaifan.douyinOperations.module.sms.service.SmsService;
import cn.gaifan.douyinOperations.module.sms.vo.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SmsServiceImpl implements SmsService {

    private static final Set<String> PROVIDER_SORTABLE = Set.of("id", "ownerId", "status", "isDefault", "createTime");
    private static final Set<String> TEMPLATE_SORTABLE = Set.of("id", "ownerId", "status", "templateType", "createTime");
    private static final Set<String> LOG_SORTABLE = Set.of("id", "ownerId", "status", "createTime");

    @Resource
    private SmsProviderConfigRepository providerConfigRepository;
    @Resource
    private SmsTemplateRepository templateRepository;
    @Resource
    private SmsSendLogRepository sendLogRepository;
    @Resource
    private SmsVerificationCodeRepository verificationCodeRepository;

    // ==================== 服务商配置 ====================

    public PageResultVO<SmsProviderConfigVO> searchProviderConfigs(SmsProviderConfigSearchVO vo) {
        vo.validateParams();
        String sortName = PROVIDER_SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "createTime";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<SmsProviderConfig> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getOwnerId() != null && vo.getOwnerId() > 0) {
                predicates.add(cb.equal(root.get("ownerId"), vo.getOwnerId()));
            }
            if (vo.getProviderCode() != null && !vo.getProviderCode().isBlank()) {
                predicates.add(cb.equal(root.get("providerCode"), vo.getProviderCode().trim()));
            }
            if (vo.getStatus() != null) predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            if (vo.getIsDefault() != null) predicates.add(cb.equal(root.get("isDefault"), vo.getIsDefault()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SmsProviderConfig> page = providerConfigRepository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toProviderConfigVO).collect(Collectors.toList()),
                vo.getPage(), vo.getRows());
    }

    @Cacheable(value = "sms:provider", key = "#id", unless = "#result == null")
    public SmsProviderConfigVO getProviderConfigById(Long id) {
        return toProviderConfigVO(providerConfigRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "短信服务商配置不存在")));
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "sms:provider", key = "#result")
    public long saveProviderConfig(SmsProviderConfigSaveVO vo) {
        SmsProviderConfig entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = providerConfigRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "短信服务商配置不存在"));
        } else {
            entity = new SmsProviderConfig();
            entity.setOwnerId(vo.getOwnerId());
            entity.setProviderCode(vo.getProviderCode());
        }
        entity.setProviderName(vo.getProviderName());
        entity.setApiKey(vo.getApiKey());
        entity.setApiSecret(vo.getApiSecret());
        if (vo.getAppId() != null) entity.setAppId(vo.getAppId());
        if (vo.getSignName() != null) entity.setSignName(vo.getSignName());
        if (vo.getRegion() != null) entity.setRegion(vo.getRegion());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        if (vo.getDailyQuota() != null) entity.setDailyQuota(vo.getDailyQuota());
        return providerConfigRepository.save(entity).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "sms:provider", key = "#id")
    public void deleteProviderConfig(Long id) {
        SmsProviderConfig entity = providerConfigRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "短信服务商配置不存在"));
        entity.setDeleted(1);
        providerConfigRepository.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "sms:provider", key = "#id")
    public void updateProviderConfigStatus(Long id, Integer status) {
        providerConfigRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "短信服务商配置不存在"));
        providerConfigRepository.updateStatus(id, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefaultProviderConfig(Long id) {
        SmsProviderConfig entity = providerConfigRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "短信服务商配置不存在"));
        providerConfigRepository.clearDefaultForOwner(entity.getOwnerId());
        providerConfigRepository.setAsDefault(id);
    }

    // ==================== 短信模板 ====================

    public PageResultVO<SmsTemplateVO> searchTemplates(SmsTemplateSearchVO vo) {
        vo.validateParams();
        String sortName = TEMPLATE_SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "createTime";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<SmsTemplate> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getOwnerId() != null && vo.getOwnerId() > 0) {
                predicates.add(cb.equal(root.get("ownerId"), vo.getOwnerId()));
            }
            if (vo.getTemplateType() != null && !vo.getTemplateType().isBlank()) {
                predicates.add(cb.equal(root.get("templateType"), vo.getTemplateType().trim()));
            }
            if (vo.getStatus() != null) predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
                String keyword = "%" + vo.getKeyword().trim() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("templateCode"), keyword),
                    cb.like(root.get("templateName"), keyword)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SmsTemplate> page = templateRepository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toTemplateVO).collect(Collectors.toList()),
                vo.getPage(), vo.getRows());
    }

    @Cacheable(value = "sms:template", key = "#id", unless = "#result == null")
    public SmsTemplateVO getTemplateById(Long id) {
        return toTemplateVO(templateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.SMS_TEMPLATE_NOT_FOUND, "短信模板不存在")));
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "sms:template", key = "#result")
    public long saveTemplate(SmsTemplateSaveVO vo) {
        SmsTemplate entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = templateRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SMS_TEMPLATE_NOT_FOUND, "短信模板不存在"));
        } else {
            entity = new SmsTemplate();
            entity.setOwnerId(vo.getOwnerId());
            entity.setTemplateCode(vo.getTemplateCode());
        }
        entity.setTemplateName(vo.getTemplateName());
        entity.setContent(vo.getContent());
        entity.setProviderCode(vo.getProviderCode());
        if (vo.getProviderTemplateId() != null) entity.setProviderTemplateId(vo.getProviderTemplateId());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        if (vo.getTemplateType() != null) entity.setTemplateType(vo.getTemplateType());
        if (vo.getRemark() != null) entity.setRemark(vo.getRemark());
        return templateRepository.save(entity).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "sms:template", key = "#id")
    public void deleteTemplate(Long id) {
        SmsTemplate entity = templateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.SMS_TEMPLATE_NOT_FOUND, "短信模板不存在"));
        entity.setDeleted(1);
        templateRepository.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "sms:template", key = "#id")
    public void updateTemplateStatus(Long id, Integer status) {
        templateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.SMS_TEMPLATE_NOT_FOUND, "短信模板不存在"));
        templateRepository.updateStatus(id, status);
    }

    // ==================== 短信发送日志 ====================

    public PageResultVO<SmsSendLogVO> searchSendLogs(SmsSendLogSearchVO vo) {
        vo.validateParams();
        String sortName = LOG_SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "createTime";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<SmsSendLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (vo.getOwnerId() != null && vo.getOwnerId() > 0) {
                predicates.add(cb.equal(root.get("ownerId"), vo.getOwnerId()));
            }
            if (vo.getPhoneNumber() != null && !vo.getPhoneNumber().isBlank()) {
                predicates.add(cb.equal(root.get("phoneNumber"), vo.getPhoneNumber().trim()));
            }
            if (vo.getStatus() != null && !vo.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), vo.getStatus().trim()));
            }
            if (vo.getBizType() != null && !vo.getBizType().isBlank()) {
                predicates.add(cb.equal(root.get("bizType"), vo.getBizType().trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SmsSendLog> page = sendLogRepository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toSendLogVO).collect(Collectors.toList()),
                vo.getPage(), vo.getRows());
    }

    public SmsSendLogVO getSendLogById(Long id) {
        return toSendLogVO(sendLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "发送日志不存在")));
    }

    // ==================== 短信验证码 ====================

    @Transactional(rollbackFor = Exception.class)
    public void sendVerificationCode(SmsVerificationCodeSendVO vo) {
        // P1-1: 短信发送频率限制 - 同一手机号 1 分钟内只能发送 1 次
        String phoneNumber = vo.getPhoneNumber();
        String bizType = vo.getBizType();

        Optional<SmsVerificationCode> recent = verificationCodeRepository
                .findTopByPhoneNumberAndBizTypeOrderByCreatedAtDesc(phoneNumber, bizType);

        if (recent.isPresent()) {
            SmsVerificationCode lastCode = recent.get();
            long timeSinceLastSend = System.currentTimeMillis() - lastCode.getCreatedAt().getTime();
            long oneMinuteMs = 60 * 1000;

            if (timeSinceLastSend < oneMinuteMs) {
                long remainingSeconds = (oneMinuteMs - timeSinceLastSend) / 1000;
                throw new BusinessException(ErrorCode.RATE_LIMIT,
                    "发送过于频繁，请 " + remainingSeconds + " 秒后再试");
            }
        }

        // 生成 6 位验证码
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 计算过期时间（5分钟）
        long expiryTime = System.currentTimeMillis() + 5 * 60 * 1000;
        Timestamp expiresAt = new Timestamp(expiryTime);

        SmsVerificationCode entity = new SmsVerificationCode();
        entity.setOwnerId(vo.getOwnerId());
        entity.setPhoneNumber(vo.getPhoneNumber());
        entity.setBizType(vo.getBizType());
        entity.setCode(code);
        entity.setExpiresAt(expiresAt);
        entity.setCreatedIp(vo.getCreatedIp());

        verificationCodeRepository.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void verifyCode(SmsVerificationCodeVerifyVO vo) {
        SmsVerificationCode entity = verificationCodeRepository
                .findTopByPhoneNumberAndBizTypeOrderByCreatedAtDesc(vo.getPhoneNumber(), vo.getBizType())
                .orElseThrow(() -> new BusinessException(ErrorCode.SMS_CODE_INVALID, "验证码不存在或已过期"));

        // 检查过期
        if (entity.getExpiresAt().getTime() < System.currentTimeMillis()) {
            throw new BusinessException(ErrorCode.SMS_CODE_EXPIRED, "验证码已过期");
        }

        // 检查是否已验证
        if (entity.getIsVerified() == 1) {
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID, "验证码已被使用");
        }

        // 检查尝试次数
        if (entity.getAttemptCount() >= entity.getMaxAttempts()) {
            throw new BusinessException(ErrorCode.SMS_CODE_ATTEMPT_LIMIT, "验证码尝试次数超限");
        }

        // 验证码检查
        if (!entity.getCode().equals(vo.getCode())) {
            verificationCodeRepository.incrementAttemptCount(entity.getId());
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID, "验证码错误");
        }

        // 标记为已验证
        verificationCodeRepository.markAsVerified(entity.getId(), new Timestamp(System.currentTimeMillis()), vo.getVerifiedIp());
    }

    public SmsVerificationCodeVO getLatestVerificationCode(String phoneNumber, String bizType) {
        return verificationCodeRepository
                .findTopByPhoneNumberAndBizTypeOrderByCreatedAtDesc(phoneNumber, bizType)
                .map(this::toVerificationCodeVO)
                .orElse(null);
    }

    // ==================== toVO 方法 ====================

    private SmsProviderConfigVO toProviderConfigVO(SmsProviderConfig entity) {
        SmsProviderConfigVO vo = new SmsProviderConfigVO();
        vo.setId(entity.getId());
        vo.setOwnerId(entity.getOwnerId());
        vo.setProviderCode(entity.getProviderCode());
        vo.setProviderName(entity.getProviderName());
        vo.setSignName(entity.getSignName());
        vo.setRegion(entity.getRegion());
        vo.setStatus(entity.getStatus());
        vo.setIsDefault(entity.getIsDefault());
        vo.setDailyQuota(entity.getDailyQuota());
        vo.setDailySentCount(entity.getDailySentCount());
        vo.setLastResetTime(entity.getLastResetTime());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private SmsTemplateVO toTemplateVO(SmsTemplate entity) {
        SmsTemplateVO vo = new SmsTemplateVO();
        vo.setId(entity.getId());
        vo.setOwnerId(entity.getOwnerId());
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setTemplateName(entity.getTemplateName());
        vo.setContent(entity.getContent());
        vo.setProviderCode(entity.getProviderCode());
        vo.setProviderTemplateId(entity.getProviderTemplateId());
        vo.setStatus(entity.getStatus());
        vo.setTemplateType(entity.getTemplateType());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private SmsSendLogVO toSendLogVO(SmsSendLog entity) {
        SmsSendLogVO vo = new SmsSendLogVO();
        vo.setId(entity.getId());
        vo.setOwnerId(entity.getOwnerId());
        vo.setPhoneNumber(entity.getPhoneNumber());
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setProviderCode(entity.getProviderCode());
        vo.setProviderRequestId(entity.getProviderRequestId());
        vo.setContent(entity.getContent());
        vo.setStatus(entity.getStatus());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setErrorCode(entity.getErrorCode());
        vo.setSendTime(entity.getSendTime());
        vo.setDeliveredTime(entity.getDeliveredTime());
        vo.setCost(entity.getCost());
        vo.setBizId(entity.getBizId());
        vo.setBizType(entity.getBizType());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private SmsVerificationCodeVO toVerificationCodeVO(SmsVerificationCode entity) {
        SmsVerificationCodeVO vo = new SmsVerificationCodeVO();
        vo.setId(entity.getId());
        vo.setOwnerId(entity.getOwnerId());
        vo.setPhoneNumber(entity.getPhoneNumber());
        vo.setBizType(entity.getBizType());
        vo.setAttemptCount(entity.getAttemptCount());
        vo.setMaxAttempts(entity.getMaxAttempts());
        vo.setIsVerified(entity.getIsVerified());
        vo.setVerifiedTime(entity.getVerifiedTime());
        vo.setExpiresAt(entity.getExpiresAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
