package com.github.cadecode.xboot.starter.mybatis.convertor;

/**
 * 枚举到数据库的转换接口
 * <p>
 * 实现本接口的枚举，按 {@link #dbValue()} 的数值落库（数据库中一般是 tinyint），
 * 读回时由 {@link DefaultEnumTypeHandler} 还原。未实现本接口的枚举仍按 MyBatis
 * 默认的 name() 落库，也可以用 MyBatis-Plus 原生的 {@code @EnumValue}。
 *
 * @author Cade Li
 * @since 2023/6/8
 */
public interface DbEnumConvertible {

    /**
     * 返回用于数据库持久化的数值
     * <p>数据库使用数值类型存储，如 tinyint unsigned 0~255
     */
    Integer dbValue();
}
