package com.boxwood.form.engine.form.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 템플릿 렌더링 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRenderResponseDto {
    private String templateName;
    private String renderedHtml;
    private boolean success;
    private List<String> errors;
    private Map<String, Object> debugInfo;
    private long renderTimeMs;
}
