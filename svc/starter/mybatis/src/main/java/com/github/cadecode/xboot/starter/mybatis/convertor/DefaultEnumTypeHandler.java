package com.github.cadecode.xboot.starter.mybatis.convertor;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toMap;

/**
 * 默认枚举 TypeHandler（备用，模板未注册）
 * <p>
 * 枚举在数据库里统一按数值落库：实现 {@link DbEnumConvertible} 的取 {@code dbValue()}，
 * 没实现的取 {@code ordinal()}（与 old-dev 行为一致）。
 * <p>
 * 两种开启方式：
 * <ol>
 *     <li>字段级：{@code @TableField(typeHandler = DefaultEnumTypeHandler.class)}</li>
 *     <li>全局级：{@code mybatis-plus.configuration.default-enum-type-handler} 指向本类。
 *     按官方语义，该配置<b>只改变 {@code CompositeEnumTypeHandler#defaultEnumTypeHandler}
 *     的值</b>：通用枚举（实现 {@code IEnum} 或标 {@code @EnumValue}）仍走 MyBatis-Plus
 *     内置处理器、不经过本类；其余枚举才进本类。</li>
 * </ol>
 * 注意 3.5.15 的实际包名是 {@code com.baomidou.mybatisplus.core.handlers.CompositeEnumTypeHandler}
 * （官方文档写作 extension.handlers），文档里的 {@code EnumAnnotationTypeHandler} 该版本已不存在。
 * <p>
 * 用 {@code ordinal()} 兜底是为了配合<b>数值列</b>：枚举名写进 tinyint/int 列会报
 * "Incorrect integer value"。代价是枚举的<b>声明顺序即数据语义</b>——往中间插入或调整顺序，
 * 历史数据会整体错位且不报错。因此业务枚举应尽量用 {@code @EnumValue} 或实现
 * {@link DbEnumConvertible} 显式声明数值，不要依赖 {@code ordinal()}。
 * <p>
 * 与 old-dev 的差别仅在于：这里只做数值映射，不再吞掉非枚举类型。
 *
 * @param <E> 枚举类型
 * @author Cade Li
 * @date 2022/6/16
 */
public class DefaultEnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

    private final Class<E> type;

    /**
     * 数据库数值与枚举常量的映射：实现 DbEnumConvertible 取 dbValue，否则取 ordinal
     */
    private final Map<Integer, E> valueEnumMap;

    public DefaultEnumTypeHandler(Class<E> type) {
        if (Objects.isNull(type)) {
            throw new IllegalArgumentException("type can not be null");
        }
        E[] enums = type.getEnumConstants();
        if (Objects.isNull(enums)) {
            throw new IllegalArgumentException(type.getSimpleName() + " is not an enum type");
        }
        this.type = type;
        this.valueEnumMap = Arrays.stream(enums)
                .collect(toMap(this::valueFromEnum, o -> o, (p, n) -> p));
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        int value = valueFromEnum(parameter);
        if (Objects.isNull(jdbcType)) {
            ps.setInt(i, value);
            return;
        }
        ps.setObject(i, value, jdbcType.TYPE_CODE);
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : enumFromValue(value);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int value = rs.getInt(columnIndex);
        return rs.wasNull() ? null : enumFromValue(value);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int value = cs.getInt(columnIndex);
        return cs.wasNull() ? null : enumFromValue(value);
    }

    /**
     * 枚举 → 数据库数值
     */
    private int valueFromEnum(E e) {
        if (e instanceof DbEnumConvertible convertible) {
            return convertible.dbValue();
        }
        return e.ordinal();
    }

    /**
     * 数据库数值 → 枚举
     */
    private E enumFromValue(int value) {
        E e = valueEnumMap.get(value);
        if (Objects.isNull(e)) {
            throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " value: " + value);
        }
        return e;
    }
}
