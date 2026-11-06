package com.github.cadecode.xboot.main.starter.mybatis;

import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class DataScopeTests {

    @Autowired
    private MybatisTestUserService service;

    @Autowired
    private MybatisTestClassScopedService classScopedService;

    @BeforeEach
    void setUp() {
        MybatisTestScopeResolver.reset();
    }

    @AfterEach
    void tearDown() {
        MybatisTestScopeResolver.reset();
    }

    @Test
    void b1_scopedByAge() {
        List<MybatisTestUser> list = service.listScoped();
        System.out.println("[B1] @DataScope(field=age, tables=test_user), scopes=[27] -> " + list.size() + " 行");
        assertEquals(2, list.size());
    }

    @Test
    void b2_noAnnotationNoFilter() {
        List<MybatisTestUser> list = service.listAll();
        System.out.println("[B2] 未标注解 -> " + list.size() + " 行（拦截器短路，不解析 SQL）");
        assertEquals(3, list.size());
    }

    @Test
    void b3_enableFilterFalse() {
        List<MybatisTestUser> list = service.listScopedDisabled();
        System.out.println("[B3] enableFilter=false -> " + list.size() + " 行");
        assertEquals(3, list.size());
    }

    @Test
    void b4_adminSkipsFilter() {
        MybatisTestScopeResolver.ADMIN = true;
        List<MybatisTestUser> list = service.listScoped();
        System.out.println("[B4] isAdmin=true -> " + list.size() + " 行");
        assertEquals(3, list.size());
    }

    @Test
    void b5_tableWhitelistSkipsOtherTables() {
        int count = service.countInformationSchema();
        System.out.println("[B5] tables={test_user} 时查 information_schema.tables -> " + count
                + "（若误加条件会报 Unknown column）");
        assertTrue(count > 0);
    }

    @Test
    void b6_classAnnotationApplies() {
        List<MybatisTestUser> list = classScopedService.listByClassAnnotation();
        System.out.println("[B6] 类上 @DataScope(field=age) -> " + list.size() + " 行");
        assertEquals(2, list.size());
    }

    @Test
    void b7_methodAnnotationOverridesWhole() {
        Exception ex = assertThrows(Exception.class, () -> classScopedService.listByMethodAnnotation());
        System.out.println("[B7] 类上 field=age、方法上只写 tables -> 整条回退，field 回落 data_scope -> "
                + ex.getClass().getSimpleName() + ": " + firstLine(ex.getMessage()));
    }

    @Test
    void b8_updateRespectsScope() {
        int affected = service.updateAllAges(99);
        System.out.println("[B8] 无 where 的全表 UPDATE 受数据权限约束，affected=" + affected);
        assertEquals(2, affected, "只有 age=27 的 2 行应被更新");

        long still28 = service.listAll().stream().filter(u -> Integer.valueOf(28).equals(u.getAge())).count();
        assertEquals(1, still28, "age=28 的行不应被更新");
    }

    @Test
    void b9_pageCountCarriesScope() {
        PageInfo<MybatisTestUser> info = service.pageScoped();
        System.out.println("[B9] PageHelper 分页 + 数据权限: total=" + info.getTotal() + ", pageSize="
                + info.getList().size() + "（count SQL 应带权限条件）");
        assertEquals(2, info.getTotal());
        assertEquals(1, info.getList().size());
    }

    private static String firstLine(String msg) {
        if (msg == null) {
            return "null";
        }
        int idx = msg.indexOf('\n');
        return idx < 0 ? msg : msg.substring(0, idx);
    }
}
