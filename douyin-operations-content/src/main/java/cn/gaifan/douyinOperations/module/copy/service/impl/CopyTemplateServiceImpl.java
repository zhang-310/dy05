package cn.gaifan.douyinOperations.module.copy.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.copy.entity.CopyTemplate;
import cn.gaifan.douyinOperations.module.copy.repository.CopyTemplateRepository;
import cn.gaifan.douyinOperations.module.copy.service.CopyTemplateService;
import cn.gaifan.douyinOperations.module.copy.vo.*;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CopyTemplateServiceImpl implements CopyTemplateService {

    /** 模板变量格式：{变量名}，变量名须为字母/下划线开头，后接字母数字下划线 */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^}]+)}");
    private static final Pattern VARIABLE_NAME_OK = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private static final Set<String> SORTABLE_FIELDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("id", "userId", "status", "createTime", "updateTime")));

    @Resource
    private CopyTemplateRepository copyTemplateRepository;

    @Resource
    private Cache<Long, Object> copyTemplateCache;

    @Override
    public PageResultVO<CopyTemplateVO> search(CopyTemplateSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE_FIELDS.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<CopyTemplate> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            // P1-2: 数据隔离 - 强制过滤 userId（不可绕过）
            if (vo.getUserId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "userId 不能为空");
            }
            predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
            if (vo.getCategory() != null && !vo.getCategory().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("category"), vo.getCategory().trim()));
            }
            if (vo.getStatus() != null) predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            if (vo.getKeyword() != null && !vo.getKeyword().trim().isEmpty()) {
                String kw = "%" + vo.getKeyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("templateName"), kw),
                        cb.like(root.get("templateContent"), kw)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CopyTemplate> page = copyTemplateRepository.findAll(spec, pageable);
        List<CopyTemplateVO> list = page.getContent().stream().map(this::toVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    private void validateTemplateVariables(String templateContent) {
        Matcher m = VARIABLE_PATTERN.matcher(templateContent);
        while (m.find()) {
            String varName = m.group(1).trim();
            if (!VARIABLE_NAME_OK.matcher(varName).matches()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                        "模板变量格式非法，须为 {变量名}，变量名仅允许字母、数字、下划线且以字母或下划线开头，非法示例: {" + varName + "}");
            }
        }
    }

    @Override
    public CopyTemplateVO getById(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "模板 ID 无效");

        // P0-3: 先查缓存
        CopyTemplateVO cached = (CopyTemplateVO) copyTemplateCache.getIfPresent(id);
        if (cached != null) return cached;

        // 缓存未命中，查数据库
        CopyTemplate entity = copyTemplateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        CopyTemplateVO vo = toVO(entity);

        // 写入缓存
        copyTemplateCache.put(id, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(CopyTemplateSaveVO vo) {
        if (vo.getTemplateContent() != null && !vo.getTemplateContent().isEmpty()) {
            validateTemplateVariables(vo.getTemplateContent());
        }
        CopyTemplate entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = copyTemplateRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
            // P0-3: 更新时失效缓存
            copyTemplateCache.invalidate(vo.getId());
        } else {
            entity = new CopyTemplate();
            entity.setUserId(vo.getUserId());
        }
        // P0-1: XSS 防护 - HTML 转义用户输入
        entity.setTemplateName(vo.getTemplateName() != null ? StringEscapeUtils.escapeHtml4(vo.getTemplateName()) : null);
        entity.setTemplateContent(vo.getTemplateContent() != null ? StringEscapeUtils.escapeHtml4(vo.getTemplateContent()) : null);
        if (vo.getCategory() != null) entity.setCategory(StringEscapeUtils.escapeHtml4(vo.getCategory()));
        if (vo.getDescription() != null) entity.setDescription(StringEscapeUtils.escapeHtml4(vo.getDescription()));
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        entity = copyTemplateRepository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "模板 ID 无效");
        CopyTemplate entity = copyTemplateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        // P1-4: IDOR 防护 - 校验所有权
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除他人的模板");
        }
        entity.setDeleted(1);
        copyTemplateRepository.save(entity);
        // P0-3: 删除时失效缓存
        copyTemplateCache.invalidate(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status, Long userId) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "模板 ID 无效");
        CopyTemplate entity = copyTemplateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        // P1-4: IDOR 防护 - 校验所有权
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改他人的模板");
        }
        copyTemplateRepository.updateStatus(id, status);
        // P0-3: 更新状态时失效缓存
        copyTemplateCache.invalidate(id);
    }

    private CopyTemplateVO toVO(CopyTemplate e) {
        CopyTemplateVO vo = new CopyTemplateVO();
        vo.setId(e.getId());
        vo.setUserId(e.getUserId());
        vo.setTemplateName(e.getTemplateName());
        vo.setTemplateContent(e.getTemplateContent());
        vo.setCategory(e.getCategory());
        vo.setDescription(e.getDescription());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
