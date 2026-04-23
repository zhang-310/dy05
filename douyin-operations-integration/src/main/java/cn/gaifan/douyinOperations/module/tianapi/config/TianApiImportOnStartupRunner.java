package cn.gaifan.douyinOperations.module.tianapi.config;

import cn.gaifan.douyinOperations.module.tianapi.service.TianApiMaterialImportService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 启动时执行一次 TianAPI 素材入库（用于验证/演示，设 tianapi.import-on-startup=true）
 */
@Component
@Order(100)
@ConditionalOnProperty(prefix = "tianapi", name = "import-on-startup", havingValue = "true")
public class TianApiImportOnStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TianApiImportOnStartupRunner.class);

    @Resource
    private TianApiMaterialImportService materialImportService;
    @Resource
    private TianApiProperties tianApiProperties;

    @Override
    public void run(ApplicationArguments args) {
        Integer apiCap = tianApiProperties.getMaterialImportApiCallsPerCategory();
        if (apiCap != null && apiCap >= 3000) {
            log.info("TianAPI 启动入库已跳过：material-import-api-calls-per-category={}（全量模式请仅用定时任务触发，避免启动阻塞）", apiCap);
            return;
        }
        try {
            log.info("TianAPI 启动入库开始...");
            Map<String, Object> result = materialImportService.runImport();
            int imported = (Integer) result.getOrDefault("totalImported", 0);
            boolean demoUsed = Boolean.TRUE.equals(result.get("demoFallbackUsed"));
            log.info("TianAPI 启动入库完成: imported={}, demoFallbackUsed={}", imported, demoUsed);
        } catch (Exception e) {
            log.error("TianAPI 启动入库失败", e);
        }
    }
}
