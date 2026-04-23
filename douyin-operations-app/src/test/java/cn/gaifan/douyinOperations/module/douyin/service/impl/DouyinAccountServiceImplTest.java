package cn.gaifan.douyinOperations.module.douyin.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinVideoRepository;
import cn.gaifan.douyinOperations.module.douyin.vo.DouyinAccountSearchVO;
import cn.gaifan.douyinOperations.module.douyin.vo.DouyinAccountVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DouyinAccountServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DouyinAccountServiceImpl 单元测试")
class DouyinAccountServiceImplTest {

    @Mock
    private DouyinAccountRepository douyinAccountRepository;
    @Mock
    private DouyinVideoRepository douyinVideoRepository;

    @InjectMocks
    private DouyinAccountServiceImpl douyinAccountService;

    private DouyinAccount sampleAccount;
    private static final Long TEST_ID = 1L;

    @BeforeEach
    void setUp() {
        sampleAccount = new DouyinAccount();
        sampleAccount.setId(TEST_ID);
        sampleAccount.setUserId(1L);
        sampleAccount.setAccountId("dy123");
        sampleAccount.setAccountName("测试账号");
        sampleAccount.setDeleted(0);
    }

    @Nested
    @DisplayName("search")
    class SearchTests {

        @Test
        @DisplayName("search_shouldReturnPage")
        void search_shouldReturnPage() {
            DouyinAccountSearchVO vo = new DouyinAccountSearchVO();
            vo.setPage(0);
            vo.setRows(10);

            when(douyinAccountRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleAccount), org.springframework.data.domain.PageRequest.of(0, 10), 1));

            PageResultVO<DouyinAccountVO> result = douyinAccountService.search(vo);

            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getAccountName()).isEqualTo("测试账号");
        }
    }

    @Nested
    @DisplayName("getAccount")
    class GetAccountTests {

        @Test
        @DisplayName("getAccount_valid_shouldReturn")
        void getAccount_valid_shouldReturn() {
            when(douyinAccountRepository.findByIdAndDeleted(TEST_ID, 0)).thenReturn(Optional.of(sampleAccount));

            DouyinAccountVO result = douyinAccountService.getAccount(TEST_ID);

            assertThat(result).isNotNull();
            assertThat(result.getAccountName()).isEqualTo("测试账号");
        }

        @Test
        @DisplayName("getAccount_null_shouldThrow")
        void getAccount_null_shouldThrow() {
            assertThrows(BusinessException.class, () -> douyinAccountService.getAccount(null));
        }

        @Test
        @DisplayName("getAccount_notFound_shouldThrow")
        void getAccount_notFound_shouldThrow() {
            when(douyinAccountRepository.findByIdAndDeleted(999L, 0)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class, () -> douyinAccountService.getAccount(999L));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.VALIDATION_FAIL);
        }
    }
}
