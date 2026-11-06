package com.github.cadecode.xboot.main.starter.datasource;

import com.github.cadecode.xboot.main.starter.mybatis.MybatisTestUser;
import com.github.cadecode.xboot.main.starter.mybatis.MybatisTestUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
@ActiveProfiles("dev")
class DynamicDsTests {

    @Autowired
    private MybatisTestUserService service;

    @Test
    void c1_masterAndSlaveAreDifferent() {
        List<MybatisTestUser> master = service.listAll();
        List<MybatisTestUser> slave = service.listSlave();
        System.out.println("[C1] master(x_boot)=" + master.size() + " 行, slave(test)=" + slave.size() + " 行");
        assertEquals(3, master.size());
        assertEquals(2, slave.size());
        assertNotEquals(master.size(), slave.size());
    }

    @Test
    @Transactional
    void c2_dsInsideTransactionKeepsMaster() {
        System.out.println("  先查 master: " + service.listAll().size() + " 行");
        List<MybatisTestUser> slaveInTx = service.listSlave();
        System.out.println("[C2] 事务内 @DS(\"slave\") 实际行数=" + slaveInTx.size()
                + "（已知限制：事务已绑定连接，切换不生效）");
        assertEquals(3, slaveInTx.size(),
                "事务内应仍走 master（3 行）；若不是 3 行说明动态数据源在事务内也切换了");
    }
}
