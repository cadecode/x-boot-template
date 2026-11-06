package com.github.cadecode.xboot.main.starter.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MybatisTestUserMapper extends BaseMapper<MybatisTestUser> {

    @Select("select id, name, age from test_user where id = #{id}")
    MybatisTestUser selectRawById(@Param("id") String id);

    @Select("select count(*) from information_schema.tables where table_schema = 'x_boot'")
    int countInformationSchemaTables();

    @Select("with t as (select id, name, age from test_user) select id, name, age from t")
    List<MybatisTestUser> selectFromCte();
}
