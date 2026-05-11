package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.script.entity.UserViolationWord;
import cn.gaifan.douyinOperations.module.script.entity.ViolationWord;
import cn.gaifan.douyinOperations.module.script.repository.UserViolationWordRepository;
import cn.gaifan.douyinOperations.module.script.repository.ViolationWordRepository;
import cn.gaifan.douyinOperations.module.script.vo.*;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViolationWordServiceImplTest {

    @Mock
    private ViolationWordRepository violationWordRepository;

    @Mock
    private UserViolationWordRepository userViolationWordRepository;

    @Mock
    private Cache<String, Object> violationWordCache;

    @Mock
    private LlmClient llmClient;

    @Mock
    private AiModelRepository aiModelRepository;

    @InjectMocks
    private ViolationWordServiceImpl service;

    private ViolationWord testWord;
    private UserViolationWord testUserWord;

    @BeforeEach
    void setUp() {
        testWord = new ViolationWord();
        testWord.setId(1L);
        testWord.setWord("违规词");
        testWord.setLevel(3);
        testWord.setStatus(1);
        testWord.setScope("all");
        testWord.setDeleted(0);

        testUserWord = new UserViolationWord();
        testUserWord.setId(1L);
        testUserWord.setUserId(100L);
        testUserWord.setWord("自定义违规词");
        testUserWord.setLevel(2);
        testUserWord.setStatus(1);
        testUserWord.setDeleted(0);
    }

    @Test
    void testCheck_WithViolationWord_ShouldDetect() {
        // Arrange
        when(violationWordRepository.findByStatusAndDeletedAndScopeIn(eq(1), eq(0), anyList()))
            .thenReturn(List.of(testWord));
        when(userViolationWordRepository.findByUserIdAndStatusAndDeleted(anyLong(), eq(1), eq(0)))
            .thenReturn(List.of());

        // Act
        ViolationCheckResultVO result = service.check("这是违规词测试", "all", 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getHasViolation()).isTrue();
        assertThat(result.getViolations()).hasSize(1);
        assertThat(result.getViolations().get(0).getWord()).isEqualTo("违规词");
        assertThat(result.getViolations().get(0).getLevel()).isEqualTo(3);
    }

    @Test
    void testCheck_WithoutViolationWord_ShouldPass() {
        // Arrange
        when(violationWordRepository.findByStatusAndDeletedAndScopeIn(eq(1), eq(0), anyList()))
            .thenReturn(List.of(testWord));
        when(userViolationWordRepository.findByUserIdAndStatusAndDeleted(anyLong(), eq(1), eq(0)))
            .thenReturn(List.of());

        // Act
        ViolationCheckResultVO result = service.check("这是正常文本", "all", 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getHasViolation()).isFalse();
        assertThat(result.getViolations()).isEmpty();
    }

    @Test
    void testCheck_WithUserViolationWord_ShouldDetect() {
        // Arrange
        when(violationWordRepository.findByStatusAndDeletedAndScopeIn(eq(1), eq(0), anyList()))
            .thenReturn(List.of());
        when(userViolationWordRepository.findByUserIdAndStatusAndDeleted(eq(100L), eq(1), eq(0)))
            .thenReturn(List.of(testUserWord));

        // Act
        ViolationCheckResultVO result = service.check("这是自定义违规词测试", "all", 100L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getHasViolation()).isTrue();
        assertThat(result.getViolations()).hasSize(1);
        assertThat(result.getViolations().get(0).getWord()).isEqualTo("自定义违规词");
    }

    @Test
    void testCheck_WithNullText_ShouldReturnNoViolation() {
        // Act
        ViolationCheckResultVO result = service.check(null, "all", 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getHasViolation()).isFalse();
        assertThat(result.getViolations()).isEmpty();
    }

    @Test
    void testCheck_WithEmptyText_ShouldReturnNoViolation() {
        // Act
        ViolationCheckResultVO result = service.check("", "all", 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getHasViolation()).isFalse();
        assertThat(result.getViolations()).isEmpty();
    }

    @Test
    void testSearch_WithKeyword_ShouldReturnFiltered() {
        // Arrange
        ViolationWordSearchVO searchVO = new ViolationWordSearchVO();
        searchVO.setKeyword("违规");
        searchVO.setPage(0);
        searchVO.setRows(10);

        Page<ViolationWord> page = new PageImpl<>(List.of(testWord));
        when(violationWordRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);

        // Act
        PageResultVO<ViolationWordVO> result = service.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getWord()).isEqualTo("违规词");
    }

    @Test
    void testGetById_WhenExists_ShouldReturn() {
        // Arrange
        when(violationWordRepository.findByIdAndDeleted(1L, 0))
            .thenReturn(Optional.of(testWord));

        // Act
        ViolationWordVO result = service.getById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getWord()).isEqualTo("违规词");
    }

    @Test
    void testGetById_WhenNotExists_ShouldThrow() {
        // Arrange
        when(violationWordRepository.findByIdAndDeleted(999L, 0))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getById(999L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND);
    }

    @Test
    void testSave_Create_ShouldSuccess() {
        // Arrange
        ViolationWordSaveVO saveVO = new ViolationWordSaveVO();
        saveVO.setWord("新违规词");
        saveVO.setLevel(2);
        saveVO.setScope("all");

        ViolationWord saved = new ViolationWord();
        saved.setId(2L);
        saved.setWord("新违规词");
        when(violationWordRepository.save(any(ViolationWord.class)))
            .thenReturn(saved);

        // Act
        long id = service.save(saveVO);

        // Assert
        assertThat(id).isEqualTo(2L);
        verify(violationWordRepository).save(any(ViolationWord.class));
    }

    @Test
    void testSave_Update_ShouldSuccess() {
        // Arrange
        ViolationWordSaveVO saveVO = new ViolationWordSaveVO();
        saveVO.setId(1L);
        saveVO.setWord("更新违规词");
        saveVO.setLevel(3);

        when(violationWordRepository.findByIdAndDeleted(1L, 0))
            .thenReturn(Optional.of(testWord));
        when(violationWordRepository.save(any(ViolationWord.class)))
            .thenReturn(testWord);

        // Act
        long id = service.save(saveVO);

        // Assert
        assertThat(id).isEqualTo(1L);
        verify(violationWordRepository).save(any(ViolationWord.class));
    }

    @Test
    void testDelete_WhenExists_ShouldSuccess() {
        // Arrange
        when(violationWordRepository.findByIdAndDeleted(1L, 0))
            .thenReturn(Optional.of(testWord));

        // Act
        service.delete(1L);

        // Assert
        verify(violationWordRepository).save(argThat(word -> word.getDeleted() == 1));
    }

    @Test
    void testDelete_WhenNotExists_ShouldThrow() {
        // Arrange
        when(violationWordRepository.findByIdAndDeleted(999L, 0))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.delete(999L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND);
    }

    @Test
    void testListActive_ShouldReturnActiveWords() {
        // Arrange
        when(violationWordRepository.findByStatusAndDeleted(1, 0))
            .thenReturn(List.of(testWord));

        // Act
        List<ViolationWordVO> results = service.listActive();

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getWord()).isEqualTo("违规词");
    }
}
