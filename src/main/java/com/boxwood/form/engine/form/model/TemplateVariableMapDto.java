package com.boxwood.form.engine.form.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 변수 맵 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVariableMapDto {
    private String templateName;
    private Map<String, Object> requiredVariables;
    private Map<String, Object> formVariables;
    private Map<String, Object> allVariables;
    private List<String> missingVariables;
    private Map<String, String> variableDescriptions;
}
