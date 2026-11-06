package com.github.cadecode.xboot.main.starter.mybatis;

import com.github.cadecode.xboot.starter.mybatis.annotation.DataScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@DataScope(field = "age", tables = {"test_user"})
public class MybatisTestClassScopedService {

    private final MybatisTestUserMapper mapper;

    public List<MybatisTestUser> listByClassAnnotation() {
        return mapper.selectList(null);
    }

    @DataScope(tables = {"test_user"})
    public List<MybatisTestUser> listByMethodAnnotation() {
        return mapper.selectList(null);
    }
}
