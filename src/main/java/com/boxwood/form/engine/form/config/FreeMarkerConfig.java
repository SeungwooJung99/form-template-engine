package com.boxwood.form.engine.form.config;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class FreeMarkerConfig {

    @Value("${spring.freemarker.template-loader-path:classpath:/templates/freemarker}")
    private String templateLoaderPath;

    @Primary
    @Bean
    public Configuration customFreeMarkerConfiguration(ResourceLoader resourceLoader) throws IOException {
        Configuration config = new Configuration(Configuration.VERSION_2_3_31);

        // 템플릿 로더 설정
        config.setDirectoryForTemplateLoading(
                resourceLoader.getResource(templateLoaderPath).getFile()
        );

        // 기본 설정
        config.setDefaultEncoding(StandardCharsets.UTF_8.name());
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        config.setLogTemplateExceptions(false);
        config.setWrapUncheckedExceptions(true);
        config.setFallbackOnNullLoopVariable(false);

        return config;
    }

}
