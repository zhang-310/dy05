package cn.gaifan.douyinOperations.module.log.service.impl;

import cn.gaifan.douyinOperations.common.entity.AuditLog;
import cn.gaifan.douyinOperations.common.repository.AuditLogRepository;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.log.vo.LogAuditSearchVO;
import cn.gaifan.douyinOperations.module.log.vo.LogAuditVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    void search_shouldMapAuditLogFieldsToVO() {
        AuditLog entity = AuditLog.builder()
                .id(7L)
                .userId(1L)
                .username("admin")
                .action("UPDATE")
                .entity("product")
                .entityId(9L)
                .oldValue("{\"name\":\"old\"}")
                .newValue("{\"name\":\"new\"}")
                .ip("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .status(1)
                .createTime(LocalDateTime.of(2026, 5, 21, 10, 0))
                .build();
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        LogAuditSearchVO query = new LogAuditSearchVO();
        query.setPage(0);
        query.setRows(20);
        query.setUsername("admin");
        PageResultVO<LogAuditVO> result = auditLogService.search(query);

        assertThat(result.getTotal()).isEqualTo(1);
        LogAuditVO vo = result.getList().get(0);
        assertThat(vo.getUsername()).isEqualTo("admin");
        assertThat(vo.getEntity()).isEqualTo("product");
        assertThat(vo.getTargetId()).isEqualTo(9L);
        assertThat(vo.getBeforeValue()).isEqualTo("{\"name\":\"old\"}");
        assertThat(vo.getAfterValue()).isEqualTo("{\"name\":\"new\"}");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }
}
