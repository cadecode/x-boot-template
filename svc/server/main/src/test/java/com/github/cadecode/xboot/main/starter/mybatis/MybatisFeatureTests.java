package com.github.cadecode.xboot.main.starter.mybatis;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class MybatisFeatureTests {

    @Autowired
    private MybatisTestUserMapper mapper;

    @Test
    void a1_contextLoads() {
        assertNotNull(mapper);
        System.out.println("[A1] 上下文启动成功");
    }

    @Test
    void a2_selectAll() {
        List<MybatisTestUser> list = mapper.selectList(null);
        list.forEach(u -> System.out.println("  " + u.getId() + " / " + u.getName() + " / " + u.getAge()
                + " / ct=" + u.getCreateTime() + " / cu=" + u.getCreateUser()));
        assertEquals(3, list.size(), "x_boot.test_user 应有 3 行");
    }

    @Test
    void a3_insertFillsAuditFields() {
        MybatisTestUser user = new MybatisTestUser();
        user.setId("TEST-INS-1");
        user.setName("inserted");
        user.setAge(40);
        int rows = mapper.insert(user);
        assertEquals(1, rows);

        MybatisTestUser saved = mapper.selectById("TEST-INS-1");
        System.out.println("[A3] 插入后 createTime=" + saved.getCreateTime() + ", updateTime=" + saved.getUpdateTime()
                + ", createUser=" + saved.getCreateUser() + ", updateUser=" + saved.getUpdateUser());
        assertNotNull(saved.getCreateTime(), "create_time 应由 BaseEntity 填充");
        assertNotNull(saved.getUpdateTime(), "update_time 应由 BaseEntity 填充");
        assertNull(saved.getCreateUser(), "EntityFillResolver 默认返回 null，create_user 应保持为空");
        assertNull(saved.getUpdateUser(), "EntityFillResolver 默认返回 null，update_user 应保持为空");
    }

    @Test
    void a4_updateFillsUpdateTime() {
        MybatisTestUser before = mapper.selectById("UUID0001");
        before.setName("Cade-updated");
        mapper.updateById(before);

        MybatisTestUser after = mapper.selectById("UUID0001");
        System.out.println("[A4] name=" + after.getName() + ", createTime=" + after.getCreateTime()
                + ", updateTime=" + after.getUpdateTime());
        assertEquals(before.getCreateTime(), after.getCreateTime(), "create_time 更新时不应变化");
        assertEquals(1, mapper.selectList(null).stream().filter(u -> "Cade-updated".equals(u.getName())).count());
    }

    @Test
    void a5_pageHelperPaginates() {
        PageHelper.startPage(1, 2);
        List<MybatisTestUser> list = mapper.selectList(null);
        PageInfo<MybatisTestUser> info = new PageInfo<>(list);
        System.out.println("[A5] PageHelper total=" + info.getTotal() + ", pageSize=" + list.size());
        assertEquals(3, info.getTotal());
        assertEquals(2, list.size());
    }

    @Test
    void a7_cteWithPageHelper() {
        PageHelper.startPage(1, 2);
        List<MybatisTestUser> list = mapper.selectFromCte();
        PageInfo<MybatisTestUser> info = new PageInfo<>(list);
        System.out.println("[A7] CTE + PageHelper: total=" + info.getTotal() + ", pageSize=" + list.size());
        assertEquals(3, info.getTotal());
        assertEquals(2, list.size());
    }

    @Test
    void a6_mpSelectPageWithoutPlugin() {
        Page<MybatisTestUser> page = new Page<>(1, 2);
        Page<MybatisTestUser> result = mapper.selectPage(page, null);
        System.out.println("[A6] MP selectPage: records=" + result.getRecords().size() + ", total=" + result.getTotal()
                + "  （ADR 0004 断言：没有分页插件时不分页）");
        assertEquals(3, result.getRecords().size(), "MP 分页插件未注册，应返回全部 3 行");
    }
}
