package cn.gaifan.douyinOperations.common.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BasicQueryDto 基础查询参数测试")
class BasicQueryDtoTest {

    @Test
    void defaultValues_shouldBeReasonable() {
        BasicQueryDto dto = new BasicQueryDto();
        assertThat(dto.getPage()).isEqualTo(0);
        assertThat(dto.getRows()).isEqualTo(30);
        assertThat(dto.getSortName()).isEqualTo("id");
        assertThat(dto.getSortOrder()).isEqualTo("desc");
    }

    @Test
    void validateParams_negativePage_shouldResetToZero() {
        BasicQueryDto dto = new BasicQueryDto();
        dto.setPage(-1);
        dto.validateParams();
        assertThat(dto.getPage()).isEqualTo(0);
    }

    @Test
    void validateParams_nullPage_shouldResetToZero() {
        BasicQueryDto dto = new BasicQueryDto();
        dto.setPage(null);
        dto.validateParams();
        assertThat(dto.getPage()).isEqualTo(0);
    }

    @Test
    void validateParams_zeroRows_shouldResetTo30() {
        BasicQueryDto dto = new BasicQueryDto();
        dto.setRows(0);
        dto.validateParams();
        assertThat(dto.getRows()).isEqualTo(30);
    }

    @Test
    void validateParams_excessiveRows_shouldCapAt1000() {
        BasicQueryDto dto = new BasicQueryDto();
        dto.setRows(5000);
        dto.validateParams();
        assertThat(dto.getRows()).isEqualTo(1000);
    }

    @Test
    void validateParams_invalidSortName_shouldResetToId() {
        BasicQueryDto dto = new BasicQueryDto();
        dto.setSortName("DROP TABLE;");
        dto.validateParams();
        assertThat(dto.getSortName()).isEqualTo("id");
    }

    @Test
    void validateParams_validSortName_shouldKeep() {
        BasicQueryDto dto = new BasicQueryDto();
        dto.setSortName("create_time");
        dto.validateParams();
        assertThat(dto.getSortName()).isEqualTo("create_time");
    }

    @Test
    void validateParams_invalidSortOrder_shouldResetToDesc() {
        BasicQueryDto dto = new BasicQueryDto();
        dto.setSortOrder("random");
        dto.validateParams();
        assertThat(dto.getSortOrder()).isEqualTo("desc");
    }

    @Test
    void validateParams_ascSortOrder_shouldKeep() {
        BasicQueryDto dto = new BasicQueryDto();
        dto.setSortOrder("ASC");
        dto.validateParams();
        assertThat(dto.getSortOrder()).isEqualTo("asc");
    }

    @Test
    void getOffset_shouldCalculateCorrectly() {
        BasicQueryDto dto = new BasicQueryDto();
        dto.setPage(2);
        dto.setRows(30);
        assertThat(dto.getOffset()).isEqualTo(60);
    }

    @Test
    void getOffset_nullValues_shouldReturnZero() {
        BasicQueryDto dto = new BasicQueryDto();
        dto.setPage(null);
        dto.setRows(null);
        assertThat(dto.getOffset()).isEqualTo(0);
    }

    @Test
    void isRandom_null_shouldReturnFalse() {
        BasicQueryDto dto = new BasicQueryDto();
        assertThat(dto.isRandom()).isFalse();
    }

    @Test
    void isRandom_true_shouldReturnTrue() {
        BasicQueryDto dto = new BasicQueryDto();
        dto.setRandom(true);
        assertThat(dto.isRandom()).isTrue();
    }

    @Test
    void constructor_withParams_shouldValidate() {
        BasicQueryDto dto = new BasicQueryDto(-1, 0, null, "invalid");
        assertThat(dto.getPage()).isEqualTo(0);
        assertThat(dto.getRows()).isEqualTo(30);
        assertThat(dto.getSortName()).isEqualTo("id");
        assertThat(dto.getSortOrder()).isEqualTo("desc");
    }
}
