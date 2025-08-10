package com.boxwood.form.engine.form.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


/**
 * 템플릿 렌더링 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRenderRequestDto {
    private String templateName;
    private Map<String, Object> variables;
    private boolean includeDebugInfo;
}
