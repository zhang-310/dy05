package cn.gaifan.douyinOperations.module.common.test;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

/**
 * 测试基类
 * 提供通用的测试初始化和工具方法
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@ActiveProfiles("test")
public class BaseServiceTest {

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 断言 REST 结果成功
     */
    protected void assertRestResultSuccess(RESTResult<?> result) {
        assert result != null : "结果不能为空";
        assert result.getStatus() == 200 : "状态码应该是 200";
        assert result.getData() != null : "数据不能为空";
    }

    /**
     * 断言 REST 结果失败
     */
    protected void assertRestResultFailure(RESTResult<?> result, int expectedErrorCode) {
        assert result != null : "结果不能为空";
        assert result.getStatus() != 200 : "应该是失败状态";
    }
}
