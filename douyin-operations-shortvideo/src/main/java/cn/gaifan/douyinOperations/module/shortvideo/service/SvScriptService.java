package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptVO;

import java.util.List;

/**
 * 脚本 Service
 */
public interface SvScriptService {

    PageResultVO<SvScriptVO> search(SvScriptSearchVO vo, Long ownerId);

    SvScriptVO get(Long id, Long ownerId);

    Long save(SvScriptSaveVO vo, Long ownerId);

    void delete(Long id, Long ownerId);

    /** AI 生成脚本，返回脚本内容 */
    String generate(String type, String theme, Long viralVideoId, String productInfo, String style, Integer duration, Long ownerId);

    /** 分析爆款脚本，返回分析结果 */
    String analyzeViral(String viralVideoUrl, String extractLevel, Long ownerId);
}
