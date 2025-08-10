package com.boxwood.form.engine.form.controller;

import com.boxwood.form.engine.form.model.*;
import com.boxwood.form.engine.form.service.TemplateAnalysisService;
import com.boxwood.form.engine.form.utils.FreeMarkerVariableExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/template")
public class TemplateAnalysisController {

    private final TemplateAnalysisService templateService;

    /**
     * 통합 템플릿 분석 API - 기존 개별 API들의 정보를 모두 포함 (선택적)
     * <p>
     * Query Parameters:
     * - hierarchy: 계층구조 변수 포함 여부 (default: true)
     * - defaults: 기본값이 설정된 변수 포함 여부 (default: true)
     * - preview: 미리보기 HTML 포함 여부 (default: false)
     * - validation: 유효성 검증 정보 포함 여부 (default: true)
     * - statistics: 통계 정보 포함 여부 (default: true)
     * - legacy: 기존 응답 형태 유지 여부 (default: false)
     */
    @GetMapping("/analyze/{templateId}")
    public ResponseEntity<?> analyzeTemplate(
            @PathVariable("templateId") String templateId,
            @RequestParam(value = "hierarchy", defaultValue = "true") boolean includeHierarchy,
            @RequestParam(value = "defaults", defaultValue = "true") boolean includeDefaults,
            @RequestParam(value = "preview", defaultValue = "false") boolean includePreview,
            @RequestParam(value = "validation", defaultValue = "true") boolean includeValidation,
            @RequestParam(value = "statistics", defaultValue = "true") boolean includeStatistics,
            @RequestParam(value = "legacy", defaultValue = "false") boolean legacyFormat) {

        try {
            log.info("Analyzing template: {} with options [hierarchy:{}, defaults:{}, preview:{}, validation:{}, statistics:{}, legacy:{}]",
                    templateId, includeHierarchy, includeDefaults, includePreview, includeValidation, includeStatistics, legacyFormat);

            // 기존 형태 응답이 필요한 경우
            if (legacyFormat) {
                TemplateAnalysisResponseDto legacyResult = templateService.analyzeTemplate(templateId);
                return ResponseEntity.ok(legacyResult);
            }

            // === 새로운 통합 분석 ===
            FreeMarkerVariableExtractor.TemplateVariableAnalysis rawAnalysis = templateService.analyzeTemplateRaw(templateId);

            Map<String, Object> response = new LinkedHashMap<>();

            // === 기본 정보 (항상 포함) ===
            response.put("templateId", templateId);
            response.put("templateName", templateId); // 기존 호환성
            response.put("templateValid", rawAnalysis.isTemplateValid());
            response.put("errors", rawAnalysis.getErrors());
            response.put("analysisTimestamp", System.currentTimeMillis());

            // === 기본 변수 정보 (항상 포함) ===
            Set<String> requiredVariables = rawAnalysis.getRequiredExternalVariables();
            response.put("requiredExternalVariables", requiredVariables);
            response.put("assignedVariables", rawAnalysis.getAssignedVariables());
            response.put("localVariables", rawAnalysis.getLocalVariables());
            response.put("globalVariables", rawAnalysis.getGlobalVariables());
            response.put("loopVariables", rawAnalysis.getLoopVariables());

            // === 폼 관련 변수 ===
            Set<String> formVariables = requiredVariables.stream()
                    .filter(this::isFormRelatedVariable)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            response.put("formVariables", formVariables);

            // === 매크로 및 함수 정보 ===
            response.put("macros", rawAnalysis.getMacros());
            response.put("functions", rawAnalysis.getFunctions());
            response.put("macroCalls", rawAnalysis.getMacroCalls());

            // === 템플릿 의존성 ===
            response.put("includedTemplates", rawAnalysis.getIncludedTemplates());
            response.put("importedTemplates", rawAnalysis.getImportedTemplates());

            // === 상세 변수 정보 ===
            Map<String, TemplateVariableDto> variableDetails = new LinkedHashMap<>();
            rawAnalysis.getReferencedVariables().forEach((varName, usageTypes) -> {
                TemplateVariableDto varDto = TemplateVariableDto.builder()
                        .name(varName)
                        .usageTypes(usageTypes)
                        .isRequired(requiredVariables.contains(varName))
                        .isFormRelated(isFormRelatedVariable(varName))
                        .description(generateUsageDescription(usageTypes))
                        .build();
                variableDetails.put(varName, varDto);
            });
            response.put("referencedVariables", variableDetails);

            // === 계층구조 변수 (선택적) ===
            if (includeHierarchy) {
                response.put("hierarchicalVariables", rawAnalysis.getHierarchicalVariables());
                response.put("hierarchicalVariablesJson", rawAnalysis.getHierarchicalVariablesJson());

                if (includeDefaults) {
                    Map<String, Object> variablesWithDefaults = templateService.getRequiredVariablesWithDefaults(templateId);
                    response.put("hierarchicalVariablesWithDefaults", variablesWithDefaults);
                }
            }

            // === 유효성 검증 정보 (선택적) ===
            if (includeValidation) {
                Map<String, Object> validation = new HashMap<>();
                validation.put("isValid", rawAnalysis.isTemplateValid());
                validation.put("hasErrors", !rawAnalysis.getErrors().isEmpty());
                validation.put("errorCount", rawAnalysis.getErrors().size());
                validation.put("validationMessage", rawAnalysis.isTemplateValid() ?
                        "Template is valid" : "Template has validation errors");
                response.put("validation", validation);
            }

            // === 통계 정보 (선택적) ===
            if (includeStatistics) {
                Map<String, Object> statistics = new HashMap<>();
                statistics.put("totalReferencedVariables", rawAnalysis.getReferencedVariables().size());
                statistics.put("requiredExternalVariablesCount", requiredVariables.size());
                statistics.put("formVariablesCount", formVariables.size());
                statistics.put("assignedVariablesCount", rawAnalysis.getAssignedVariables().size());
                statistics.put("localVariablesCount", rawAnalysis.getLocalVariables().size());
                statistics.put("globalVariablesCount", rawAnalysis.getGlobalVariables().size());
                statistics.put("loopVariablesCount", rawAnalysis.getLoopVariables().size());
                statistics.put("macrosCount", rawAnalysis.getMacros().size());
                statistics.put("functionsCount", rawAnalysis.getFunctions().size());
                statistics.put("macroCallsCount", rawAnalysis.getMacroCalls().size());
                statistics.put("includedTemplatesCount", rawAnalysis.getIncludedTemplates().size());
                statistics.put("importedTemplatesCount", rawAnalysis.getImportedTemplates().size());
                response.put("statistics", statistics);
            }

            // === 미리보기 (선택적) ===
            if (includePreview) {
                try {
                    TemplateRenderResponseDto previewResult = templateService.renderTemplatePreview(templateId);
                    Map<String, Object> preview = new HashMap<>();
                    preview.put("success", previewResult.isSuccess());
                    preview.put("renderedHtml", previewResult.getRenderedHtml());
                    preview.put("renderTimeMs", previewResult.getRenderTimeMs());
                    preview.put("previewErrors", previewResult.getErrors());
                    if (previewResult.getDebugInfo() != null) {
                        preview.put("debugInfo", previewResult.getDebugInfo());
                    }
                    response.put("preview", preview);
                } catch (Exception e) {
                    log.warn("Failed to generate preview for {}: {}", templateId, e.getMessage());
                    Map<String, Object> preview = new HashMap<>();
                    preview.put("success", false);
                    preview.put("previewError", e.getMessage());
                    response.put("preview", preview);
                }
            }

            // === 요약 (항상 포함) ===
            response.put("summary", rawAnalysis.getSummary());

            log.info("Template analysis completed for: {} ({} variables, {} errors, {}ms)",
                    templateId, requiredVariables.size(), rawAnalysis.getErrors().size(),
                    System.currentTimeMillis() - (Long) response.get("analysisTimestamp"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to analyze template: {}", templateId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("templateId", templateId);
            errorResponse.put("templateName", templateId);
            errorResponse.put("templateValid", false);
            errorResponse.put("error", "Template analysis failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            errorResponse.put("requiredExternalVariables", new HashSet<>());
            errorResponse.put("formVariables", new HashSet<>());
            errorResponse.put("assignedVariables", new HashSet<>());
            errorResponse.put("errors", List.of(e.getMessage()));

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 템플릿 렌더링 - HTML 출력
     */
    @PostMapping("/render")
    public ResponseEntity<?> renderTemplate(@RequestBody TemplateRenderRequestDto request) {
        try {
            log.info("Rendering template: {} with {} variables",
                    request.getTemplateName(),
                    request.getVariables() != null ? request.getVariables().size() : 0);

            TemplateRenderResponseDto result = templateService.renderTemplate(request);

            if (!result.isSuccess()) {
                log.warn("Template rendering failed: {}", result.getErrors());
            } else {
                log.info("Template rendering completed successfully: {} ({}ms)",
                        request.getTemplateName(), result.getRenderTimeMs());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to render template: {}",
                    request != null ? request.getTemplateName() : "unknown", e);

            TemplateRenderResponseDto errorResponse = TemplateRenderResponseDto.builder()
                    .templateName(request != null ? request.getTemplateName() : "unknown")
                    .renderedHtml("")
                    .success(false)
                    .errors(List.of("Render failed: " + e.getMessage()))
                    .renderTimeMs(0)
                    .build();

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/render-html")
    public ResponseEntity<?> renderTemplateInHtml(@RequestBody TemplateRenderRequestDto request) {
        try {
            TemplateRenderResponseDto result = templateService.renderTemplate(request);

            if (!result.isSuccess()) {
                String errorHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head><title>Template Error</title></head>
                        <body>
                            <h1>Template Render Error</h1>
                            <p>Template: %s</p>
                            <ul>%s</ul>
                        </body>
                        </html>
                        """.formatted(
                        request.getTemplateName(),
                        result.getErrors().stream()
                                .map(error -> "<li>" + error + "</li>")
                                .collect(Collectors.joining())
                );

                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(errorHtml);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(result.getRenderedHtml());

        } catch (Exception e) {
            log.error("Failed to generate HTML preview for template: {}", request.getTemplateName(), e);

            String errorHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head><title>Preview Error</title></head>
                    <body>
                        <h1>Preview Generation Failed</h1>
                        <p>Template: %s</p>
                        <p>Error: %s</p>
                    </body>
                    </html>
                    """.formatted(request.getTemplateName(), e.getMessage());

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(errorHtml);
        }
    }

    /**
     * 템플릿 미리보기 - 기본값으로 렌더링
     */
    @GetMapping("/preview-json/{templateId}")
    public ResponseEntity<?> previewTemplate(@PathVariable("templateId") String templateId) {
        try {
            log.info("Generating preview for template: {}", templateId);
            TemplateRenderResponseDto result = templateService.renderTemplatePreview(templateId);

            if (!result.isSuccess()) {
                log.warn("Template preview generation failed: {}", result.getErrors());
            } else {
                log.info("Template preview generated successfully: {} ({}ms)",
                        templateId, result.getRenderTimeMs());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to generate preview for template: {}", templateId, e);

            TemplateRenderResponseDto errorResponse = TemplateRenderResponseDto.builder()
                    .templateName(templateId)
                    .renderedHtml("")
                    .success(false)
                    .errors(List.of("Preview failed: " + e.getMessage()))
                    .renderTimeMs(0)
                    .build();

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 템플릿 미리보기 - HTML 직접 반환
     */
    @GetMapping("/preview-html/{templateId}")
    public ResponseEntity<String> previewTemplateHtml(@PathVariable("templateId") String templateId) {
        try {
            log.info("Generating HTML preview for template: {}", templateId);
            TemplateRenderResponseDto result = templateService.renderTemplatePreview(templateId);

            if (!result.isSuccess()) {
                String errorHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head><title>Template Error</title></head>
                        <body>
                            <h1>Template Render Error</h1>
                            <p>Template: %s</p>
                            <ul>%s</ul>
                        </body>
                        </html>
                        """.formatted(
                        templateId,
                        result.getErrors().stream()
                                .map(error -> "<li>" + error + "</li>")
                                .collect(Collectors.joining())
                );

                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(errorHtml);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(result.getRenderedHtml());

        } catch (Exception e) {
            log.error("Failed to generate HTML preview for template: {}", templateId, e);

            String errorHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head><title>Preview Error</title></head>
                    <body>
                        <h1>Preview Generation Failed</h1>
                        <p>Template: %s</p>
                        <p>Error: %s</p>
                    </body>
                    </html>
                    """.formatted(templateId, e.getMessage());

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(errorHtml);
        }
    }

    /**
     * 템플릿 변수 기본값 조회 - 렌더링 준비용
     */
    @GetMapping("/variables/{templateId}/defaults")
    public ResponseEntity<?> getTemplateDefaultVariables(@PathVariable("templateId") String templateId) {
        try {
            log.info("Getting default variables for template: {}", templateId);
            Map<String, Object> result = templateService.getRequiredVariablesWithDefaults(templateId);

            Map<String, Object> response = new HashMap<>();
            response.put("templateId", templateId);
            response.put("variables", result);
            response.put("count", result.size());

            log.info("Default variables retrieved for template: {} ({} variables)",
                    templateId, result.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get default variables for template: {}", templateId, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get default variables");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("templateId", templateId);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 템플릿 유효성 검증
     */
    @GetMapping("/validate/{templateId}")
    public ResponseEntity<?> validateTemplate(@PathVariable("templateId") String templateId) {
        try {
            log.info("Validating template: {}", templateId);
            boolean isValid = templateService.validateTemplate(templateId);

            Map<String, Object> response = new HashMap<>();
            response.put("templateId", templateId);
            response.put("valid", isValid);
            response.put("message", isValid ? "Template is valid" : "Template is invalid");

            log.info("Template validation completed: {} - {}", templateId, isValid ? "VALID" : "INVALID");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to validate template: {}", templateId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("templateId", templateId);
            errorResponse.put("valid", false);
            errorResponse.put("error", "Validation failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 템플릿 요약 정보 조회 - 간단한 메타데이터
     */
    @GetMapping("/summary/{templateId}")
    public ResponseEntity<?> getTemplateSummary(@PathVariable("templateId") String templateId) {
        try {
            log.info("Getting summary for template: {}", templateId);
            TemplateAnalysisResponseDto analysis = templateService.analyzeTemplate(templateId);

            Map<String, Object> summary = getSummary(templateId, analysis);

            log.info("Template summary retrieved: {}", templateId);
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Failed to get template summary: {}", templateId, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get template summary");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("templateId", templateId);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }


    /**
     * 템플릿 변수 계층 구조 조회 - 개선된 버전
     */
    @GetMapping("/variables/{templateId}")
    public ResponseEntity<?> getTemplateVariablesHierarchical(@PathVariable("templateId") String templateId) {
        try {
            log.info("Getting hierarchical variable structure for template: {}", templateId);

            // 변경된 부분: analyzeTemplateRaw 사용
            FreeMarkerVariableExtractor.TemplateVariableAnalysis analysis = templateService.analyzeTemplateRaw(templateId);

            Map<String, Object> response = new HashMap<>();
            response.put("templateId", templateId);
            response.put("templateValid", analysis.isTemplateValid());
            response.put("hierarchicalVariables", analysis.getHierarchicalVariables());
            response.put("variablesJson", analysis.getHierarchicalVariablesJson());
            response.put("assignedVariables", analysis.getAssignedVariables());
            response.put("loopVariables", analysis.getLoopVariables());
            response.put("errors", analysis.getErrors());

            log.info("Hierarchical variables retrieved for template: {} ({} root variables)",
                    templateId, analysis.getHierarchicalVariables().size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get hierarchical variables for template: {}", templateId, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get hierarchical variables");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("templateId", templateId);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 템플릿 미리보기 - 지능적 기본값 사용
     */
    @GetMapping("/preview-smart/{templateId}")
    public ResponseEntity<String> previewTemplateWithSmartDefaults(@PathVariable("templateId") String templateId) {
        try {
            log.info("Generating smart preview for template: {}", templateId);
            TemplateRenderResponseDto result = templateService.renderTemplatePreview(templateId);

            if (!result.isSuccess()) {
                String errorHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head><title>Template Error</title></head>
                        <body>
                            <h1>Template Render Error</h1>
                            <p>Template: %s</p>
                            <ul>%s</ul>
                        </body>
                        </html>
                        """.formatted(
                        templateId,
                        result.getErrors().stream()
                                .map(error -> "<li>" + error + "</li>")
                                .collect(Collectors.joining())
                );

                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(errorHtml);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(result.getRenderedHtml());

        } catch (Exception e) {
            log.error("Failed to generate smart preview for template: {}", templateId, e);

            String errorHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head><title>Preview Error</title></head>
                    <body>
                        <h1>Smart Preview Generation Failed</h1>
                        <p>Template: %s</p>
                        <p>Error: %s</p>
                    </body>
                    </html>
                    """.formatted(templateId, e.getMessage());

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(errorHtml);
        }
    }


    // --- 유틸 메서드 ---------------------------------------------------------------------------------

    private static Map<String, Object> getSummary(String templateId, TemplateAnalysisResponseDto analysis) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("templateId", templateId);
        summary.put("valid", analysis.isTemplateValid());
        summary.put("requiredVariablesCount", analysis.getRequiredExternalVariables().size());
        summary.put("formVariablesCount", analysis.getFormVariables().size());
        summary.put("macrosCount", analysis.getMacros().size());
        summary.put("functionsCount", analysis.getFunctions().size());
        summary.put("hasErrors", !analysis.getErrors().isEmpty());
        summary.put("errorCount", analysis.getErrors().size());
        return summary;
    }


    private boolean isFormRelatedVariable(String varName) {
        return examineIfFormRelated(varName);
    }

    private String generateUsageDescription(Set<FreeMarkerVariableExtractor.ExpressionType> usageTypes) {
        if (usageTypes == null || usageTypes.isEmpty()) {
            return "Variable used in template";
        }

        List<String> descriptions = usageTypes.stream()
                .map(this::getUsageDescription)
                .collect(Collectors.toList());

        return "Used for: " + String.join(", ", descriptions);
    }

    private String getUsageDescription(FreeMarkerVariableExtractor.ExpressionType type) {
        return getType(type);
    }

    public static String getType(FreeMarkerVariableExtractor.ExpressionType type) {
        return switch (type) {
            case OUTPUT -> "output display";
            case ASSIGNMENT -> "variable assignment";
            case CONDITION -> "conditional logic";
            case ITERATION -> "loop iteration";
            case PARAMETER -> "parameter passing";
            case INTERPOLATION -> "string interpolation";
            case STRING_INTERPOLATION -> "string template";
            default -> "general usage";
        };
    }

    public static boolean examineIfFormRelated(String varName) {
        String lowerName = varName.toLowerCase();
        return lowerName.contains("form") ||
                lowerName.contains("field") ||
                lowerName.contains("input") ||
                lowerName.contains("value") ||
                lowerName.contains("data") ||
                lowerName.contains("selected") ||
                lowerName.contains("checked") ||
                lowerName.contains("option") ||
                lowerName.contains("submit") ||
                lowerName.contains("valid");
    }
}
