package cn.gaifan.douyinOperations.module.storage.service.impl;

import cn.gaifan.douyinOperations.module.storage.entity.BosFileMetadata;
import cn.gaifan.douyinOperations.module.storage.repository.BosFileMetadataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BosFileMetadataServiceImpl 单元测试")
class BosFileMetadataServiceImplTest {

    @Mock
    private BosFileMetadataRepository repository;

    @InjectMocks
    private BosFileMetadataServiceImpl bosFileMetadataService;

    @Test
    @DisplayName("recordUpload 参数不完整时应直接忽略")
    void recordUpload_shouldSkipWhenRequiredFieldsMissing() {
        bosFileMetadataService.recordUpload(" ", 100L, 1L, "image", 1024L, 9L, "https://example.com/a.png");
        bosFileMetadataService.recordUpload("bos/key", null, 1L, "image", 1024L, 9L, "https://example.com/a.png");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("recordUpload 应写入标准化后的元数据")
    void recordUpload_shouldNormalizeAndPersistMetadata() {
        bosFileMetadataService.recordUpload(" bos/key-1 ", 100L, 5L, "", 1024L * 1024L * 1024L, 8L, "");

        verify(repository).save(org.mockito.ArgumentMatchers.argThat((BosFileMetadata metadata) ->
                "bos/key-1".equals(metadata.getBosKey())
                        && metadata.getUserId().equals(100L)
                        && metadata.getTaskId().equals(5L)
                        && "unknown".equals(metadata.getCategory())
                        && metadata.getFileSize().equals(1024L * 1024L * 1024L)
                        && metadata.getShotId().equals(8L)
                        && metadata.getStorageCostMonthly().compareTo(BigDecimal.ZERO) > 0
                        && metadata.getSourceUrl() == null
        ));
    }

    @Test
    @DisplayName("incrementUsageCount 应增加使用次数并保存")
    void incrementUsageCount_shouldIncrementUsageCount() {
        BosFileMetadata metadata = new BosFileMetadata();
        metadata.setBosKey("bos/key-2");
        metadata.setUsageCount(2);

        when(repository.findByBosKeyAndDeleted("bos/key-2", 0)).thenReturn(Optional.of(metadata));

        bosFileMetadataService.incrementUsageCount(" bos/key-2 ");

        assertEquals(3, metadata.getUsageCount());
        verify(repository).save(metadata);
    }

    @Test
    @DisplayName("incrementUsageCount 找不到记录时不应保存")
    void incrementUsageCount_shouldDoNothingWhenMetadataDoesNotExist() {
        when(repository.findByBosKeyAndDeleted("bos/missing", 0)).thenReturn(Optional.empty());

        bosFileMetadataService.incrementUsageCount("bos/missing");

        verify(repository, never()).save(any(BosFileMetadata.class));
    }

    @Test
    @DisplayName("recordUpload 仓储异常时应吞掉异常避免主流程失败")
    void recordUpload_shouldSwallowRepositoryExceptions() {
        when(repository.save(any(BosFileMetadata.class))).thenThrow(new RuntimeException("db error"));

        bosFileMetadataService.recordUpload("bos/key-3", 100L, null, "image", -1L, null, null);

        verify(repository).save(any(BosFileMetadata.class));
    }
}
