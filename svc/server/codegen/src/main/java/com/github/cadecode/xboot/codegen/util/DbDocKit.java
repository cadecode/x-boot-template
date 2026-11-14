package com.github.cadecode.xboot.codegen.util;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.smallbun.screw.core.Configuration;
import cn.smallbun.screw.core.engine.EngineConfig;
import cn.smallbun.screw.core.engine.EngineFileType;
import cn.smallbun.screw.core.engine.EngineTemplateType;
import cn.smallbun.screw.core.execute.DocumentationExecute;
import cn.smallbun.screw.core.process.ProcessConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 数据库文档导出工具
 * <p>
 * screw 内部把 DataSource 强转成 HikariDataSource（AbstractDatabaseQuery#getSchema），
 * 所以本模块的连接池必须是 Hikari，不能引 druid。
 *
 * @author Cade Li
 * @since 2026/11/14
 */
@Component
@RequiredArgsConstructor
public class DbDocKit {

    private final DataSource dataSource;

    /**
     * 导出全部表，但排除掉指定的表 / 表前缀 / 表后缀，三者都为空时即导出全部表
     */
    public void exportIgnore(DbDocParam param, String ignoreTablePrefix, String ignoreTableSuffix, String[] ignoreTableNames) {
        ProcessConfig processConfig = ProcessConfig.builder()
                .designatedTableName(Collections.emptyList())
                .designatedTablePrefix(Collections.emptyList())
                .designatedTableSuffix(Collections.emptyList())
                .ignoreTableName(listOf(ignoreTableNames))
                .ignoreTablePrefix(singleOf(ignoreTablePrefix))
                .ignoreTableSuffix(singleOf(ignoreTableSuffix))
                .build();
        execute(param, processConfig);
    }

    private void execute(DbDocParam param, ProcessConfig processConfig) {
        EngineConfig engineConfig = EngineConfig.builder()
                .fileOutputDir(param.getOutDir())
                // 关闭，打开文件目录存在兼容性问题
                .openOutputDir(false)
                .fileType(param.getFileType())
                .produceType(EngineTemplateType.freemarker)
                .fileName(param.getFileName())
                .build();
        Configuration configuration = Configuration.builder()
                .version(StrUtil.nullToEmpty(param.getVersion()))
                .description(StrUtil.nullToEmpty(param.getDescription()))
                .dataSource(dataSource)
                .engineConfig(engineConfig)
                .produceConfig(processConfig)
                .build();
        new DocumentationExecute(configuration).execute();
    }

    /**
     * 数据库文档生成参数
     */
    @Data
    @AllArgsConstructor
    public static class DbDocParam {

        /** 产物类型 */
        private EngineFileType fileType;

        /** 文件名，不含后缀 */
        private String fileName;

        /** 输出目录 */
        private String outDir;

        /** 文档版本 */
        private String version;

        /** 文档描述 */
        private String description;
    }

    private static List<String> listOf(String[] values) {
        if (ArrayUtil.isEmpty(values)) {
            return Collections.emptyList();
        }
        return Arrays.stream(values).filter(StrUtil::isNotBlank).toList();
    }

    private static List<String> singleOf(String value) {
        return StrUtil.isBlank(value) ? Collections.emptyList() : Collections.singletonList(value);
    }
}
