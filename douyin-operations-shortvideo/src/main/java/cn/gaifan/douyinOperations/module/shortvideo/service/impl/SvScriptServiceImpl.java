package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvScript;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvScriptRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoAiService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AiScriptGenerateVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.ViralCollectVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptVO;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SvScriptServiceImpl implements SvScriptService {

    @Resource
    private SvScriptRepository scriptRepository;
    @Resource
    private ShortVideoAiService aiService;
    @Resource
    private ViralVideoService viralVideoService;

    @Override
    public PageResultVO<SvScriptVO> search(SvScriptSearchVO vo, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        vo.validateParams();
        Specification<SvScript> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("ownerId"), ownerId));
            preds.add(cb.equal(root.get("deleted"), 0));
            if (StringUtils.hasText(vo.getScriptType())) preds.add(cb.equal(root.get("scriptType"), vo.getScriptType()));
            if (StringUtils.hasText(vo.getStyle())) preds.add(cb.equal(root.get("style"), vo.getStyle()));
            if (StringUtils.hasText(vo.getTitle())) preds.add(cb.like(root.get("title"), "%" + vo.getTitle().trim() + "%"));
            return cb.and(preds.toArray(new Predicate[0]));
        };
        String sortName = StringUtils.hasText(vo.getSortName()) ? vo.getSortName() : "createTime";
        Sort sort = "asc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.by(sortName).ascending() : Sort.by(sortName).descending();
        vo.validateParams();
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(), sort);
        Page<SvScript> page = scriptRepository.findAll(spec, pageable);
        List<SvScriptVO> list = page.getContent().stream().map(this::toVO).toList();
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public SvScriptVO get(Long id, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvScript e = scriptRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "脚本不存在"));
        if (!e.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问");
        if (e.getDeleted() != 0) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "脚本不存在");
        return toVO(e);
    }

    @Override
    public Long save(SvScriptSaveVO vo, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvScript e;
        if (vo.getId() != null && vo.getId() > 0) {
            e = scriptRepository.findById(vo.getId()).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "脚本不存在"));
            if (!e.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限修改");
        } else {
            e = new SvScript();
            e.setOwnerId(ownerId);
        }
        e.setTitle(vo.getTitle());
        e.setContent(vo.getContent());
        e.setScriptType(vo.getScriptType());
        e.setGenerationType(vo.getGenerationType());
        e.setReferenceViralId(vo.getReferenceViralId());
        e.setTheme(vo.getTheme());
        e.setStyle(vo.getStyle());
        e.setDuration(vo.getDuration());
        e.setWordCount(vo.getWordCount());
        e.setTags(vo.getTags());
        e.setAiPrompt(vo.getAiPrompt());
        e.setAiModel(vo.getAiModel());
        e = scriptRepository.save(e);
        return e.getId();
    }

    @Override
    public void delete(Long id, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvScript e = scriptRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "脚本不存在"));
        if (!e.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除");
        e.setDeleted(1);
        scriptRepository.save(e);
    }

    @Override
    public String generate(String type, String theme, Long viralVideoId, String productInfo, String style, Integer duration, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        AiScriptGenerateVO vo = new AiScriptGenerateVO();
        vo.setCopyText(theme != null ? theme : (productInfo != null ? productInfo : "短视频脚本"));
        vo.setViralId(viralVideoId);
        vo.setPersonaId(null);
        return aiService.generateScript(vo, ownerId);
    }

    @Override
    public String analyzeViral(String viralVideoUrl, String extractLevel, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (!StringUtils.hasText(viralVideoUrl)) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "爆款视频 URL 不能为空");
        ViralCollectVO collectVO = new ViralCollectVO();
        collectVO.setVideoUrl(viralVideoUrl);
        collectVO.setDouyinVideoId("url_" + System.currentTimeMillis());
        collectVO.setTitle("爆款分析");
        Long viralId = viralVideoService.collectViralVideo(collectVO, ownerId);
        viralVideoService.triggerAnalysis(viralId, ownerId);
        SvViralVideo viral = viralVideoService.getViralVideo(viralId, ownerId);
        return viral.getAnalysisResult() != null ? viral.getAnalysisResult() : "分析已触发，请稍后查看";
    }

    private SvScriptVO toVO(SvScript e) {
        SvScriptVO vo = new SvScriptVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setTitle(e.getTitle());
        vo.setContent(e.getContent());
        vo.setScriptType(e.getScriptType());
        vo.setGenerationType(e.getGenerationType());
        vo.setReferenceViralId(e.getReferenceViralId());
        vo.setTheme(e.getTheme());
        vo.setStyle(e.getStyle());
        vo.setDuration(e.getDuration());
        vo.setWordCount(e.getWordCount());
        vo.setTags(e.getTags());
        vo.setAiPrompt(e.getAiPrompt());
        vo.setAiModel(e.getAiModel());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
