package com.github.cadecode.xboot.starter.mybatis.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类，统一公共审计字段
 * <p>
 * 继承本类的实体，其对应的表必须包含 create_time、create_user、update_time、update_user 四列。
 * <p>
 * 之所以用抽象类而非接口：MyBatis-Plus 的 {@code @TableField} 只能标注在字段上
 * （{@code @Target({FIELD, ANNOTATION_TYPE})}），且 {@code ReflectionKit#getFieldList}
 * 只遍历类继承链、不遍历接口，接口上的注解不会被读取。
 *
 * @author Cade Li
 * @since 2024/5/20
 */
@Getter
@Setter
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建时间，插入时填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 创建人，插入时填充，取值见 {@code EntityFillResolver}
     */
    @TableField(fill = FieldFill.INSERT)
    private String createUser;

    /**
     * 更新时间，插入、更新时填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 更新人，插入、更新时填充，取值见 {@code EntityFillResolver}
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateUser;
}
