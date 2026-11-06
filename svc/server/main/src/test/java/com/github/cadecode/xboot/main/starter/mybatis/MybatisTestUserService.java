package com.github.cadecode.xboot.main.starter.mybatis;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.cadecode.xboot.starter.mybatis.annotation.DataScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MybatisTestUserService {

    private final MybatisTestUserMapper mapper;

    public List<MybatisTestUser> listAll() {
        return mapper.selectList(null);
    }

    @DataScope(field = "age", tables = {"test_user"})
    public List<MybatisTestUser> listScoped() {
        return mapper.selectList(null);
    }

    @DataScope(field = "age", tables = {"test_user"}, enableFilter = false)
    public List<MybatisTestUser> listScopedDisabled() {
        return mapper.selectList(null);
    }

    @DataScope(field = "age", tables = {"test_user"})
    public PageInfo<MybatisTestUser> pageScoped() {
        PageHelper.startPage(1, 1);
        return new PageInfo<>(mapper.selectList(null));
    }

    @DataScope(field = "age", tables = {"test_user"})
    public int updateAllAges(Integer age) {
        MybatisTestUser user = new MybatisTestUser();
        user.setAge(age);
        return mapper.update(user, null);
    }

    @DataScope(field = "age", tables = {"test_user"})
    public int countInformationSchema() {
        return mapper.countInformationSchemaTables();
    }

    @DS("slave")
    public List<MybatisTestUser> listSlave() {
        return mapper.selectList(null);
    }
}
