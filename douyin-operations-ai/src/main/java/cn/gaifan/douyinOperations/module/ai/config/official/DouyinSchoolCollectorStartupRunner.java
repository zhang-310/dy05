package cn.gaifan.douyinOperations.module.ai.config.official;

import cn.gaifan.douyinOperations.module.ai.service.official.DouyinSchoolCollectorService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(120)
@ConditionalOnProperty(prefix = "app.douyin-school.collector", name = "import-on-startup", havingValue = "true")
public class DouyinSchoolCollectorStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DouyinSchoolCollectorStartupRunner.class);

    @Resource
    private DouyinSchoolCollectorService collectorService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("抖音学习中心官方资料启动采集开始");
            log.info("抖音学习中心官方资料启动采集完成: {}", collectorService.collect());
        } catch (Exception e) {
            log.error("抖音学习中心官方资料启动采集失败", e);
        }
    }
}
