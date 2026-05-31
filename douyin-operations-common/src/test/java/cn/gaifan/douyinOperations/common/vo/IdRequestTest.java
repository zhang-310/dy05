package cn.gaifan.douyinOperations.common.vo;

import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import static org.junit.jupiter.api.Assertions.*;

class IdRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validId() {
        var req = new IdRequest();
        req.setId(1L);
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void nullIdFailsValidation() {
        var req = new IdRequest();
        assertFalse(validator.validate(req).isEmpty());
    }
}
