package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAccount;
import cn.gaifan.douyinOperations.module.benchmark.entity.DouyinCookie;
import cn.gaifan.douyinOperations.module.benchmark.metrics.BenchmarkMetrics;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkAccountRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.impl.BenchmarkAccountServiceImpl;
import cn.gaifan.douyinOperations.module.benchmark.vo.*;
import cn.gaifan.douyinOperations.module.shortvideo.service.impl.AccountVideoScraper;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BenchmarkAccountService 单元测试
 */
@DisplayName("BenchmarkAccountService 单元测试")
class BenchmarkAccountServiceTest {

    @Mock
    private BenchmarkAccountRepository repository;

    @Mock
    private DouyinCookieService cookieService;

    @Mock
    private AccountVideoScraper accountVideoScraper;

    @Mock
    private BenchmarkMetrics metrics;

    @InjectMocks
    private BenchmarkAccountServiceImpl service;

    private static final Long TEST_USER_ID = 1L;
    private BenchmarkAccount testAccount;
    private DouyinCookie testCookie;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testAccount = new BenchmarkAccount();
        testAccount.setId(1L);
        testAccount.setOwnerId(TEST_USER_ID);
        testAccount.setAccountName("测试账号");
        testAccount.setAccountUrl("https://www.douyin.com/user/MS4wLjABAAAAtest");
        testAccount.setSecUid("MS4wLjABAAAAtest");
        testAccount.setFanCount(100000L);
        testAccount.setVideoCount(500);
        testAccount.setDeleted(0);
        testAccount.setCreateTime(LocalDateTime.now());
        testAccount.setUpdateTime(LocalDateTime.now());

        testCookie = new DouyinCookie();
        testCookie.setId(1L);
        testCookie.setCookieValue("sessionid=test123");

        // Mock AccountVideoScraper 为可用状态
        when(accountVideoScraper.isAvailable()).thenReturn(true);
    }

    @Test
    @DisplayName("保存账号 - 新增 - 应成功保存")
    void save_newAccount_shouldSaveSuccessfully() {
        BenchmarkAccountSaveVO saveVO = new BenchmarkAccountSaveVO();
        saveVO.setAccountName("新账号");
        saveVO.setAccountUrl("https://www.douyin.com/user/MS4wLjABAAAAnew");
        saveVO.setSecUid("MS4wLjABAAAAnew");

        when(repository.save(any(BenchmarkAccount.class))).thenReturn(testAccount);

        service.save(saveVO, TEST_USER_ID);

        verify(repository, times(1)).save(any(BenchmarkAccount.class));
    }

    @Test
    @DisplayName("保存账号 - 更新 - 应成功更新")
    void save_existingAccount_shouldUpdateSuccessfully() {
        BenchmarkAccountSaveVO saveVO = new BenchmarkAccountSaveVO();
        saveVO.setId(1L);
        saveVO.setAccountName("更新账号");

        when(repository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(repository.save(any(BenchmarkAccount.class))).thenReturn(testAccount);

        service.save(saveVO, TEST_USER_ID);

        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(any(BenchmarkAccount.class));
        assertThat(testAccount.getAccountName()).isEqualTo("更新账号");
    }

    @Test
    @DisplayName("查询列表 - 应返回分页结果")
    void search_shouldReturnPagedResult() {
        BenchmarkAccountSearchVO searchVO = new BenchmarkAccountSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        Page<BenchmarkAccount> page = new PageImpl<>(List.of(testAccount), PageRequest.of(0, 10), 1);
        when(repository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        PageResultVO<BenchmarkAccountVO> result = service.search(searchVO, TEST_USER_ID);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getAccountName()).isEqualTo("测试账号");
    }

    @Test
    @DisplayName("按 ID 查询 - 应返回账号")
    void getById_shouldReturnAccount() {
        when(repository.findById(1L)).thenReturn(Optional.of(testAccount));

        BenchmarkAccountVO result = service.getById(1L, TEST_USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getAccountName()).isEqualTo("测试账号");
    }

    @Test
    @DisplayName("删除账号 - 应逻辑删除")
    void delete_shouldLogicallyDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(testAccount));

        service.delete(1L, TEST_USER_ID);

        verify(repository, times(1)).save(testAccount);
        assertThat(testAccount.getDeleted()).isEqualTo(1);
    }

    @Test
    @DisplayName("按关键词搜索 - 应调用 Playwright 搜索")
    void searchByKeyword_shouldCallPlaywright() {
        SearchAccountByKeywordVO searchVO = new SearchAccountByKeywordVO();
        searchVO.setKeyword("护肤");
        searchVO.setMinFanCount(50000L);
        searchVO.setMaxResults(10);

        when(cookieService.getAvailableCookie(TEST_USER_ID, "douyin")).thenReturn("sessionid=test123");

        // Mock Playwright 搜索结果
        when(accountVideoScraper.searchDouyinAccount(anyString())).thenReturn("https://www.douyin.com/user/MS4wLjABAAAAsearch");

        AccountVideoScraper.ScrapeResult scrapeResult = new AccountVideoScraper.ScrapeResult();
        scrapeResult.setAccountName("搜索到的账号");
        scrapeResult.setSecUid("MS4wLjABAAAAsearch");
        when(accountVideoScraper.scrapeAccountVideos(anyString())).thenReturn(scrapeResult);

        when(repository.findBySecUidAndOwnerId(anyString(), eq(TEST_USER_ID))).thenReturn(null);
        when(repository.save(any(BenchmarkAccount.class))).thenReturn(testAccount);

        List<BenchmarkAccountVO> result = service.searchByKeyword(searchVO, TEST_USER_ID);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("按关键词搜索 - 无可用 Cookie - 应抛出异常")
    void searchByKeyword_noCookie_shouldThrowException() {
        SearchAccountByKeywordVO searchVO = new SearchAccountByKeywordVO();
        searchVO.setKeyword("护肤");

        when(cookieService.getAvailableCookie(TEST_USER_ID, "douyin")).thenReturn(null);

        assertThatThrownBy(() -> service.searchByKeyword(searchVO, TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("没有可用的Cookie");
    }

    @Test
    @DisplayName("按 URL 分析 - 应调用 Playwright 分析")
    void analyzeByUrl_shouldCallPlaywright() {
        AnalyzeAccountByUrlVO analyzeVO = new AnalyzeAccountByUrlVO();
        analyzeVO.setAccountUrl("https://www.douyin.com/user/MS4wLjABAAAAurl");

        when(cookieService.getAvailableCookie(TEST_USER_ID, "douyin")).thenReturn("sessionid=test123");

        // Mock extractSecUid
        when(accountVideoScraper.extractSecUid(anyString())).thenReturn("MS4wLjABAAAAurl");

        // Mock Playwright 抓取结果
        AccountVideoScraper.ScrapeResult scrapeResult = new AccountVideoScraper.ScrapeResult();
        scrapeResult.setAccountName("URL账号");
        scrapeResult.setSecUid("MS4wLjABAAAAurl");
        when(accountVideoScraper.scrapeAccountVideos(anyString())).thenReturn(scrapeResult);

        when(repository.findBySecUidAndOwnerId(anyString(), eq(TEST_USER_ID))).thenReturn(null);
        when(repository.save(any(BenchmarkAccount.class))).thenReturn(testAccount);

        BenchmarkAccountVO result = service.analyzeByUrl(analyzeVO, TEST_USER_ID);

        assertThat(result).isNotNull();
    }
}
