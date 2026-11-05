package com.github.cadecode.xboot.starter.mybatis.interceptor;

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.github.cadecode.xboot.starter.mybatis.aspect.DataScopeAspect;
import com.github.cadecode.xboot.starter.mybatis.aspect.DataScopeAspect.DataScopeParam;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * 数据权限拦截器
 * <p>
 * MyBatis-Plus 的 {@link DataPermissionInterceptor} 在 {@code beforeQuery} 里无条件把 SQL 解析成 AST，
 * 只有 Mapper 方法上标了 {@code @InterceptorIgnore(dataPermission = "1")} 才跳过。本项目的数据权限由
 * {@link DataScopeAspect} 的注解驱动，绝大多数查询并不需要过滤，所以在父类之前加一道判断：
 * 本次请求没有数据权限参数、或注解里关掉了过滤时直接返回，连解析都不做。
 * <p>
 * {@code beforeQuery} 与 {@code beforePrepare} 都要拦：查询走前者，UPDATE / DELETE 走后者。
 *
 * @author Cade Li
 * @since 2024/6/3
 */
public class DataScopePermissionInterceptor extends DataPermissionInterceptor {

    public DataScopePermissionInterceptor(DataPermissionHandler dataPermissionHandler) {
        super(dataPermissionHandler);
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        if (skip()) {
            return;
        }
        super.beforeQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        if (skip()) {
            return;
        }
        super.beforePrepare(sh, connection, transactionTimeout);
    }

    /**
     * 本次请求不需要数据权限时跳过解析
     */
    private static boolean skip() {
        DataScopeParam dataScopeParam = DataScopeAspect.currDataScope();
        return Objects.isNull(dataScopeParam) || !dataScopeParam.isEnableFilter();
    }
}
