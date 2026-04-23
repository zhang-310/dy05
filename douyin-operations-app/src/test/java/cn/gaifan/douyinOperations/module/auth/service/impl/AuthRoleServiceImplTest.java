package cn.gaifan.douyinOperations.module.auth.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.auth.entity.AuthRole;
import cn.gaifan.douyinOperations.module.auth.repository.AuthRoleRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthRoleResourceRepository;
import cn.gaifan.douyinOperations.module.auth.vo.AuthRoleSearchVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthRoleVO;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthRoleServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthRoleServiceImpl 单元测试")
class AuthRoleServiceImplTest {

    @Mock
    private AuthRoleRepository authRoleRepository;
    @Mock
    private AuthRoleResourceRepository authRoleResourceRepository;

    @InjectMocks
    private AuthRoleServiceImpl authRoleService;

    private AuthRole sampleRole;
    private static final Long TEST_ID = 1L;

    @BeforeEach
    void setUp() {
        sampleRole = new AuthRole();
        sampleRole.setId(TEST_ID);
        sampleRole.setRoleCode("admin");
        sampleRole.setRoleName("管理员");
        sampleRole.setDeleted(0);
    }

    @Nested
    @DisplayName("search")
    class SearchTests {

        @Test
        @DisplayName("search_shouldReturnPage")
        void search_shouldReturnPage() {
            AuthRoleSearchVO vo = new AuthRoleSearchVO();
            vo.setPage(0);
            vo.setRows(10);

            when(authRoleRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleRole), org.springframework.data.domain.PageRequest.of(0, 10), 1));

            PageResultVO<AuthRoleVO> result = authRoleService.search(vo);

            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getRoleCode()).isEqualTo("admin");
        }
    }

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("getById_existing_shouldReturn")
        void getById_existing_shouldReturn() {
            when(authRoleRepository.findById(TEST_ID)).thenReturn(Optional.of(sampleRole));

            AuthRoleVO result = authRoleService.getById(TEST_ID);

            assertThat(result).isNotNull();
            assertThat(result.getRoleCode()).isEqualTo("admin");
        }

        @Test
        @DisplayName("getById_null_shouldReturnNull")
        void getById_null_shouldReturnNull() {
            assertThat(authRoleService.getById(null)).isNull();
        }

        @Test
        @DisplayName("getById_notFound_shouldReturnNull")
        void getById_notFound_shouldReturnNull() {
            when(authRoleRepository.findById(999L)).thenReturn(Optional.empty());
            assertThat(authRoleService.getById(999L)).isNull();
        }
    }

    @Nested
    @DisplayName("listAll")
    class ListAllTests {

        @Test
        @DisplayName("listAll_shouldReturnList")
        void listAll_shouldReturnList() {
            when(authRoleRepository.findByDeletedOrderBySortOrderAsc(0)).thenReturn(List.of(sampleRole));

            List<AuthRoleVO> result = authRoleService.listAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoleCode()).isEqualTo("admin");
        }
    }
}
