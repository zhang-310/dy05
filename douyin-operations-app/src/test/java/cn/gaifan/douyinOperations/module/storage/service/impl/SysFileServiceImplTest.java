package cn.gaifan.douyinOperations.module.storage.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.storage.entity.SysFile;
import cn.gaifan.douyinOperations.module.storage.repository.SysFileRepository;
import cn.gaifan.douyinOperations.module.storage.vo.SysFileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SysFileServiceImpl 单元测试")
class SysFileServiceImplTest {

    @Mock
    private SysFileRepository sysFileRepository;

    @InjectMocks
    private SysFileServiceImpl sysFileService;

    private SysFile sampleFile;

    @BeforeEach
    void setUp() {
        sampleFile = new SysFile();
        sampleFile.setId(1L);
        sampleFile.setOwnerId(200L);
        sampleFile.setOriginalName("cover.png");
        sampleFile.setStorageName("cover_1.png");
        sampleFile.setStoragePath("storage/cover_1.png");
        sampleFile.setFileUrl("https://cdn.example.com/cover_1.png");
        sampleFile.setFileType("image");
        sampleFile.setFileExt("png");
        sampleFile.setFileSize(1024L);
        sampleFile.setModule("product");
        sampleFile.setProvider("bos");
        sampleFile.setDeleted(0);
        sampleFile.setCreateTime(Timestamp.valueOf("2026-04-10 10:00:00"));
    }

    @Test
    @DisplayName("getById 对非法 ID 应抛出校验异常")
    void getById_shouldRejectInvalidId() {
        BusinessException exception = assertThrows(BusinessException.class, () -> sysFileService.getById(0L));

        assertEquals(ErrorCode.VALIDATION_FAIL, exception.getCode());
        verifyNoInteractions(sysFileRepository);
    }

    @Test
    @DisplayName("getById 应返回映射后的文件详情")
    void getById_shouldMapEntityToViewObject() {
        when(sysFileRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(sampleFile));

        SysFileVO result = sysFileService.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals(200L, result.getOwnerId());
        assertEquals("cover.png", result.getOriginalName());
        assertEquals("bos", result.getProvider());
        assertEquals("product", result.getModule());
    }

    @Test
    @DisplayName("delete 应执行软删除")
    void delete_shouldMarkEntityAsDeleted() {
        when(sysFileRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(sampleFile));

        sysFileService.delete(1L);

        assertEquals(1, sampleFile.getDeleted());
        verify(sysFileRepository).save(sampleFile);
    }

    @Test
    @DisplayName("delete 文件不存在时应抛出数据不存在异常")
    void delete_shouldThrowWhenFileDoesNotExist() {
        when(sysFileRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> sysFileService.delete(1L));

        assertEquals(ErrorCode.DATA_NOT_FOUND, exception.getCode());
        assertTrue(exception.getMessage().contains("文件不存在"));
    }
}
