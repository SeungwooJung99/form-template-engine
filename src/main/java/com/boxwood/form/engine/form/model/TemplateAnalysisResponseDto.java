package com.boxwood.form.engine.form.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * 템플릿 분석 결과 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateAnalysisResponseDto {
    private String templateName;
    private boolean templateValid;
    private Set<String> requiredExternalVariables;
    private Set<String> formVariables;
    private Set<String> assignedVariables;
    private Set<String> localVariables;
    private Set<String> globalVariables;
    private Set<String> loopVariables;
    private Map<String, TemplateVariableDto> referencedVariables;
    private Map<String, List<String>> macros;
    private Map<String, List<String>> functions;
    private Set<String> macroCalls;
    private Set<String> includedTemplates;
    private Map<String, String> importedTemplates;
    private List<String> errors;
    private String summary;

}
