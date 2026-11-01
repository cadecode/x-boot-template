package com.github.cadecode.xboot.starter.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.github.cadecode.xboot.starter.mybatis.spi.EntityFillResolver;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 公共字段填充器
 * <p>
 * 配合 {@code BaseEntity} 上的 {@code @TableField(fill = ...)} 使用，填充 create_time、
 * create_user、update_time、update_user。
 * <p>
 * 说明：填充只对 SqlCommandType 为 INSERT/UPDATE 的语句生效，自定义 XML 的 insert/update 同样会被填充，
 * 前提是参数中能识别到实体（直接传实体，或参数中带 key 为 et 的实体）。
 *
 * @author Cade Li
 * @since 2024/5/20
 */
public class BaseEntityFillHandler implements MetaObjectHandler {

    private final EntityFillResolver entityFillResolver;

    public BaseEntityFillHandler(EntityFillResolver entityFillResolver) {
        this.entityFillResolver = entityFillResolver;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        String user = currentUser();
        if (Objects.nonNull(user)) {
            strictInsertFill(metaObject, "createUser", String.class, user);
            strictInsertFill(metaObject, "updateUser", String.class, user);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        String user = currentUser();
        if (Objects.nonNull(user)) {
            strictUpdateFill(metaObject, "updateUser", String.class, user);
        }
    }

    private String currentUser() {
        return Objects.isNull(entityFillResolver) ? null : entityFillResolver.currentUser();
    }
}
