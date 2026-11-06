package com.github.cadecode.xboot.main.starter.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.cadecode.xboot.starter.mybatis.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("test_user")
public class MybatisTestUser extends BaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;

    private Integer age;
}
