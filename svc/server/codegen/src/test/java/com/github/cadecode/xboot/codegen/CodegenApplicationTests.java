package com.github.cadecode.xboot.codegen;

import cn.smallbun.screw.core.engine.EngineFileType;
import com.github.cadecode.xboot.codegen.util.DbDocKit;
import com.github.cadecode.xboot.codegen.util.DbDocKit.DbDocParam;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 代码生成工具入口
 * <p>
 * 参数写在 genDbDoc 方法里，改完直接运行该测试。
 *
 * @author Cade Li
 * @since 2026/11/14
 */
@SpringBootTest
@ActiveProfiles("dev")
public class CodegenApplicationTests {

    @Autowired
    private DbDocKit dbDocKit;

    /**
     * 生成数据库文档
     * <p>
     * 走排除模式：ignore 参数留空即导出全部表，填了就把匹配的表排除掉。
     */
    @Test
    public void genDbDoc() {
        DbDocParam param = new DbDocParam(
                // 产物类型：MD / HTML / WORD
                EngineFileType.MD,
                // 文件名，不含后缀
                "dbdoc",
                // 输出目录
                "./dbdoc",
                // 文档版本
                "0.0.1-SNAPSHOT",
                // 文档描述
                "数据库设计文档");
        // 忽略表前缀、忽略表后缀、忽略表名，全部为空即导出全部表
        dbDocKit.exportIgnore(param, null, null, new String[]{});
    }
}
