package com.github.cadecode.xboot.starter.mybatis.handler;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.github.cadecode.xboot.starter.mybatis.aspect.DataScopeAspect;
import com.github.cadecode.xboot.starter.mybatis.aspect.DataScopeAspect.DataScopeParam;
import com.github.cadecode.xboot.starter.mybatis.spi.DataScopeResolver;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import java.util.List;
import java.util.Objects;

/**
 * 数据权限处理器
 * <p>
 * 用 MyBatis-Plus 的 {@link MultiDataPermissionHandler}：SQL 里的每张表都会回调一次，
 * 只有命中 {@code @DataScope#tables()} 的表才追加 {@code field IN (...)}，
 * 这样 join 里的字典表、关联表不会被误加条件。返回 null 表示不改动。
 *
 * @author Cade Li
 * @since 2024/5/23
 */
public class DataScopePermissionHandler implements MultiDataPermissionHandler {

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        DataScopeParam param = DataScopeAspect.currDataScope();
        if (Objects.isNull(param) || !param.isEnableFilter() || !param.matchTable(table.getName())) {
            return null;
        }
        DataScopeResolver resolver = param.getResolver();
        // 解析器缺失、管理员、没配范围时不限制
        if (Objects.isNull(resolver) || resolver.isAdmin()) {
            return null;
        }
        List<Object> scopes = resolver.getScopes();
        if (ObjUtil.isEmpty(scopes)) {
            return null;
        }
        List<StringValue> values = scopes.stream()
                .filter(Objects::nonNull)
                .map(scope -> new StringValue(scope.toString()))
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        // 右侧必须是 ParenthesedExpressionList：ExpressionList 渲染出来没有括号，会拼出 `age IN '27'`
        return new InExpression(new Column(param.getField()), new ParenthesedExpressionList<>(values));
    }
}
