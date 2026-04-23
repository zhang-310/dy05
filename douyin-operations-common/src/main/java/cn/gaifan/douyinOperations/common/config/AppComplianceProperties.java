package cn.gaifan.douyinOperations.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 合规相关配置：抖音等平台公开规则文档入口（供前端/运营跳转，不用于自动爬取）。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.compliance")
public class AppComplianceProperties {

    private Douyin douyin = new Douyin();

    @Data
    public static class Douyin {
        /**
         * 展示给运营与用户的说明（请以各平台官网最新规则为准）
         */
        private String notice = "以下为抖音及抖音电商等公开协议/规则入口，便于对照维护词库；检测规则以本系统词库与行业正则为准，请以官方最新版为准。";
        /**
         * 官方公开页面 URL 列表（可自行在 yml 中增删）
         */
        private List<String> referenceUrls = new ArrayList<>(List.of(
                "https://www.douyin.com/agreements",
                "https://school.jinritemai.com/",
                "https://fxg.jinritemai.com/"
        ));
    }
}
