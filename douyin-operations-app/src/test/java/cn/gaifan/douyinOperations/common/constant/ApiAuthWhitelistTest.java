package cn.gaifan.douyinOperations.common.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiAuthWhitelistTest {

    @Test
    void containsMatchesExactPaths() {
        assertThat(ApiAuthWhitelist.contains("/api/v1/auth/login")).isTrue();
        assertThat(ApiAuthWhitelist.contains("/api/v1/auth/captcha")).isTrue();
        assertThat(ApiAuthWhitelist.contains("/api/v1/product/list")).isFalse();
    }

    @Test
    void pathsArrayCoversFilterWhitelist() {
        assertThat(ApiAuthWhitelist.PATHS).contains(
                "/api/v1/auth/login",
                "/api/v1/messaging/webhook/wecom"
        );
    }
}
