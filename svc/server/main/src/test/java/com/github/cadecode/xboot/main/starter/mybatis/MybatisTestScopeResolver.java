package com.github.cadecode.xboot.main.starter.mybatis;

import com.github.cadecode.xboot.starter.mybatis.spi.DataScopeResolver;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MybatisTestScopeResolver implements DataScopeResolver {

    public static volatile List<Object> SCOPES = List.of("27");

    public static volatile boolean ADMIN = false;

    @Override
    public List<Object> getScopes() {
        return SCOPES;
    }

    @Override
    public boolean isAdmin() {
        return ADMIN;
    }

    public static void reset() {
        SCOPES = List.of("27");
        ADMIN = false;
    }
}
