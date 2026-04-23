package cn.gaifan.douyinOperations.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

/**
 * JPA 属性转换器：将实体中的 String（存 JSON 字符串）与 PostgreSQL jsonb 列互转。
 * 避免 "column metadata is of type jsonb but expression is of type character varying" 错误。
 */
@Converter(autoApply = false)
public class JsonbStringConverter implements AttributeConverter<String, Object> {

    @Override
    public Object convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        PGobject pg = new PGobject();
        pg.setType("jsonb");
        try {
            pg.setValue(attribute);
        } catch (SQLException e) {
            throw new IllegalArgumentException("Invalid JSON string for jsonb: " + e.getMessage(), e);
        }
        return pg;
    }

    @Override
    public String convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }
        if (dbData instanceof PGobject pg) {
            return pg.getValue();
        }
        if (dbData instanceof String s) {
            return s;
        }
        return dbData.toString();
    }
}
