package com.github.cadecode.xboot.starter.mybatis.spi;

import java.util.List;

/**
 * 数据权限范围解析器
 * <p>
 * 由业务侧实现：返回当前用户可访问的数据范围值（如部门 ID 列表）。
 * 没有实现时 starter 提供一个返回 {@code null} 的默认实现，表示不做限制。
 *
 * @author Cade Li
 * @since 2024/5/23
 */
public interface DataScopeResolver {

    /**
     * 可访问的数据范围值，返回 null 或空集合表示不限制
     */
    List<Object> getScopes();

    /**
     * 是否管理员，true 时不做限制
     */
    default boolean isAdmin() {
        return false;
    }
}
