package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.AiModelService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.vo.AiModelAdminVO;
import cn.gaifan.douyinOperations.module.ai.vo.AiModelSaveVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiModelServiceImpl implements AiModelService {

    @Resource
    private AiModelRepository aiModelRepository;

    @Resource
    private OpenAiCompatibleLlmClient openAiCompatibleLlmClient;

    @Resource
    private LlmClient llmClient;

    @Override
    public List<AiModel> listAll() {
        return aiModelRepository.findAll().stream()
                .filter(m -> m.getDeleted() == 0)
                .toList();
    }

    @Override
    public List<AiModelAdminVO> listAllForAdmin() {
        return aiModelRepository.findAll().stream()
                .filter(m -> m.getDeleted() == 0)
                .map(this::toAdminVo)
                .collect(Collectors.toList());
    }

    private AiModelAdminVO toAdminVo(AiModel m) {
        AiModelAdminVO vo = new AiModelAdminVO();
        vo.setId(m.getId());
        vo.setModelName(m.getModelName());
        vo.setModelProvider(m.getModelProvider());
        vo.setModelVersion(m.getModelVersion());
        vo.setApiBaseUrl(m.getApiBaseUrl());
        vo.setApiKeyMasked(maskApiKey(m.getApiKey()));
        vo.setMaxTokens(m.getMaxTokens());
        vo.setTemperature(m.getTemperature());
        vo.setStatus(m.getStatus());
        vo.setIsDefault(m.getIsDefault());
        vo.setCostPer1kTokens(m.getCostPer1kTokens());
        vo.setQuotaLimit(m.getQuotaLimit());
        vo.setQuotaUsed(m.getQuotaUsed());
        vo.setCreateTime(m.getCreateTime());
        vo.setUpdateTime(m.getUpdateTime());
        vo.setResolvedBaseUrl(openAiCompatibleLlmClient.resolveDisplayBaseUrl(m));
        return vo;
    }

    private static String maskApiKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        if (key.length() <= 4) {
            return "****";
        }
        return "****" + key.substring(key.length() - 4);
    }

    @Override
    @Transactional
    public void save(AiModelSaveVO vo) {
        boolean wasDefault = false;
        AiModel model;
        if (vo.getId() != null) {
            model = aiModelRepository.findById(vo.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模型不存在"));
            wasDefault = model.getIsDefault() != null && model.getIsDefault() == 1;
        } else {
            model = new AiModel();
        }

        model.setModelName(vo.getModelName());
        model.setModelProvider(vo.getProvider());
        model.setModelVersion(vo.getEndpoint() != null ? vo.getEndpoint() : "");
        if (vo.getId() == null) {
            model.setApiKey(vo.getApiKey() != null && !vo.getApiKey().isBlank() ? vo.getApiKey() : null);
        } else if (vo.getApiKey() != null && !vo.getApiKey().isBlank()) {
            model.setApiKey(vo.getApiKey());
        }
        if (vo.getApiBaseUrl() != null) {
            String u = vo.getApiBaseUrl().trim();
            model.setApiBaseUrl(u.isEmpty() ? null : u);
        } else if (vo.getId() == null) {
            model.setApiBaseUrl(null);
        }
        model.setMaxTokens(vo.getMaxTokens());
        model.setTemperature(vo.getTemperature());
        model.setStatus(vo.getStatus() != null ? vo.getStatus() : 1);

        aiModelRepository.save(model);

        if (vo.getIsDefault() != null && vo.getIsDefault() == 1) {
            setDefault(model.getId());
        } else if (vo.getIsDefault() != null && vo.getIsDefault() == 0 && wasDefault) {
            model.setIsDefault(0);
            aiModelRepository.save(model);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AiModel model = aiModelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模型不存在"));
        model.setDeleted(1);
        aiModelRepository.save(model);
    }

    @Override
    @Transactional
    public void setDefault(Long id) {
        AiModel model = aiModelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模型不存在"));

        List<AiModel> allModels = aiModelRepository.findAll();
        for (AiModel m : allModels) {
            if (m.getIsDefault() != null && m.getIsDefault() == 1) {
                m.setIsDefault(0);
                aiModelRepository.save(m);
            }
        }

        model.setIsDefault(1);
        aiModelRepository.save(model);
    }

    @Override
    public Map<String, Object> testConnection(Long id) {
        AiModel model = aiModelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模型不存在"));
        if (model.getDeleted() != null && model.getDeleted() != 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模型不存在");
        }
        LlmClient.LlmResponse r = llmClient.chat(model, "Reply with exactly: OK", "ping");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", r.success());
        out.put("errorMsg", r.errorMsg());
        out.put("tokensUsed", r.tokensUsed());
        return out;
    }
}
