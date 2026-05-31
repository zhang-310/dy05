package cn.gaifan.douyinOperations.module.log.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.log.service.AuditLogService;
import cn.gaifan.douyinOperations.module.log.vo.LogAuditSearchVO;
import cn.gaifan.douyinOperations.module.log.vo.LogAuditVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogAuditControllerTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private LogAuditController logAuditController;

    @Test
    void search_shouldReturnAuditLogPage() {
        LogAuditVO row = new LogAuditVO();
        row.setId(1L);
        row.setUsername("admin");
        row.setEntity("product");
        row.setBeforeValue("{\"name\":\"old\"}");
        row.setAfterValue("{\"name\":\"new\"}");
        row.setCreateTime(LocalDateTime.of(2026, 5, 21, 10, 0));
        when(auditLogService.search(any())).thenReturn(PageResultVO.of(1L, List.of(row), 0, 20));

        PageResultVO<LogAuditVO> result = logAuditController.search(new LogAuditSearchVO()).getData();

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getUsername()).isEqualTo("admin");
        verify(auditLogService).search(any());
    }
}
