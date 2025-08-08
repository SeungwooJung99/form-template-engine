package com.boxwood.form.engine.form.model;

import com.boxwood.form.engine.form.utils.FreeMarkerVariableExtractor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 템플릿 변수 상세 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVariableDto {
    private String name;
    private Set<FreeMarkerVariableExtractor.ExpressionType> usageTypes;
    private boolean isRequired;
    private boolean isFormRelated;
    private String description;
}
