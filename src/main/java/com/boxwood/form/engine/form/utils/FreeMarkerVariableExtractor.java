package com.boxwood.form.engine.form.utils;

import freemarker.template.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Mock Environment + 정교한 정규식 기반 FreeMarker 변수 추출 서비스
 * 안전하고 실용적인 접근법
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
            log.info("Starting template analysis for: {}", templateName);

            // 1. 템플릿 내용 읽기
            String templateContent = readTemplateContent(templateName);

            // 2. Mock Environment를 통한 변수 추출 시도
            Set<String> mockExtractedVars = extractVariablesUsingMockEnvironment(templateName);

            // 3. 정교한 정규식을 통한 변수 추출
            Set<String> regexExtractedVars = extractVariablesUsingRegex(templateContent, analysis);

            // 4. 두 결과를 합치기
            Set<String> allVariables = new LinkedHashSet<>();
            allVariables.addAll(mockExtractedVars);
            allVariables.addAll(regexExtractedVars);

            // 5. 계층 구조 생성
            Map<String, Object> hierarchicalVariables = new LinkedHashMap<>();
            buildHierarchicalStructure(allVariables, hierarchicalVariables);

            analysis.setHierarchicalVariables(hierarchicalVariables);
            analysis.setTemplateValid(true);

            log.info("Analysis completed: {} variables found (mock: {}, regex: {})",
                    allVariables.size(), mockExtractedVars.size(), regexExtractedVars.size());
            log.debug("All variables: {}", allVariables);

        } catch (Exception e) {
            log.error("Template analysis failed for: {}", templateName, e);
            analysis.addError("Analysis failed: " + e.getMessage());
            analysis.setTemplateValid(false);
        }

        return analysis;
    }

    /**
     * Mock Environment를 사용한 변수 추출
     */
    private Set<String> extractVariablesUsingMockEnvironment(String templateName) {
        Set<String> variables = new LinkedHashSet<>();

        try {
            Template template = freeMarkerConfig.getTemplate(templateName);

            // Mock 데이터 모델 생성
            VariableCapturingModel mockModel = new VariableCapturingModel();

            // StringWriter로 출력 캡처
            StringWriter writer = new StringWriter();

            try {
                // 템플릿 실행하면서 변수 접근을 감지
                template.process(mockModel, writer);
            } catch (Exception e) {
                // 템플릿 실행 중 오류는 예상되는 상황 (변수가 없으니까)
                log.trace("Expected error during mock processing: {}", e.getMessage());
            }

            // 캡처된 변수들 가져오기
            variables.addAll(mockModel.getAccessedVariables());

        } catch (Exception e) {
            log.warn("Mock environment variable extraction failed: {}", e.getMessage());
        }

        return variables;
    }

    /**
     * 정교한 정규식을 사용한 변수 추출
     */
    private Set<String> extractVariablesUsingRegex(String content, TemplateVariableAnalysis analysis) {
        Set<String> variables = new LinkedHashSet<>();

        // 1. ${...} 표현식 추출
        extractDollarExpressions(content, variables);

        // 2. 지시어에서 변수 추출
        extractFromDirectives(content, analysis, variables);

        return variables;
    }

    /**
     * ${...} 표현식에서 변수 추출 - 매우 정교한 방식
     */
    private void extractDollarExpressions(String content, Set<String> variables) {
        // 중첩 괄호를 고려한 ${...} 패턴
        Pattern dollarPattern = Pattern.compile("\\$\\{([^}]+(?:\\{[^}]*\\}[^}]*)*)\\}", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = dollarPattern.matcher(content);

        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            log.trace("Found ${} expression: {}", expression);

            // 표현식에서 변수 경로들 추출
            extractVariablePathsFromExpression(expression, variables);
        }
    }

    /**
     * 단일 표현식에서 모든 변수 경로 추출
     */
    private void extractVariablePathsFromExpression(String expression, Set<String> variables) {
        // 1. 기본값 구문 처리 (variable!"default")
        String cleanExpr = removeDefaultValue(expression);

        // 2. 함수 호출과 연산자 제거하면서 변수 찾기
        Set<String> paths = findVariablePathsInExpression(cleanExpr);

        // 3. 유효한 변수만 추가
        for (String path : paths) {
            if (isValidVariablePath(path)) {
                variables.add(path);
                log.trace("Added variable path: {}", path);
            }
        }
    }

    /**
     * 표현식에서 변수 경로들 찾기
     */
    private Set<String> findVariablePathsInExpression(String expression) {
        Set<String> paths = new LinkedHashSet<>();

        // 1. 가장 단순한 경우: 단일 변수나 점 표기법
        if (expression.matches("^[a-zA-Z_][a-zA-Z0-9_.]*$")) {
            paths.add(expression);
            return paths;
        }

        // 2. 복잡한 표현식에서 변수 경로들 추출
        // 공백, 연산자, 괄호를 기준으로 토큰화
        String[] tokens = expression.split("[\\s()+\\-*/%<>=!&|,?:]+");

        for (String token : tokens) {
            token = token.trim();
            if (!token.isEmpty() && !isLiteralOrKeyword(token)) {
                // 점 표기법 또는 단순 변수인지 확인
                if (token.matches("^[a-zA-Z_][a-zA-Z0-9_.]*$")) {
                    paths.add(token);
                }
            }
        }

        // 3. 특수 패턴들 (method call 제거 등)
        findAdditionalPatterns(expression, paths);

        return paths;
    }

    /**
     * 추가적인 패턴들 찾기
     */
    private void findAdditionalPatterns(String expression, Set<String> paths) {
        // object.method() 형태에서 object 추출
        Pattern methodCallPattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_.]*)\\.\\w+\\s*\\(");
        Matcher methodMatcher = methodCallPattern.matcher(expression);
        while (methodMatcher.find()) {
            String objectPath = methodMatcher.group(1);
            if (isValidVariablePath(objectPath)) {
                paths.add(objectPath);
            }
        }

        // object?has_content 형태에서 object 추출
        Pattern builtinPattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_.]*)(\\?\\w+)+");
        Matcher builtinMatcher = builtinPattern.matcher(expression);
        while (builtinMatcher.find()) {
            String objectPath = builtinMatcher.group(1);
            if (isValidVariablePath(objectPath)) {
                paths.add(objectPath);
            }
        }
    }

    /**
     * 지시어에서 변수 추출
     */
    private void extractFromDirectives(String content, TemplateVariableAnalysis analysis, Set<String> variables) {
        // <#assign var = expression>
        extractAssignDirectives(content, analysis, variables);

        // <#list sequence as item>
        extractListDirectives(content, analysis, variables);

        // <#if condition>
        extractConditionalDirectives(content, variables);

        // <#macro name params>
        extractMacroDirectives(content, analysis);

        // <#function name params>
        extractFunctionDirectives(content, analysis);

        // <#include "template">
        extractIncludeDirectives(content, analysis);

        // <#import "template" as namespace>
        extractImportDirectives(content, analysis);
    }

    private void extractAssignDirectives(String content, TemplateVariableAnalysis analysis, Set<String> variables) {
        Pattern pattern = Pattern.compile("<#assign\\s+([^=\\s]+)\\s*=\\s*([^>]+)>", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            String valueExpr = matcher.group(2).trim();

            analysis.addAssignedVariable(varName);
            extractVariablePathsFromExpression(valueExpr, variables);
            log.trace("Found assign: {} = {}", varName, valueExpr);
        }
    }

    private void extractListDirectives(String content, TemplateVariableAnalysis analysis, Set<String> variables) {
        Pattern pattern = Pattern.compile("<#list\\s+([^\\s]+)\\s+as\\s+(\\w+)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String sequence = matcher.group(1).trim();
            String itemVar = matcher.group(2).trim();

            extractVariablePathsFromExpression(sequence, variables);
            analysis.addLoopVariable(itemVar);
            log.trace("Found list: {} as {}", sequence, itemVar);
        }
    }

    private void extractConditionalDirectives(String content, Set<String> variables) {
        Pattern pattern = Pattern.compile("<#(if|elseif)\\s+([^>]+)>", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String condition = matcher.group(2).trim();
            extractVariablePathsFromExpression(condition, variables);
            log.trace("Found condition: {}", condition);
        }
    }

    private void extractMacroDirectives(String content, TemplateVariableAnalysis analysis) {
        Pattern pattern = Pattern.compile("<#macro\\s+(\\w+)([^>]*)>", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String macroName = matcher.group(1);
            String paramsStr = matcher.group(2).trim();
            List<String> params = parseParameters(paramsStr);
            analysis.addMacro(macroName, params);
            log.trace("Found macro: {} with params: {}", macroName, params);
        }
    }

    private void extractFunctionDirectives(String content, TemplateVariableAnalysis analysis) {
        Pattern pattern = Pattern.compile("<#function\\s+(\\w+)([^>]*)>", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String funcName = matcher.group(1);
            String paramsStr = matcher.group(2).trim();
            List<String> params = parseParameters(paramsStr);
            analysis.addFunction(funcName, params);
            log.trace("Found function: {} with params: {}", funcName, params);
        }
    }

    private void extractIncludeDirectives(String content, TemplateVariableAnalysis analysis) {
        Pattern pattern = Pattern.compile("<#include\\s+[\"']([^\"']+)[\"']", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String templateName = matcher.group(1);
            analysis.addIncludedTemplate(templateName);
            log.trace("Found include: {}", templateName);
        }
    }

    private void extractImportDirectives(String content, TemplateVariableAnalysis analysis) {
        Pattern pattern = Pattern.compile("<#import\\s+[\"']([^\"']+)[\"']\\s+as\\s+(\\w+)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String templateName = matcher.group(1);
            String namespace = matcher.group(2);
            analysis.addImportedTemplate(templateName, namespace);
            log.trace("Found import: {} as {}", templateName, namespace);
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

    // 유틸리티 메서드들
    private String removeDefaultValue(String expression) {
        int exclamationIndex = expression.indexOf('!');
        if (exclamationIndex > 0) {
            return expression.substring(0, exclamationIndex).trim();
        }
        return expression;
    }

    private boolean isLiteralOrKeyword(String expression) {
        if (expression == null || expression.isEmpty()) return true;
        if (expression.matches("^\\d+(\\.\\d+)?$")) return true; // 숫자
        if ((expression.startsWith("\"") && expression.endsWith("\"")) ||
                (expression.startsWith("'") && expression.endsWith("'"))) return true; // 문자열
        if ("true".equals(expression) || "false".equals(expression) || "null".equals(expression))
            return true; // Boolean/null

        // FreeMarker 내장 함수들
        Set<String> builtins = Set.of("now", ".now", "is_hash", "is_date", "is_string", "is_number",
                "is_boolean", "is_sequence", "has_content", "size", "length", "string", "number");
        return builtins.contains(expression);
    }

    private boolean isValidVariablePath(String path) {
        if (path == null || path.isEmpty()) return false;
        if (isLiteralOrKeyword(path)) return false;

        String[] parts = path.split("\\.");
        for (String part : parts) {
            if (!part.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
                return false;
            }
        }
        return true;
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
     * 변수 접근을 캡처하는 Mock 데이터 모델
     */
    private static class VariableCapturingModel implements TemplateHashModel {
        private final Set<String> accessedVariables = new LinkedHashSet<>();

        @Override
        public TemplateModel get(String key) throws TemplateModelException {
            accessedVariables.add(key);
            log.trace("Mock model accessed variable: {}", key);

            // 중첩 객체도 캡처할 수 있도록 새로운 CapturingModel 반환
            return new NestedCapturingModel(key, accessedVariables);
        }

        @Override
        public boolean isEmpty() throws TemplateModelException {
            return false; // 모든 변수에 접근할 수 있다고 가정
        }

        public Set<String> getAccessedVariables() {
            return new LinkedHashSet<>(accessedVariables);
        }
    }

    /**
     * 중첩된 변수 접근을 캡처하는 모델
     */
    private static class NestedCapturingModel implements TemplateHashModel, TemplateScalarModel,
            TemplateSequenceModel, TemplateBooleanModel, TemplateNumberModel, TemplateDateModel {

        private final String basePath;
        private final Set<String> accessedVariables;

        public NestedCapturingModel(String basePath, Set<String> accessedVariables) {
            this.basePath = basePath;
            this.accessedVariables = accessedVariables;
        }

        @Override
        public TemplateModel get(String key) throws TemplateModelException {
            String fullPath = basePath + "." + key;
            accessedVariables.add(fullPath);
            log.trace("Mock nested model accessed: {}", fullPath);
            return new NestedCapturingModel(fullPath, accessedVariables);
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
            return new NestedCapturingModel(basePath + "[" + index + "]", accessedVariables);
        }

        @Override
        public int size() throws TemplateModelException {
            return 1;
        }

        @Override
        public boolean getAsBoolean() throws TemplateModelException {
            return true;
        }

        @Override
        public Number getAsNumber() throws TemplateModelException {
            return 0;
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
            sb.append("=== Template Analysis: ").append(templateName).append(" ===\n");
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