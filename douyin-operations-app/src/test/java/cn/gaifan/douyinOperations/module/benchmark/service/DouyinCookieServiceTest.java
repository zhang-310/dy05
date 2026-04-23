package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.entity.DouyinCookie;
import cn.gaifan.douyinOperations.module.benchmark.repository.DouyinCookieRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.impl.DouyinCookieServiceImpl;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieSaveVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieVO;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DouyinCookieService 单元测试
 */
@DisplayName("DouyinCookieService 单元测试")
class DouyinCookieServiceTest {

    @Mock
    private DouyinCookieRepository repository;

    @InjectMocks
    private DouyinCookieServiceImpl service;

    private static final Long TEST_USER_ID = 1L;
    private DouyinCookie testCookie;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testCookie = new DouyinCookie();
        testCookie.setId(1L);
        testCookie.setOwnerId(TEST_USER_ID);
        testCookie.setCookieName("测试Cookie");
        // 存储加密后的Cookie值（使用真实的AES加密）
        testCookie.setCookieValue("B7Tu6FAdI9CmSu/STieeLUMlVzJV1fAJoT4wNigZZPo=");
        testCookie.setIsValid(true);
        testCookie.setUsageCount(0);
        testCookie.setDeleted(0);
    }

    @Test
    @DisplayName("保存 Cookie - 新增 - 应成功保存")
    void save_newCookie_shouldSaveSuccessfully() {
        DouyinCookieSaveVO saveVO = new DouyinCookieSaveVO();
        saveVO.setCookieName("新Cookie");
        saveVO.setCookieValue("sessionid=new123");

        when(repository.save(any(DouyinCookie.class))).thenReturn(testCookie);

        service.save(saveVO, TEST_USER_ID);

        verify(repository, times(1)).save(any(DouyinCookie.class));
    }

    @Test
    @DisplayName("保存 Cookie - 更新 - 应成功更新")
    void save_existingCookie_shouldUpdateSuccessfully() {
        DouyinCookieSaveVO saveVO = new DouyinCookieSaveVO();
        saveVO.setId(1L);
        saveVO.setCookieName("更新Cookie");
        saveVO.setCookieValue("sessionid=updated123");

        when(repository.findById(1L)).thenReturn(Optional.of(testCookie));
        when(repository.save(any(DouyinCookie.class))).thenReturn(testCookie);

        service.save(saveVO, TEST_USER_ID);

        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(any(DouyinCookie.class));
        assertThat(testCookie.getCookieName()).isEqualTo("更新Cookie");
    }

    @Test
    @DisplayName("保存 Cookie - ID 不存在 - 应抛出异常")
    void save_nonExistentId_shouldThrowException() {
        DouyinCookieSaveVO saveVO = new DouyinCookieSaveVO();
        saveVO.setId(999L);
        saveVO.setCookieName("不存在");

        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(saveVO, TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cookie不存在");
    }

    @Test
    @DisplayName("查询列表 - 应返回分页结果")
    void search_shouldReturnPagedResult() {
        DouyinCookieSearchVO searchVO = new DouyinCookieSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        Page<DouyinCookie> page = new PageImpl<>(List.of(testCookie), PageRequest.of(0, 10), 1);
        when(repository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        PageResultVO<DouyinCookieVO> result = service.search(searchVO, TEST_USER_ID);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getCookieName()).isEqualTo("测试Cookie");
    }

    @Test
    @DisplayName("按 ID 查询 - 应返回 Cookie")
    void getById_shouldReturnCookie() {
        when(repository.findById(1L)).thenReturn(Optional.of(testCookie));

        DouyinCookieVO result = service.getById(1L, TEST_USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getCookieName()).isEqualTo("测试Cookie");
    }

    @Test
    @DisplayName("按 ID 查询 - 不存在 - 应抛出异常")
    void getById_nonExistent_shouldThrowException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L, TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cookie不存在");
    }

    @Test
    @DisplayName("删除 Cookie - 应逻辑删除")
    void delete_shouldLogicallyDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(testCookie));

        service.delete(1L, TEST_USER_ID);

        verify(repository, times(1)).save(testCookie);
        assertThat(testCookie.getDeleted()).isEqualTo(1);
    }

    @Test
    @DisplayName("验证 Cookie - 有效 - 应返回 true")
    void validate_validCookie_shouldReturnTrue() {
        when(repository.findById(1L)).thenReturn(Optional.of(testCookie));

        Boolean result = service.validate(1L, TEST_USER_ID);

        assertThat(result).isTrue();
        verify(repository, times(1)).save(testCookie);
    }

    @Test
    @DisplayName("获取可用 Cookie - 应返回有效的 Cookie")
    void getAvailableCookie_shouldReturnValidCookie() {
        when(repository.findByOwnerIdAndPlatformAndIsValid(TEST_USER_ID, "douyin", true)).thenReturn(List.of(testCookie));
        when(repository.save(any(DouyinCookie.class))).thenReturn(testCookie);

        String result = service.getAvailableCookie(TEST_USER_ID, "douyin");

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("sessionid=test123"); // 解密后的明文
        verify(repository, times(1)).save(any(DouyinCookie.class)); // 验证更新了使用统计
    }

    @Test
    @DisplayName("获取可用 Cookie - 无可用 - 应返回 null")
    void getAvailableCookie_noneAvailable_shouldReturnNull() {
        when(repository.findByOwnerIdAndPlatformAndIsValid(TEST_USER_ID, "douyin", true)).thenReturn(List.of());

        String result = service.getAvailableCookie(TEST_USER_ID, "douyin");

        assertThat(result).isNull();
    }
}
