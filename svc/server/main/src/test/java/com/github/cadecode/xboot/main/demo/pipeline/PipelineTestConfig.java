package com.github.cadecode.xboot.main.demo.pipeline;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Pipeline 测试配置注册
 *
 * @author Cade Li
 * @since 2026/6/28
 */
@TestConfiguration
@EnableConfigurationProperties(PipelineTestProperties.class)
public class PipelineTestConfig {
}
