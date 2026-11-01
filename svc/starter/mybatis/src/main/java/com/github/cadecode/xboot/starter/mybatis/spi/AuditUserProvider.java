package com.github.cadecode.xboot.starter.mybatis.spi;

/**
 * 审计字段取值提供者
 * <p>
 * 填充 create_user、update_user 时使用。当前模板尚无认证体系，默认没有实现，
 * 填充器取不到用户时跳过这两个字段。接入用户模块后提供实现即可：
 *
 * <pre>
 * &#64;Bean
 * public AuditUserProvider auditUserProvider() {
 *     return () -&gt; SecurityContextHolder.getUserName();
 * }
 * </pre>
 *
 * @author Cade Li
 * @since 2024/5/20
 */
public interface AuditUserProvider {

    /**
     * 当前操作人，取不到时返回 null
     */
    String currentUser();
}
