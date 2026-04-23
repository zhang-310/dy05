package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;
import java.util.Map;

public interface MultiPlatformContentService {
    Map<String, String> generateMultiPlatformVersions(Long userId, String originalScript, List<String> platforms);
}
