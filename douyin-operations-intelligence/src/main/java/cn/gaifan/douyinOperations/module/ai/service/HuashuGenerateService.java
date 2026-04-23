package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.vo.HuashuGenerateRequestVO;
import cn.gaifan.douyinOperations.module.ai.vo.HuashuGenerateResponseVO;

/**
 * 基于话术知识库（huashu）生成高光话术。
 * 支持：1）可选导入本地参考素材到 huashu；2）RAG 检索；3）LLM 生成。
 */
public interface HuashuGenerateService {

    /**
     * 从 huashu 知识库生成高光话术
     *
     * @param vo     请求参数：hostName（主播名）、sourcePath（可选，导入目录）、query（可选，检索关键词）
     * @param userId 当前用户 ID
     * @return 生成结果
     */
    HuashuGenerateResponseVO generate(HuashuGenerateRequestVO vo, Long userId);
}
