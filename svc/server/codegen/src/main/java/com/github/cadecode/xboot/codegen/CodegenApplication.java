package com.github.cadecode.xboot.codegen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 代码生成工具启动类
 * <p>
 * 当前只包含数据库文档导出（{@code DbDocKit}），后续可在此模块继续加其他生成能力。
 *
 * @author Cade Li
 * @since 2026/11/14
 */
@SpringBootApplication
public class CodegenApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodegenApplication.class, args);
    }
}
