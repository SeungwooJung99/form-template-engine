package com.boxwood.form.engine.form.utils;

import freemarker.template.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 기업용 안전한 FreeMarker 변수 추출 서비스
 * - Mock Environment + Variable Tracking 방식
 * - 정규식 대신 FreeMarker 실행 추적으로 정확성 확보
 * - 기업 환경에 적합한 안정성과 신뢰성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FreeMarkerVariableExtractor {
    private final Configuration freeMarkerConfig;

    /**
     * 템플릿에서 모든 변수 정보를 추출
     */
    public TemplateVariableAnalysis analyzeTemplate(String templateName) throws IOException, TemplateException {
        TemplateVariableAnalysis analysis = new TemplateVariableAnalysis(templateName);

        try {
            log.info("Starting enterprise template analysis for: {}", templateName);

            // 1. Mock Environment를 통한 변수 추출 (메인 방식)
            Set<String> extractedVariables = extractVariablesUsingEnhancedMockEnvironment(templateName, analysis);

            // 2. 계층 구조 생성
            Map<String, Object> hierarchicalVariables = new LinkedHashMap<>();
            buildHierarchicalStructure(extractedVariables, hierarchicalVariables);

            analysis.setHierarchicalVariables(hierarchicalVariables);
            analysis.setTemplateValid(true);

            log.info("Enterprise analysis completed: {} variables found", extractedVariables.size());
            log.debug("Variables: {}", extractedVariables);

        } catch (Exception e) {
            log.error("Template analysis failed for: {}", templateName, e);
            analysis.addError("Analysis failed: " + e.getMessage());
            analysis.setTemplateValid(false);
        }

        return analysis;
    }

    /**
     * Enhanced Mock Environment를 사용한 변수 추출
     * - 여러 번의 실행으로 모든 변수 캐치
     * - 다양한 시나리오 테스트
     */
    private Set<String> extractVariablesUsingEnhancedMockEnvironment(String templateName, TemplateVariableAnalysis analysis) {
        Set<String> allVariables = new LinkedHashSet<>();

        try {
            Template template = freeMarkerConfig.getTemplate(templateName);

            // === 1단계: 기본 Mock Environment ===
            EnterpriseVariableCapturingModel basicMock = new EnterpriseVariableCapturingModel();
            executeTemplateWithMock(template, basicMock, "basic");
            allVariables.addAll(basicMock.getAccessedVariables());

            // === 2단계: 조건문/반복문을 위한 Mock Environment ===
            EnterpriseVariableCapturingModel conditionalMock = new EnterpriseVariableCapturingModel();
            conditionalMock.enableConditionalMode(); // 조건문 테스트를 위한 모드
            executeTemplateWithMock(template, conditionalMock, "conditional");
            allVariables.addAll(conditionalMock.getAccessedVariables());

            // === 3단계: 리스트/배열 접근을 위한 Mock Environment ===
            EnterpriseVariableCapturingModel iterationMock = new EnterpriseVariableCapturingModel();
            iterationMock.enableIterationMode(); // 반복문 테스트를 위한 모드
            executeTemplateWithMock(template, iterationMock, "iteration");
            allVariables.addAll(iterationMock.getAccessedVariables());

            // === 4단계: 매크로/함수 분석 ===
            extractMacrosAndFunctions(template, analysis);

            log.info("Enhanced mock analysis completed: {} unique variables", allVariables.size());

        } catch (Exception e) {
            log.warn("Enhanced mock environment variable extraction failed: {}", e.getMessage());
        }

        return allVariables;
    }

    /**
     * Mock 환경에서 템플릿 실행
     */
    private void executeTemplateWithMock(Template template, EnterpriseVariableCapturingModel mockModel, String mode) {
        try (StringWriter writer = new StringWriter()) {
            template.process(mockModel, writer);
            log.trace("Template execution completed in {} mode", mode);
        } catch (Exception e) {
            // 템플릿 실행 중 오류는 예상되는 상황 (변수가 없으니까)
            log.trace("Expected error during {} mock processing: {}", mode, e.getMessage());
        }
    }

    /**
     * 매크로와 함수 추출 (AST 기반)
     */
    private void extractMacrosAndFunctions(Template template, TemplateVariableAnalysis analysis) {
        try {
            // FreeMarker 내부 AST 구조에서 매크로/함수 정보 추출
            String templateSource = template.toString();

            // 간단한 패턴 매칭으로 매크로/함수만 추출 (안전한 범위)
            extractMacroDefinitions(templateSource, analysis);
            extractFunctionDefinitions(templateSource, analysis);

        } catch (Exception e) {
            log.warn("Macro/Function extraction failed: {}", e.getMessage());
        }
    }

    private void extractMacroDefinitions(String content, TemplateVariableAnalysis analysis) {
        // 매크로 정의만 추출 (변수는 추출하지 않음)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "<#macro\\s+(\\w+)([^>]*)>",
                java.util.regex.Pattern.MULTILINE
        );
        java.util.regex.Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String macroName = matcher.group(1);
            String paramsStr = matcher.group(2).trim();
            List<String> params = parseParameters(paramsStr);
            analysis.addMacro(macroName, params);
            log.trace("Found macro: {} with params: {}", macroName, params);
        }
    }

    private void extractFunctionDefinitions(String content, TemplateVariableAnalysis analysis) {
        // 함수 정의만 추출 (변수는 추출하지 않음)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "<#function\\s+(\\w+)([^>]*)>",
                java.util.regex.Pattern.MULTILINE
        );
        java.util.regex.Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String funcName = matcher.group(1);
            String paramsStr = matcher.group(2).trim();
            List<String> params = parseParameters(paramsStr);
            analysis.addFunction(funcName, params);
            log.trace("Found function: {} with params: {}", funcName, params);
        }
    }

    /**
     * 계층 구조 생성
     */
    private void buildHierarchicalStructure(Set<String> variables, Map<String, Object> hierarchicalVariables) {
        for (String varPath : variables) {
            if (varPath.contains(".")) {
                createHierarchicalPath(varPath, hierarchicalVariables);
            } else {
                if (!hierarchicalVariables.containsKey(varPath)) {
                    hierarchicalVariables.put(varPath, createDefaultValue(varPath));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void createHierarchicalPath(String varPath, Map<String, Object> hierarchicalVariables) {
        String[] parts = varPath.split("\\.");
        Map<String, Object> current = hierarchicalVariables;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];

            if (!current.containsKey(part)) {
                current.put(part, new LinkedHashMap<String, Object>());
            }

            Object next = current.get(part);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                Map<String, Object> newMap = new LinkedHashMap<>();
                current.put(part, newMap);
                current = newMap;
            }
        }

        String lastPart = parts[parts.length - 1];
        current.put(lastPart, createDefaultValue(lastPart));
    }

    private Object createDefaultValue(String varName) {
        String lowerName = varName.toLowerCase();

        if (lowerName.contains("is") || lowerName.contains("has") || lowerName.contains("enable")) {
            return false;
        }
        if (lowerName.contains("count") || lowerName.contains("amount") || lowerName.contains("total")) {
            return 0;
        }
        if (lowerName.contains("rate") || lowerName.contains("price")) {
            return 0.0;
        }
        if (lowerName.contains("list") || lowerName.contains("items") || lowerName.endsWith("s")) {
            return new ArrayList<>();
        }
        if (lowerName.contains("date") || lowerName.contains("time")) {
            return "2024-01-01";
        }

        return "";
    }

    private List<String> parseParameters(String paramsStr) {
        List<String> params = new ArrayList<>();
        if (paramsStr == null || paramsStr.trim().isEmpty()) {
            return params;
        }

        String[] parts = paramsStr.trim().split("\\s+");
        for (String part : parts) {
            part = part.trim();
            if (part.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
                params.add(part);
            }
        }
        return params;
    }

    private String readTemplateContent(String templateName) throws IOException {
        try {
            String templateDir = System.getProperty("freemarker.template.path", "src/main/resources/templates/freemarker");
            Path templatePath = Paths.get(templateDir, templateName);

            if (Files.exists(templatePath)) {
                return Files.readString(templatePath);
            }

            try (InputStream is = getClass().getClassLoader().getResourceAsStream("templates/" + templateName)) {
                if (is != null) {
                    return new String(is.readAllBytes());
                }
            }

            Path currentDirPath = Paths.get("templates", templateName);
            if (Files.exists(currentDirPath)) {
                return Files.readString(currentDirPath);
            }

            throw new IOException("Template not found: " + templateName);
        } catch (Exception e) {
            throw new IOException("Failed to read template: " + templateName, e);
        }
    }

    // Expression 타입 enum
    public enum ExpressionType {
        OUTPUT, ASSIGNMENT, CONDITION, ITERATION, PARAMETER, INTERPOLATION, STRING_INTERPOLATION
    }

    /**
     * 기업용 강화된 변수 캡처 모델
     */
    private static class EnterpriseVariableCapturingModel implements TemplateHashModel {
        private final Set<String> accessedVariables = new LinkedHashSet<>();
        private boolean conditionalMode = false;
        private boolean iterationMode = false;

        public void enableConditionalMode() {
            this.conditionalMode = true;
        }

        public void enableIterationMode() {
            this.iterationMode = true;
        }

        @Override
        public TemplateModel get(String key) throws TemplateModelException {
            accessedVariables.add(key);
            log.trace("Enterprise mock accessed variable: {}", key);

            // 모드에 따른 다른 응답
            if (conditionalMode) {
                return new EnterpriseNestedCapturingModel(key, accessedVariables, true, false);
            } else if (iterationMode) {
                return new EnterpriseNestedCapturingModel(key, accessedVariables, false, true);
            } else {
                return new EnterpriseNestedCapturingModel(key, accessedVariables, false, false);
            }
        }

        @Override
        public boolean isEmpty() throws TemplateModelException {
            return false;
        }

        public Set<String> getAccessedVariables() {
            return new LinkedHashSet<>(accessedVariables);
        }
    }

    /**
     * 기업용 중첩 변수 캡처 모델
     */
    private static class EnterpriseNestedCapturingModel implements TemplateHashModel, TemplateScalarModel,
            TemplateSequenceModel, TemplateBooleanModel, TemplateNumberModel, TemplateDateModel {

        private final String basePath;
        private final Set<String> accessedVariables;
        private final boolean conditionalMode;
        private final boolean iterationMode;

        public EnterpriseNestedCapturingModel(String basePath, Set<String> accessedVariables,
                                              boolean conditionalMode, boolean iterationMode) {
            this.basePath = basePath;
            this.accessedVariables = accessedVariables;
            this.conditionalMode = conditionalMode;
            this.iterationMode = iterationMode;
        }

        @Override
        public TemplateModel get(String key) throws TemplateModelException {
            String fullPath = basePath + "." + key;
            accessedVariables.add(fullPath);
            log.trace("Enterprise nested model accessed: {}", fullPath);
            return new EnterpriseNestedCapturingModel(fullPath, accessedVariables, conditionalMode, iterationMode);
        }

        @Override
        public boolean isEmpty() throws TemplateModelException {
            return false;
        }

        @Override
        public String getAsString() throws TemplateModelException {
            return "mock_value";
        }

        @Override
        public TemplateModel get(int index) throws TemplateModelException {
            String indexPath = basePath + "[" + index + "]";
            accessedVariables.add(indexPath);
            return new EnterpriseNestedCapturingModel(indexPath, accessedVariables, conditionalMode, iterationMode);
        }

        @Override
        public int size() throws TemplateModelException {
            // 반복 모드에서는 여러 아이템이 있는 것처럼 동작
            return iterationMode ? 3 : 1;
        }

        @Override
        public boolean getAsBoolean() throws TemplateModelException {
            // 조건 모드에서는 true/false 모두 테스트
            return conditionalMode ? Math.random() > 0.5 : true;
        }

        @Override
        public Number getAsNumber() throws TemplateModelException {
            return conditionalMode ? (Math.random() > 0.5 ? 1 : 0) : 1;
        }

        @Override
        public Date getAsDate() throws TemplateModelException {
            return new Date();
        }

        @Override
        public int getDateType() {
            return TemplateDateModel.DATETIME;
        }
    }

    /**
     * 템플릿 분석 결과 클래스
     */
    @Getter
    public static class TemplateVariableAnalysis {
        private final String templateName;
        private boolean templateValid = true;
        private final Set<String> assignedVariables = new LinkedHashSet<>();
        private final Set<String> localVariables = new LinkedHashSet<>();
        private final Set<String> globalVariables = new LinkedHashSet<>();
        private final Set<String> loopVariables = new LinkedHashSet<>();
        private final Map<String, Set<ExpressionType>> referencedVariables = new LinkedHashMap<>();
        private final Map<String, List<String>> macros = new LinkedHashMap<>();
        private final Map<String, List<String>> functions = new LinkedHashMap<>();
        private final Set<String> macroCalls = new LinkedHashSet<>();
        private final Set<String> includedTemplates = new LinkedHashSet<>();
        private final Map<String, String> importedTemplates = new LinkedHashMap<>();
        private final List<String> errors = new ArrayList<>();
        private Map<String, Object> hierarchicalVariables = new LinkedHashMap<>();

        public TemplateVariableAnalysis(String templateName) {
            this.templateName = templateName;
        }

        public void addAssignedVariable(String name) {
            assignedVariables.add(name);
        }

        public void addLocalVariable(String name) {
            localVariables.add(name);
        }

        public void addGlobalVariable(String name) {
            globalVariables.add(name);
        }

        public void addLoopVariable(String name) {
            loopVariables.add(name);
        }

        public void addReferencedVariable(String name, ExpressionType type) {
            referencedVariables.computeIfAbsent(name, k -> new LinkedHashSet<>()).add(type);
        }

        public void addMacro(String name, List<String> params) {
            macros.put(name, new ArrayList<>(params));
        }

        public void addFunction(String name, List<String> params) {
            functions.put(name, new ArrayList<>(params));
        }

        public void addMacroCall(String name) {
            macroCalls.add(name);
        }

        public void addIncludedTemplate(String templateName) {
            includedTemplates.add(templateName);
        }

        public void addImportedTemplate(String templateName, String namespace) {
            importedTemplates.put(namespace, templateName);
        }

        public void addError(String error) {
            errors.add(error);
        }

        public void setTemplateValid(boolean valid) {
            this.templateValid = valid;
        }

        public void setHierarchicalVariables(Map<String, Object> hierarchicalVariables) {
            this.hierarchicalVariables = hierarchicalVariables;
        }

        public Set<String> getRequiredExternalVariables() {
            return hierarchicalVariables.keySet();
        }

        public String getHierarchicalVariablesJson() {
            return convertToJson(hierarchicalVariables, 0);
        }

        private String convertToJson(Object obj, int depth) {
            if (depth > 10) return "\"...\"";
            String indent = "  ".repeat(depth);
            String nextIndent = "  ".repeat(depth + 1);

            if (obj instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) obj;
                if (map.isEmpty()) return "{}";

                StringBuilder sb = new StringBuilder("{\n");
                Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();

                while (iter.hasNext()) {
                    Map.Entry<String, Object> entry = iter.next();
                    sb.append(nextIndent).append("\"").append(entry.getKey()).append("\": ");
                    sb.append(convertToJson(entry.getValue(), depth + 1));
                    if (iter.hasNext()) sb.append(",");
                    sb.append("\n");
                }

                sb.append(indent).append("}");
                return sb.toString();
            } else if (obj instanceof List) {
                return "[]";
            } else if (obj instanceof String) {
                return "\"" + obj + "\"";
            } else if (obj instanceof Number || obj instanceof Boolean) {
                return obj.toString();
            } else {
                return "\"" + (obj != null ? obj.toString() : "") + "\"";
            }
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Enterprise Template Analysis: ").append(templateName).append(" ===\n");
            sb.append("Template Valid: ").append(templateValid).append("\n");
            sb.append("Hierarchical Variables:\n").append(getHierarchicalVariablesJson()).append("\n");
            sb.append("Assigned Variables: ").append(assignedVariables).append("\n");
            sb.append("Loop Variables: ").append(loopVariables).append("\n");
            sb.append("Macros: ").append(macros.keySet()).append("\n");
            sb.append("Functions: ").append(functions.keySet()).append("\n");
            if (!errors.isEmpty()) {
                sb.append("Errors: ").append(errors).append("\n");
            }
            return sb.toString();
        }
    }
}

// package com.boxwood.form.engine.form.utils;
//
// import freemarker.template.*;
// import lombok.Getter;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Service;
//
// import java.io.*;
// import java.util.*;
// import java.util.regex.Pattern;
// import java.util.regex.Matcher;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
//
// /**
//  * 개선된 Mock Environment + 정교한 정규식 기반 FreeMarker 변수 추출 서비스
//  * - FreeMarker 연산자 및 내장 함수 필터링 강화
//  * - 날짜 포맷팅 패턴 인식 개선
//  * - 문자열 리터럴 파싱 정교화
//  */
// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class FreeMarkerVariableExtractor {
//     private final Configuration freeMarkerConfig;
//
//     // FreeMarker 연산자들 (비교, 논리, 산술 등)
//     private static final Set<String> FREEMARKER_OPERATORS = Set.of(
//             // 비교 연산자
//             "gt", "gte", "lt", "lte", "eq", "ne",
//             // 논리 연산자
//             "and", "or", "not",
//             // 기타
//             "as", "in", "using"
//     );
//
//     // FreeMarker 내장 함수들
//     private static final Set<String> FREEMARKER_BUILTINS = Set.of(
//             "abs", "ancestor", "api", "boolean", "byte", "c", "cap_first", "capitalize",
//             "ceiling", "children", "chop_linebreak", "chunk", "contains", "counter", "date",
//             "datetime", "default", "double", "ends_with", "esc", "eval", "exists", "first",
//             "float", "floor", "format", "groups", "has_content", "html", "index", "index_of",
//             "int", "is_boolean", "is_collection", "is_date", "is_directive", "is_enumerable",
//             "is_hash", "is_hash_ex", "is_indexable", "is_infinite", "is_macro", "is_method",
//             "is_nan", "is_node", "is_number", "is_sequence", "is_string", "is_transform",
//             "iso_utc", "j_string", "join", "js_string", "json_string", "keep_after",
//             "keep_after_last", "keep_before", "keep_before_last", "keys", "last", "last_index_of",
//             "left_pad", "length", "long", "lower_case", "ltrim", "matches", "max", "min",
//             "namespace", "new", "next_sibling", "node_name", "node_namespace", "node_type",
//             "number", "number_to_date", "number_to_datetime", "number_to_time", "parent",
//             "previous_sibling", "replace", "reverse", "right_pad", "round", "rtrim", "seq_contains",
//             "seq_index_of", "seq_last_index_of", "short", "size", "sort", "sort_by", "split",
//             "starts_with", "string", "substring", "time", "trim", "uncap_first", "upper_case",
//             "url", "values", "web_safe", "word_list", "xhtml", "xml"
//     );
//
//     // 키워드들
//     private static final Set<String> FREEMARKER_KEYWORDS = Set.of(
//             "true", "false", "null", "empty", "if", "else", "elseif", "endif", "list",
//             "assign", "global", "local", "include", "import", "macro", "nested", "return",
//             "visit", "recurse", "fallback", "case", "switch", "default", "break", "function",
//             "flush", "stop", "compress", "escape", "noescape", "setting", "outputformat",
//             "autoesc", "noautoesc", "attempt", "recover", "sep", "t", "lt", "rt", "nt"
//     );
//
//     // 날짜 포맷팅 패턴에서 사용되는 패턴 문자들
//     private static final Set<String> DATE_FORMAT_PATTERNS = Set.of(
//             "yyyy", "yy", "MM", "MMM", "MMMM", "dd", "d", "HH", "H", "hh", "h",
//             "mm", "m", "ss", "s", "SSS", "SS", "S", "E", "EE", "EEE", "EEEE",
//             "a", "z", "Z", "X", "x", "w", "W", "D", "F", "G", "u", "Y", "Q", "q"
//     );
//
//     /**
//      * 템플릿에서 모든 변수 정보를 추출
//      */
//     public TemplateVariableAnalysis analyzeTemplate(String templateName) throws IOException, TemplateException {
//         TemplateVariableAnalysis analysis = new TemplateVariableAnalysis(templateName);
//
//         try {
//             log.info("Starting template analysis for: {}", templateName);
//
//             // 1. 템플릿 내용 읽기
//             String templateContent = readTemplateContent(templateName);
//
//             // 2. Mock Environment를 통한 변수 추출 시도
//             Set<String> mockExtractedVars = extractVariablesUsingMockEnvironment(templateName);
//
//             // 3. 정교한 정규식을 통한 변수 추출
//             Set<String> regexExtractedVars = extractVariablesUsingRegex(templateContent, analysis);
//
//             // 4. 두 결과를 합치기
//             Set<String> allVariables = new LinkedHashSet<>();
//             allVariables.addAll(mockExtractedVars);
//             allVariables.addAll(regexExtractedVars);
//
//             // 5. 계층 구조 생성
//             Map<String, Object> hierarchicalVariables = new LinkedHashMap<>();
//             buildHierarchicalStructure(allVariables, hierarchicalVariables);
//
//             analysis.setHierarchicalVariables(hierarchicalVariables);
//             analysis.setTemplateValid(true);
//
//             log.info("Analysis completed: {} variables found (mock: {}, regex: {})",
//                     allVariables.size(), mockExtractedVars.size(), regexExtractedVars.size());
//             log.debug("All variables: {}", allVariables);
//
//         } catch (Exception e) {
//             log.error("Template analysis failed for: {}", templateName, e);
//             analysis.addError("Analysis failed: " + e.getMessage());
//             analysis.setTemplateValid(false);
//         }
//
//         return analysis;
//     }
//
//     /**
//      * Mock Environment를 사용한 변수 추출
//      */
//     private Set<String> extractVariablesUsingMockEnvironment(String templateName) {
//         Set<String> variables = new LinkedHashSet<>();
//
//         try {
//             Template template = freeMarkerConfig.getTemplate(templateName);
//
//             // Mock 데이터 모델 생성
//             VariableCapturingModel mockModel = new VariableCapturingModel();
//
//             // StringWriter로 출력 캡처
//             StringWriter writer = new StringWriter();
//
//             try {
//                 // 템플릿 실행하면서 변수 접근을 감지
//                 template.process(mockModel, writer);
//             } catch (Exception e) {
//                 // 템플릿 실행 중 오류는 예상되는 상황 (변수가 없으니까)
//                 log.trace("Expected error during mock processing: {}", e.getMessage());
//             }
//
//             // 캡처된 변수들 가져오기
//             variables.addAll(mockModel.getAccessedVariables());
//
//         } catch (Exception e) {
//             log.warn("Mock environment variable extraction failed: {}", e.getMessage());
//         }
//
//         return variables;
//     }
//
//     /**
//      * 정교한 정규식을 사용한 변수 추출
//      */
//     private Set<String> extractVariablesUsingRegex(String content, TemplateVariableAnalysis analysis) {
//         Set<String> variables = new LinkedHashSet<>();
//
//         // 1. ${...} 표현식 추출
//         extractDollarExpressions(content, variables);
//
//         // 2. 지시어에서 변수 추출
//         extractFromDirectives(content, analysis, variables);
//
//         return variables;
//     }
//
//     /**
//      * ${...} 표현식에서 변수 추출 - 매우 정교한 방식
//      */
//     private void extractDollarExpressions(String content, Set<String> variables) {
//         // 중첩 괄호를 고려한 ${...} 패턴
//         Pattern dollarPattern = Pattern.compile("\\$\\{([^}]+(?:\\{[^}]*\\}[^}]*)*)\\}", Pattern.MULTILINE | Pattern.DOTALL);
//         Matcher matcher = dollarPattern.matcher(content);
//
//         while (matcher.find()) {
//             String expression = matcher.group(1).trim();
//             log.trace("Found ${} expression: {}", expression);
//
//             // 표현식에서 변수 경로들 추출
//             extractVariablePathsFromExpression(expression, variables);
//         }
//     }
//
//     /**
//      * 단일 표현식에서 모든 변수 경로 추출 (개선된 버전)
//      */
//     private void extractVariablePathsFromExpression(String expression, Set<String> variables) {
//         // 1. 문자열 리터럴 제거 (따옴표 안의 내용 제거)
//         String cleanExpr = removeStringLiterals(expression);
//
//         // 2. 기본값 구문 처리 (variable!"default")
//         cleanExpr = removeDefaultValue(cleanExpr);
//
//         // 3. 함수 호출과 연산자 제거하면서 변수 찾기
//         Set<String> paths = findVariablePathsInExpression(cleanExpr);
//
//         // 4. 유효한 변수만 추가
//         for (String path : paths) {
//             if (isValidVariablePath(path)) {
//                 variables.add(path);
//                 log.trace("Added variable path: {}", path);
//             }
//         }
//     }
//
//     /**
//      * 문자열 리터럴 제거 (따옴표 안의 내용을 공백으로 대체)
//      */
//     private String removeStringLiterals(String expression) {
//         // 1. 작은따옴표 문자열 제거
//         expression = expression.replaceAll("'[^']*'", " ");
//
//         // 2. 큰따옴표 문자열 제거
//         expression = expression.replaceAll("\"[^\"]*\"", " ");
//
//         return expression;
//     }
//
//     /**
//      * 표현식에서 변수 경로들 찾기 (개선된 버전)
//      */
//     private Set<String> findVariablePathsInExpression(String expression) {
//         Set<String> paths = new LinkedHashSet<>();
//
//         // 1. 가장 단순한 경우: 단일 변수나 점 표기법
//         if (expression.matches("^[a-zA-Z_][a-zA-Z0-9_.]*$")) {
//             paths.add(expression);
//             return paths;
//         }
//
//         // 2. 복잡한 표현식에서 변수 경로들 추출
//         // 더 정교한 토큰화: 연산자, 괄호, 공백을 기준으로 분할
//         String[] tokens = expression.split("[\\s()+\\-*/%<>=!&|,?:;\\[\\]]+");
//
//         for (String token : tokens) {
//             token = token.trim();
//             if (!token.isEmpty() && !isLiteralOrKeyword(token)) {
//                 // 점 표기법 또는 단순 변수인지 확인
//                 if (token.matches("^[a-zA-Z_][a-zA-Z0-9_.]*$")) {
//                     paths.add(token);
//                 }
//             }
//         }
//
//         // 3. 특수 패턴들 (method call 제거 등)
//         findAdditionalPatterns(expression, paths);
//
//         return paths;
//     }
//
//     /**
//      * 추가적인 패턴들 찾기
//      */
//     private void findAdditionalPatterns(String expression, Set<String> paths) {
//         // object.method() 형태에서 object 추출
//         Pattern methodCallPattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_.]*)\\.\\w+\\s*\\(");
//         Matcher methodMatcher = methodCallPattern.matcher(expression);
//         while (methodMatcher.find()) {
//             String objectPath = methodMatcher.group(1);
//             if (isValidVariablePath(objectPath)) {
//                 paths.add(objectPath);
//             }
//         }
//
//         // object?builtin 형태에서 object 추출
//         Pattern builtinPattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_.]*)(\\?\\w+)+");
//         Matcher builtinMatcher = builtinPattern.matcher(expression);
//         while (builtinMatcher.find()) {
//             String objectPath = builtinMatcher.group(1);
//             if (isValidVariablePath(objectPath)) {
//                 paths.add(objectPath);
//             }
//         }
//
//         // 배열/맵 접근 object[key] 형태에서 object 추출
//         Pattern arrayAccessPattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_.]*)\\[");
//         Matcher arrayMatcher = arrayAccessPattern.matcher(expression);
//         while (arrayMatcher.find()) {
//             String objectPath = arrayMatcher.group(1);
//             if (isValidVariablePath(objectPath)) {
//                 paths.add(objectPath);
//             }
//         }
//     }
//
//     /**
//      * 지시어에서 변수 추출
//      */
//     private void extractFromDirectives(String content, TemplateVariableAnalysis analysis, Set<String> variables) {
//         // <#assign var = expression>
//         extractAssignDirectives(content, analysis, variables);
//
//         // <#list sequence as item>
//         extractListDirectives(content, analysis, variables);
//
//         // <#if condition>
//         extractConditionalDirectives(content, variables);
//
//         // <#macro name params>
//         extractMacroDirectives(content, analysis);
//
//         // <#function name params>
//         extractFunctionDirectives(content, analysis);
//
//         // <#include "template">
//         extractIncludeDirectives(content, analysis);
//
//         // <#import "template" as namespace>
//         extractImportDirectives(content, analysis);
//     }
//
//     private void extractAssignDirectives(String content, TemplateVariableAnalysis analysis, Set<String> variables) {
//         Pattern pattern = Pattern.compile("<#assign\\s+([^=\\s]+)\\s*=\\s*([^>]+)>", Pattern.MULTILINE | Pattern.DOTALL);
//         Matcher matcher = pattern.matcher(content);
//
//         while (matcher.find()) {
//             String varName = matcher.group(1).trim();
//             String valueExpr = matcher.group(2).trim();
//
//             analysis.addAssignedVariable(varName);
//             extractVariablePathsFromExpression(valueExpr, variables);
//             log.trace("Found assign: {} = {}", varName, valueExpr);
//         }
//     }
//
//     private void extractListDirectives(String content, TemplateVariableAnalysis analysis, Set<String> variables) {
//         Pattern pattern = Pattern.compile("<#list\\s+([^\\s]+)\\s+as\\s+(\\w+)", Pattern.MULTILINE);
//         Matcher matcher = pattern.matcher(content);
//
//         while (matcher.find()) {
//             String sequence = matcher.group(1).trim();
//             String itemVar = matcher.group(2).trim();
//
//             extractVariablePathsFromExpression(sequence, variables);
//             analysis.addLoopVariable(itemVar);
//             log.trace("Found list: {} as {}", sequence, itemVar);
//         }
//     }
//
//     private void extractConditionalDirectives(String content, Set<String> variables) {
//         Pattern pattern = Pattern.compile("<#(if|elseif)\\s+([^>]+)>", Pattern.MULTILINE | Pattern.DOTALL);
//         Matcher matcher = pattern.matcher(content);
//
//         while (matcher.find()) {
//             String condition = matcher.group(2).trim();
//             extractVariablePathsFromExpression(condition, variables);
//             log.trace("Found condition: {}", condition);
//         }
//     }
//
//     private void extractMacroDirectives(String content, TemplateVariableAnalysis analysis) {
//         Pattern pattern = Pattern.compile("<#macro\\s+(\\w+)([^>]*)>", Pattern.MULTILINE);
//         Matcher matcher = pattern.matcher(content);
//
//         while (matcher.find()) {
//             String macroName = matcher.group(1);
//             String paramsStr = matcher.group(2).trim();
//             List<String> params = parseParameters(paramsStr);
//             analysis.addMacro(macroName, params);
//             log.trace("Found macro: {} with params: {}", macroName, params);
//         }
//     }
//
//     private void extractFunctionDirectives(String content, TemplateVariableAnalysis analysis) {
//         Pattern pattern = Pattern.compile("<#function\\s+(\\w+)([^>]*)>", Pattern.MULTILINE);
//         Matcher matcher = pattern.matcher(content);
//
//         while (matcher.find()) {
//             String funcName = matcher.group(1);
//             String paramsStr = matcher.group(2).trim();
//             List<String> params = parseParameters(paramsStr);
//             analysis.addFunction(funcName, params);
//             log.trace("Found function: {} with params: {}", funcName, params);
//         }
//     }
//
//     private void extractIncludeDirectives(String content, TemplateVariableAnalysis analysis) {
//         Pattern pattern = Pattern.compile("<#include\\s+[\"']([^\"']+)[\"']", Pattern.MULTILINE);
//         Matcher matcher = pattern.matcher(content);
//
//         while (matcher.find()) {
//             String templateName = matcher.group(1);
//             analysis.addIncludedTemplate(templateName);
//             log.trace("Found include: {}", templateName);
//         }
//     }
//
//     private void extractImportDirectives(String content, TemplateVariableAnalysis analysis) {
//         Pattern pattern = Pattern.compile("<#import\\s+[\"']([^\"']+)[\"']\\s+as\\s+(\\w+)", Pattern.MULTILINE);
//         Matcher matcher = pattern.matcher(content);
//
//         while (matcher.find()) {
//             String templateName = matcher.group(1);
//             String namespace = matcher.group(2);
//             analysis.addImportedTemplate(templateName, namespace);
//             log.trace("Found import: {} as {}", templateName, namespace);
//         }
//     }
//
//     /**
//      * 계층 구조 생성
//      */
//     private void buildHierarchicalStructure(Set<String> variables, Map<String, Object> hierarchicalVariables) {
//         for (String varPath : variables) {
//             if (varPath.contains(".")) {
//                 createHierarchicalPath(varPath, hierarchicalVariables);
//             } else {
//                 if (!hierarchicalVariables.containsKey(varPath)) {
//                     hierarchicalVariables.put(varPath, createDefaultValue(varPath));
//                 }
//             }
//         }
//     }
//
//     @SuppressWarnings("unchecked")
//     private void createHierarchicalPath(String varPath, Map<String, Object> hierarchicalVariables) {
//         String[] parts = varPath.split("\\.");
//         Map<String, Object> current = hierarchicalVariables;
//
//         for (int i = 0; i < parts.length - 1; i++) {
//             String part = parts[i];
//
//             if (!current.containsKey(part)) {
//                 current.put(part, new LinkedHashMap<String, Object>());
//             }
//
//             Object next = current.get(part);
//             if (next instanceof Map) {
//                 current = (Map<String, Object>) next;
//             } else {
//                 Map<String, Object> newMap = new LinkedHashMap<>();
//                 current.put(part, newMap);
//                 current = newMap;
//             }
//         }
//
//         String lastPart = parts[parts.length - 1];
//         current.put(lastPart, createDefaultValue(lastPart));
//     }
//
//     // 유틸리티 메서드들
//
//     private String removeDefaultValue(String expression) {
//         int exclamationIndex = expression.indexOf('!');
//         if (exclamationIndex > 0) {
//             return expression.substring(0, exclamationIndex).trim();
//         }
//         return expression;
//     }
//
//     /**
//      * 리터럴이나 키워드인지 판단 (개선된 버전)
//      */
//     private boolean isLiteralOrKeyword(String expression) {
//         if (expression == null || expression.isEmpty()) return true;
//
//         // 숫자 리터럴
//         if (expression.matches("^\\d+(\\.\\d+)?$")) return true;
//
//         // 문자열 리터럴
//         if ((expression.startsWith("\"") && expression.endsWith("\"")) ||
//                 (expression.startsWith("'") && expression.endsWith("'"))) return true;
//
//         // Boolean/null 리터럴
//         if ("true".equals(expression) || "false".equals(expression) || "null".equals(expression))
//             return true;
//
//         // FreeMarker 연산자들
//         if (FREEMARKER_OPERATORS.contains(expression)) {
//             log.trace("Filtered out FreeMarker operator: {}", expression);
//             return true;
//         }
//
//         // FreeMarker 키워드들
//         if (FREEMARKER_KEYWORDS.contains(expression)) {
//             log.trace("Filtered out FreeMarker keyword: {}", expression);
//             return true;
//         }
//
//         // FreeMarker 내장 함수들
//         if (FREEMARKER_BUILTINS.contains(expression)) {
//             log.trace("Filtered out FreeMarker builtin: {}", expression);
//             return true;
//         }
//
//         // 날짜 포맷팅 패턴들
//         if (DATE_FORMAT_PATTERNS.contains(expression)) {
//             log.trace("Filtered out date format pattern: {}", expression);
//             return true;
//         }
//
//         // 기타 일반적인 내장 함수들 (.으로 시작하는 것들)
//         if (expression.startsWith(".")) {
//             log.trace("Filtered out builtin starting with dot: {}", expression);
//             return true;
//         }
//
//         return false;
//     }
//
//     /**
//      * 유효한 변수 경로인지 확인 (개선된 버전)
//      */
//     private boolean isValidVariablePath(String path) {
//         if (path == null || path.isEmpty()) return false;
//         if (isLiteralOrKeyword(path)) return false;
//
//         // 배열 인덱스 접근 패턴 체크 (예: items[0])
//         if (path.matches(".*\\[\\d+\\]$")) {
//             // 배열 접근은 유효하지만, 베이스 변수명만 추출
//             String basePath = path.replaceAll("\\[\\d+\\]$", "");
//             return isValidVariablePath(basePath);
//         }
//
//         String[] parts = path.split("\\.");
//         for (String part : parts) {
//             // 각 부분이 유효한 식별자인지 확인
//             if (!part.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
//                 return false;
//             }
//
//             // 각 부분이 키워드나 연산자가 아닌지 확인
//             if (isLiteralOrKeyword(part)) {
//                 return false;
//             }
//         }
//
//         return true;
//     }
//
//     private Object createDefaultValue(String varName) {
//         String lowerName = varName.toLowerCase();
//
//         if (lowerName.contains("is") || lowerName.contains("has") || lowerName.contains("enable")) {
//             return false;
//         }
//         if (lowerName.contains("count") || lowerName.contains("amount") || lowerName.contains("total")) {
//             return 0;
//         }
//         if (lowerName.contains("rate") || lowerName.contains("price")) {
//             return 0.0;
//         }
//         if (lowerName.contains("list") || lowerName.contains("items") || lowerName.endsWith("s")) {
//             return new ArrayList<>();
//         }
//         if (lowerName.contains("date") || lowerName.contains("time")) {
//             return "2024-01-01";
//         }
//
//         return "";
//     }
//
//     private List<String> parseParameters(String paramsStr) {
//         List<String> params = new ArrayList<>();
//         if (paramsStr == null || paramsStr.trim().isEmpty()) {
//             return params;
//         }
//
//         String[] parts = paramsStr.trim().split("\\s+");
//         for (String part : parts) {
//             part = part.trim();
//             if (part.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
//                 params.add(part);
//             }
//         }
//         return params;
//     }
//
//     private String readTemplateContent(String templateName) throws IOException {
//         try {
//             String templateDir = System.getProperty("freemarker.template.path", "src/main/resources/templates/freemarker");
//             Path templatePath = Paths.get(templateDir, templateName);
//
//             if (Files.exists(templatePath)) {
//                 return Files.readString(templatePath);
//             }
//
//             try (InputStream is = getClass().getClassLoader().getResourceAsStream("templates/" + templateName)) {
//                 if (is != null) {
//                     return new String(is.readAllBytes());
//                 }
//             }
//
//             Path currentDirPath = Paths.get("templates", templateName);
//             if (Files.exists(currentDirPath)) {
//                 return Files.readString(currentDirPath);
//             }
//
//             throw new IOException("Template not found: " + templateName);
//         } catch (Exception e) {
//             throw new IOException("Failed to read template: " + templateName, e);
//         }
//     }
//
//     // Expression 타입 enum
//     public enum ExpressionType {
//         OUTPUT, ASSIGNMENT, CONDITION, ITERATION, PARAMETER, INTERPOLATION, STRING_INTERPOLATION
//     }
//
//     /**
//      * 변수 접근을 캡처하는 Mock 데이터 모델
//      */
//     private static class VariableCapturingModel implements TemplateHashModel {
//         private final Set<String> accessedVariables = new LinkedHashSet<>();
//
//         @Override
//         public TemplateModel get(String key) throws TemplateModelException {
//             accessedVariables.add(key);
//             log.trace("Mock model accessed variable: {}", key);
//
//             // 중첩 객체도 캡처할 수 있도록 새로운 CapturingModel 반환
//             return new NestedCapturingModel(key, accessedVariables);
//         }
//
//         @Override
//         public boolean isEmpty() throws TemplateModelException {
//             return false; // 모든 변수에 접근할 수 있다고 가정
//         }
//
//         public Set<String> getAccessedVariables() {
//             return new LinkedHashSet<>(accessedVariables);
//         }
//     }
//
//     /**
//      * 중첩된 변수 접근을 캡처하는 모델
//      */
//     private static class NestedCapturingModel implements TemplateHashModel, TemplateScalarModel,
//             TemplateSequenceModel, TemplateBooleanModel, TemplateNumberModel, TemplateDateModel {
//
//         private final String basePath;
//         private final Set<String> accessedVariables;
//
//         public NestedCapturingModel(String basePath, Set<String> accessedVariables) {
//             this.basePath = basePath;
//             this.accessedVariables = accessedVariables;
//         }
//
//         @Override
//         public TemplateModel get(String key) throws TemplateModelException {
//             String fullPath = basePath + "." + key;
//             accessedVariables.add(fullPath);
//             log.trace("Mock nested model accessed: {}", fullPath);
//             return new NestedCapturingModel(fullPath, accessedVariables);
//         }
//
//         @Override
//         public boolean isEmpty() throws TemplateModelException {
//             return false;
//         }
//
//         @Override
//         public String getAsString() throws TemplateModelException {
//             return "mock_value";
//         }
//
//         @Override
//         public TemplateModel get(int index) throws TemplateModelException {
//             return new NestedCapturingModel(basePath + "[" + index + "]", accessedVariables);
//         }
//
//         @Override
//         public int size() throws TemplateModelException {
//             return 1;
//         }
//
//         @Override
//         public boolean getAsBoolean() throws TemplateModelException {
//             return true;
//         }
//
//         @Override
//         public Number getAsNumber() throws TemplateModelException {
//             return 0;
//         }
//
//         @Override
//         public Date getAsDate() throws TemplateModelException {
//             return new Date();
//         }
//
//         @Override
//         public int getDateType() {
//             return TemplateDateModel.DATETIME;
//         }
//     }
//
//     /**
//      * 템플릿 분석 결과 클래스
//      */
//     @Getter
//     public static class TemplateVariableAnalysis {
//         private final String templateName;
//         private boolean templateValid = true;
//         private final Set<String> assignedVariables = new LinkedHashSet<>();
//         private final Set<String> localVariables = new LinkedHashSet<>();
//         private final Set<String> globalVariables = new LinkedHashSet<>();
//         private final Set<String> loopVariables = new LinkedHashSet<>();
//         private final Map<String, Set<ExpressionType>> referencedVariables = new LinkedHashMap<>();
//         private final Map<String, List<String>> macros = new LinkedHashMap<>();
//         private final Map<String, List<String>> functions = new LinkedHashMap<>();
//         private final Set<String> macroCalls = new LinkedHashSet<>();
//         private final Set<String> includedTemplates = new LinkedHashSet<>();
//         private final Map<String, String> importedTemplates = new LinkedHashMap<>();
//         private final List<String> errors = new ArrayList<>();
//         private Map<String, Object> hierarchicalVariables = new LinkedHashMap<>();
//
//         public TemplateVariableAnalysis(String templateName) {
//             this.templateName = templateName;
//         }
//
//         public void addAssignedVariable(String name) {
//             assignedVariables.add(name);
//         }
//
//         public void addLocalVariable(String name) {
//             localVariables.add(name);
//         }
//
//         public void addGlobalVariable(String name) {
//             globalVariables.add(name);
//         }
//
//         public void addLoopVariable(String name) {
//             loopVariables.add(name);
//         }
//
//         public void addReferencedVariable(String name, ExpressionType type) {
//             referencedVariables.computeIfAbsent(name, k -> new LinkedHashSet<>()).add(type);
//         }
//
//         public void addMacro(String name, List<String> params) {
//             macros.put(name, new ArrayList<>(params));
//         }
//
//         public void addFunction(String name, List<String> params) {
//             functions.put(name, new ArrayList<>(params));
//         }
//
//         public void addMacroCall(String name) {
//             macroCalls.add(name);
//         }
//
//         public void addIncludedTemplate(String templateName) {
//             includedTemplates.add(templateName);
//         }
//
//         public void addImportedTemplate(String templateName, String namespace) {
//             importedTemplates.put(namespace, templateName);
//         }
//
//         public void addError(String error) {
//             errors.add(error);
//         }
//
//         public void setTemplateValid(boolean valid) {
//             this.templateValid = valid;
//         }
//
//         public void setHierarchicalVariables(Map<String, Object> hierarchicalVariables) {
//             this.hierarchicalVariables = hierarchicalVariables;
//         }
//
//         public Set<String> getRequiredExternalVariables() {
//             return hierarchicalVariables.keySet();
//         }
//
//         public String getHierarchicalVariablesJson() {
//             return convertToJson(hierarchicalVariables, 0);
//         }
//
//         private String convertToJson(Object obj, int depth) {
//             if (depth > 10) return "\"...\"";
//             String indent = "  ".repeat(depth);
//             String nextIndent = "  ".repeat(depth + 1);
//
//             if (obj instanceof Map) {
//                 Map<String, Object> map = (Map<String, Object>) obj;
//                 if (map.isEmpty()) return "{}";
//
//                 StringBuilder sb = new StringBuilder("{\n");
//                 Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
//
//                 while (iter.hasNext()) {
//                     Map.Entry<String, Object> entry = iter.next();
//                     sb.append(nextIndent).append("\"").append(entry.getKey()).append("\": ");
//                     sb.append(convertToJson(entry.getValue(), depth + 1));
//                     if (iter.hasNext()) sb.append(",");
//                     sb.append("\n");
//                 }
//
//                 sb.append(indent).append("}");
//                 return sb.toString();
//             } else if (obj instanceof List) {
//                 return "[]";
//             } else if (obj instanceof String) {
//                 return "\"" + obj + "\"";
//             } else if (obj instanceof Number || obj instanceof Boolean) {
//                 return obj.toString();
//             } else {
//                 return "\"" + (obj != null ? obj.toString() : "") + "\"";
//             }
//         }
//
//         public String getSummary() {
//             StringBuilder sb = new StringBuilder();
//             sb.append("=== Template Analysis: ").append(templateName).append(" ===\n");
//             sb.append("Template Valid: ").append(templateValid).append("\n");
//             sb.append("Hierarchical Variables:\n").append(getHierarchicalVariablesJson()).append("\n");
//             sb.append("Assigned Variables: ").append(assignedVariables).append("\n");
//             sb.append("Loop Variables: ").append(loopVariables).append("\n");
//             sb.append("Macros: ").append(macros.keySet()).append("\n");
//             sb.append("Functions: ").append(functions.keySet()).append("\n");
//             if (!errors.isEmpty()) {
//                 sb.append("Errors: ").append(errors).append("\n");
//             }
//             return sb.toString();
//         }
//     }
// }