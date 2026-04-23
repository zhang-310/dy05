package cn.gaifan.douyinOperations.module.config.service.impl;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.config.entity.SysConfig;
import cn.gaifan.douyinOperations.module.config.repository.ConfigVersionHistoryRepository;
import cn.gaifan.douyinOperations.module.config.repository.SysConfigRepository;
import cn.gaifan.douyinOperations.module.config.vo.ConfigSaveVO;
import cn.gaifan.douyinOperations.module.config.vo.ConfigSearchVO;
import cn.gaifan.douyinOperations.module.config.vo.ConfigVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigServiceImpl 配置服务测试")
class ConfigServiceImplTest {

    @InjectMocks
    private ConfigServiceImpl configService;

    @Mock
    private SysConfigRepository sysConfigRepository;

    @Mock
    private ConfigVersionHistoryRepository configVersionHistoryRepository;

    private SysConfig buildConfig(Long id, String key, String value, int sensitive) {
        SysConfig c = new SysConfig();
        c.setId(id);
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setValueType("string");
        c.setIsSensitive(sensitive);
        c.setDeleted(0);
        c.setCreateTime(new Timestamp(System.currentTimeMillis()));
        return c;
    }

    @Nested
    @DisplayName("search 分页搜索")
    class SearchTests {

        @Test
        void search_withNullVO_shouldUseDefaults() {
            when(sysConfigRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            PageResultVO<ConfigVO> result = configService.search(null);
            assertThat(result.getList()).isEmpty();
            assertThat(result.getTotal()).isEqualTo(0L);
        }

        @Test
        void search_withData_shouldReturnMappedVOs() {
            SysConfig c = buildConfig(1L, "site.name", "抖音运营", 0);
            when(sysConfigRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(c)));

            ConfigSearchVO vo = new ConfigSearchVO();
            PageResultVO<ConfigVO> result = configService.search(vo);

            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getConfigKey()).isEqualTo("site.name");
            assertThat(result.getList().get(0).getConfigValue()).isEqualTo("抖音运营");
        }

        @Test
        void search_sensitiveValue_shouldBeMasked() {
            SysConfig c = buildConfig(1L, "db.password", "mysecretpassword", 1);
            when(sysConfigRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(c)));

            PageResultVO<ConfigVO> result = configService.search(new ConfigSearchVO());

            assertThat(result.getList().get(0).getConfigValue()).isEqualTo("****word");
        }

        @Test
        void search_shortSensitiveValue_shouldNotBeMasked() {
            SysConfig c = buildConfig(1L, "key", "ab", 1);
            when(sysConfigRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(c)));

            PageResultVO<ConfigVO> result = configService.search(new ConfigSearchVO());
            assertThat(result.getList().get(0).getConfigValue()).isEqualTo("ab");
        }
    }

    @Nested
    @DisplayName("getByKey 按键获取")
    class GetByKeyTests {

        @Test
        void getByKey_exists_shouldReturnVO() {
            SysConfig c = buildConfig(1L, "site.name", "test", 0);
            when(sysConfigRepository.findByConfigKeyAndDeleted("site.name", 0))
                    .thenReturn(Optional.of(c));

            ConfigVO vo = configService.getByKey("site.name");
            assertThat(vo).isNotNull();
            assertThat(vo.getConfigKey()).isEqualTo("site.name");
        }

        @Test
        void getByKey_notExists_shouldReturnNull() {
            when(sysConfigRepository.findByConfigKeyAndDeleted("missing", 0))
                    .thenReturn(Optional.empty());

            assertThat(configService.getByKey("missing")).isNull();
        }

        @Test
        void getByKey_nullKey_shouldReturnNull() {
            assertThat(configService.getByKey(null)).isNull();
        }

        @Test
        void getByKey_emptyKey_shouldReturnNull() {
            assertThat(configService.getByKey("  ")).isNull();
        }
    }

    @Nested
    @DisplayName("save 保存配置")
    class SaveTests {

        @Test
        void save_newConfig_shouldCreate() {
            when(sysConfigRepository.findByConfigKeyAndDeleted("new.key", 0))
                    .thenReturn(Optional.empty());
            when(sysConfigRepository.save(any(SysConfig.class))).thenAnswer(invocation -> {
                SysConfig arg = invocation.getArgument(0);
                arg.setId(10L);
                return arg;
            });

            ConfigSaveVO vo = new ConfigSaveVO();
            vo.setConfigKey("new.key");
            vo.setConfigValue("value");

            long id = configService.save(vo, 1L);
            assertThat(id).isEqualTo(10L);
            verify(sysConfigRepository).save(any(SysConfig.class));
        }

        @Test
        void save_existingConfig_shouldUpdate() {
            SysConfig existing = buildConfig(5L, "old.key", "old", 0);
            when(sysConfigRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(sysConfigRepository.save(any(SysConfig.class))).thenReturn(existing);

            ConfigSaveVO vo = new ConfigSaveVO();
            vo.setId(5L);
            vo.setConfigKey("old.key");
            vo.setConfigValue("new");

            long id = configService.save(vo, 1L);
            assertThat(id).isEqualTo(5L);
        }

        @Test
        void save_duplicateKey_shouldThrow() {
            when(sysConfigRepository.findByConfigKeyAndDeleted("dup.key", 0))
                    .thenReturn(Optional.of(new SysConfig()));

            ConfigSaveVO vo = new ConfigSaveVO();
            vo.setConfigKey("dup.key");
            vo.setConfigValue("val");

            assertThatThrownBy(() -> configService.save(vo, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("配置键已存在");
        }

        @Test
        void save_nullVO_shouldThrow() {
            assertThatThrownBy(() -> configService.save(null, 1L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void save_notFoundById_shouldThrow() {
            when(sysConfigRepository.findById(999L)).thenReturn(Optional.empty());

            ConfigSaveVO vo = new ConfigSaveVO();
            vo.setId(999L);
            vo.setConfigKey("key");

            assertThatThrownBy(() -> configService.save(vo, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("配置不存在");
        }
    }

    @Nested
    @DisplayName("deleteById 删除配置")
    class DeleteTests {

        @Test
        void deleteById_exists_shouldSoftDelete() {
            SysConfig c = buildConfig(1L, "key", "val", 0);
            when(sysConfigRepository.findById(1L)).thenReturn(Optional.of(c));

            configService.deleteById(1L);

            assertThat(c.getDeleted()).isEqualTo(1);
            verify(sysConfigRepository).save(c);
        }

        @Test
        void deleteById_notExists_shouldDoNothing() {
            when(sysConfigRepository.findById(999L)).thenReturn(Optional.empty());
            configService.deleteById(999L);
            verify(sysConfigRepository, never()).save(any());
        }

        @Test
        void deleteById_null_shouldDoNothing() {
            configService.deleteById(null);
            verify(sysConfigRepository, never()).findById(any());
        }
    }
}
