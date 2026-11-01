package com.github.cadecode.xboot.starter.mybatis.convertor;

import com.github.cadecode.xboot.common.util.JacksonUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Java 对象与数据库 varchar 互转，即以 JSON 字符串落库
 * <p>
 * 用法：在实体字段上显式指定，如
 * {@code @TableField(typeHandler = ObjToStrTypeHandler.class) private List&lt;String&gt; roles;}
 *
 * @author Cade Li
 * @since 2023/5/10
 */
@MappedTypes({Object.class, List.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class ObjToStrTypeHandler extends BaseTypeHandler<Object> {

    /**
     * 供 Wrapper 条件中指定 typeHandler 使用
     */
    public static final String MAPPING = "typeHandler=ObjToStrTypeHandler";

    public static String mapping(Object o) {
        return JacksonUtil.toJson(o);
    }

    private final Class<?> type;

    public ObjToStrTypeHandler(Class<?> type) {
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, JacksonUtil.toJson(parameter));
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return JacksonUtil.toBean(rs.getString(columnName), type);
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return JacksonUtil.toBean(rs.getString(columnIndex), type);
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return JacksonUtil.toBean(cs.getString(columnIndex), type);
    }
}
