package com.github.cadecode.xboot.starter.mybatis.convertor;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Java 布尔值与数据库数字类型互转（库中一般用 tinyint(1)）
 * <p>
 * 用法：在实体字段上显式指定，如
 * {@code @TableField(typeHandler = BoolToIntTypeHandler.class) private Boolean enableFlag;}
 *
 * @author Cade Li
 * @since 2023/4/25
 */
@MappedTypes({Boolean.class})
@MappedJdbcTypes({JdbcType.INTEGER})
public class BoolToIntTypeHandler extends BaseTypeHandler<Boolean> {

    /**
     * 供 Wrapper 条件中指定 typeHandler 使用，如
     * {@code .eq("typeHandler=BoolToIntTypeHandler", Xxx::getFlag, BoolToIntTypeHandler.mapping(true))}
     */
    public static final String MAPPING = "typeHandler=BoolToIntTypeHandler";

    public static Integer mapping(Boolean o) {
        return Objects.isNull(o) ? null : (o ? 1 : 0);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter ? 1 : 0);
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getInt(columnName) != 0;
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getInt(columnIndex) != 0;
    }

    @Override
    public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getInt(columnIndex) != 0;
    }
}
