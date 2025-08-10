package com.boxwood.form.engine.form.service.impl;

import com.boxwood.form.engine.form.model.*;
import com.boxwood.form.engine.form.service.TemplateAnalysisService;
import com.boxwood.form.engine.form.utils.FreeMarkerVariableExtractor;
import com.boxwood.form.engine.form.utils.FreeMarkerVariableExtractor.TemplateVariableAnalysis;
import freemarker.template.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.boxwood.form.engine.form.controller.TemplateAnalysisController.getType;
import static com.boxwood.form.engine.form.controller.TemplateAnalysisController.examineIfFormRelated;
import static com.boxwood.form.engine.form.utils.FreeMarkerVariableExtractor.*;

@Service
@Slf4j
public class TemplateAnalysisServiceImpl implements TemplateAnalysisService {
    private final FreeMarkerVariableExtractor extractor;
    private final Configuration freeMarkerConfig;

    public TemplateAnalysisServiceImpl(
            FreeMarkerVariableExtractor extractor,
            Configuration freeMarkerConfig
    ) {
        this.extractor = extractor;
        this.freeMarkerConfig = freeMarkerConfig;
    }

    @Override
    public TemplateAnalysisResponseDto analyzeTemplate(String templateName) {
        try {
            log.info("Starting template analysis for: {}", templateName);
            TemplateVariableAnalysis analysis = extractor.analyzeTemplate(templateName);
            return convertToDto(analysis);
        } catch (Exception e) {
            log.error("Failed to analyze template: {}", templateName, e);
            return TemplateAnalysisResponseDto.builder()
                    .templateName(templateName)
                    .templateValid(false)
                    .errors(Arrays.asList("Analysis failed: " + e.getMessage()))
                    .requiredExternalVariables(new HashSet<>())
                    .formVariables(new HashSet<>())
                    .assignedVariables(new HashSet<>())
                    .localVariables(new HashSet<>())
                    .globalVariables(new HashSet<>())
                    .loopVariables(new HashSet<>())
                    .referencedVariables(new HashMap<>())
                    .macros(new HashMap<>())
                    .functions(new HashMap<>())
                    .macroCalls(new HashSet<>())
                    .includedTemplates(new HashSet<>())
                    .importedTemplates(new HashMap<>())
                    .summary("Analysis failed")
                    .build();
        }
    }

    @Override
    public TemplateRenderResponseDto renderTemplate(TemplateRenderRequestDto request) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Rendering template: {} with {} variables",
                    request.getTemplateName(),
                    request.getVariables() != null ? request.getVariables().size() : 0);

            Template template = freeMarkerConfig.getTemplate(request.getTemplateName());
            StringWriter writer = new StringWriter();

            Map<String, Object> dataModel = request.getVariables() != null ?
                    request.getVariables() : new HashMap<>();

            template.process(dataModel, writer);

            long renderTime = System.currentTimeMillis() - startTime;

            TemplateRenderResponseDto.TemplateRenderResponseDtoBuilder builder = TemplateRenderResponseDto.builder()
                    .templateName(request.getTemplateName())
                    .renderedHtml(writer.toString())
                    .success(true)
                    .errors(new ArrayList<>())
                    .renderTimeMs(renderTime);

            if (request.isIncludeDebugInfo()) {
                Map<String, Object> debugInfo = new HashMap<>();
                debugInfo.put("variableCount", dataModel.size());
                debugInfo.put("templateSize", writer.toString().length());
                debugInfo.put("renderTime", renderTime);
                debugInfo.put("timestamp", LocalDateTime.now());
                builder.debugInfo(debugInfo);
            }

            return builder.build();

        } catch (IOException | TemplateException e) {
            log.error("Failed to render template: {}", request.getTemplateName(), e);
            long renderTime = System.currentTimeMillis() - startTime;

            return TemplateRenderResponseDto.builder()
                    .templateName(request.getTemplateName())
                    .renderedHtml("")
                    .success(false)
                    .errors(List.of("Render failed: " + e.getMessage()))
                    .renderTimeMs(renderTime)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TemplateRenderResponseDto renderTemplatePreview(String templateName) {
        try {
            // 계층 구조 기반 기본값 생성
            Map<String, Object> defaultVariables = createHierarchicalDefaults(templateName);

            TemplateRenderRequestDto request = TemplateRenderRequestDto.builder()
                    .templateName(templateName)
                    .variables(defaultVariables)
                    .includeDebugInfo(true)
                    .build();

            return renderTemplate(request);

        } catch (Exception e) {
            log.error("Failed to render template preview: {}", templateName, e);
            return TemplateRenderResponseDto.builder()
                    .templateName(templateName)
                    .renderedHtml("")
                    .success(false)
                    .errors(Arrays.asList("Preview failed: " + e.getMessage()))
                    .renderTimeMs(0)
                    .build();
        }
    }

    @Override
    public TemplateVariableMapDto getTemplateVariableMap(String templateName) {
        try {
            TemplateVariableAnalysis analysis = extractor.analyzeTemplate(templateName);

            // 계층 구조 변수 가져오기
            Map<String, Object> hierarchicalVars = analysis.getHierarchicalVariables();

            // 평면화된 변수들 (역호환성을 위해)
            Map<String, Object> flattenedVars = flattenHierarchicalVariables(hierarchicalVars);

            // 폼 관련 변수들
            Map<String, Object> formVars = filterFormVariables(flattenedVars);

            Map<String, String> descriptions = new LinkedHashMap<>();
            for (String var : hierarchicalVars.keySet()) {
                descriptions.put(var, generateVariableDescription(var, analysis));
            }

            return TemplateVariableMapDto.builder()
                    .templateName(templateName)
                    .requiredVariables(hierarchicalVars)  // 계층 구조로 반환
                    .formVariables(formVars)
                    .allVariables(hierarchicalVars)
                    .missingVariables(new ArrayList<>())
                    .variableDescriptions(descriptions)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get variable map for template: {}", templateName, e);
            return TemplateVariableMapDto.builder()
                    .templateName(templateName)
                    .requiredVariables(new HashMap<>())
                    .formVariables(new HashMap<>())
                    .allVariables(new HashMap<>())
                    .missingVariables(Arrays.asList("Error: " + e.getMessage()))
                    .variableDescriptions(new HashMap<>())
                    .build();
        }
    }

    @Override
    public Map<String, Object> getRequiredVariablesWithDefaults(String templateName) {
        try {
            TemplateVariableAnalysis analysis = extractor.analyzeTemplate(templateName);
            return analysis.getHierarchicalVariables();
        } catch (Exception e) {
            log.error("Failed to get required variables with defaults: {}", templateName, e);
            return new HashMap<>();
        }
    }

    @Override
    public TemplateAnalysisResponseDto convertToDto(TemplateVariableAnalysis analysis) {
        Map<String, TemplateVariableDto> referencedVars = new LinkedHashMap<>();

        // 참조된 변수들을 DTO로 변환
        analysis.getReferencedVariables().forEach((varName, usageTypes) -> {
            TemplateVariableDto varDto = TemplateVariableDto.builder()
                    .name(varName)
                    .usageTypes(usageTypes)
                    .isRequired(analysis.getRequiredExternalVariables().contains(varName))
                    .isFormRelated(isFormRelatedVariable(varName))
                    .description(generateVariableDescription(varName, analysis))
                    .build();
            referencedVars.put(varName, varDto);
        });

        return TemplateAnalysisResponseDto.builder()
                .templateName(analysis.getTemplateName())
                .templateValid(analysis.isTemplateValid())
                .requiredExternalVariables(analysis.getRequiredExternalVariables())
                .formVariables(filterFormVariableNames(analysis.getRequiredExternalVariables()))
                .assignedVariables(analysis.getAssignedVariables())
                .localVariables(analysis.getLocalVariables())
                .globalVariables(analysis.getGlobalVariables())
                .loopVariables(analysis.getLoopVariables())
                .referencedVariables(referencedVars)
                .macros(analysis.getMacros())
                .functions(analysis.getFunctions())
                .macroCalls(analysis.getMacroCalls())
                .includedTemplates(analysis.getIncludedTemplates())
                .importedTemplates(analysis.getImportedTemplates())
                .errors(analysis.getErrors())
                .summary(analysis.getSummary())
                .build();
    }

    @Override
    public boolean validateTemplate(String templateName) {
        try {
            Template template = freeMarkerConfig.getTemplate(templateName);
            return template != null;
        } catch (Exception e) {
            log.warn("Template validation failed for: {}", templateName, e);
            return false;
        }
    }

    /**
     * 템플릿 분석 원본 객체 반환 (계층구조 API용)
     */
    public TemplateVariableAnalysis analyzeTemplateRaw(String templateName) {
        try {
            log.info("Starting raw template analysis for: {}", templateName);
            return extractor.analyzeTemplate(templateName);
        } catch (Exception e) {
            log.error("Failed to analyze template raw: {}", templateName, e);
            throw new RuntimeException("Template analysis failed: " + e.getMessage(), e);
        }
    }

    /**
     * 계층 구조 기반 기본값 생성
     */
    private Map<String, Object> createHierarchicalDefaults(String templateName) {
        try {
            TemplateVariableAnalysis analysis = extractor.analyzeTemplate(templateName);
            Map<String, Object> hierarchicalVars = analysis.getHierarchicalVariables();

            // 계층 구조에 기본값 채우기
            return populateDefaultValues(hierarchicalVars);

        } catch (Exception e) {
            log.error("Failed to create hierarchical defaults for: {}", templateName, e);
            return createFallbackDefaults();
        }
    }

    /**
     * 계층 구조에 기본값 채우기
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> populateDefaultValues(Map<String, Object> variables) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                // 중첩된 객체인 경우
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                result.put(key, populateDefaultValues(nestedMap));
            } else {
                // 기본값 생성
                result.put(key, createIntelligentDefaultValue(key, value));
            }
        }

        return result;
    }

    /**
     * 지능적인 기본값 생성 (정교하게 수정된 버전)
     */
    private Object createIntelligentDefaultValue(String varName, Object existingValue) {
        String lowerName = varName.toLowerCase();

        // === 🔥 특별한 변수들 (정확한 매칭이 우선) ===

        // index.ftl 관련
        if ("templates".equals(lowerName)) {
            return createTemplateList();
        }

        // invoice.ftl 관련 - 정확한 이름으로 매칭
        if ("company".equals(lowerName)) {
            return createCompanyInfo();
        }

        if ("client".equals(lowerName)) {
            return createClientInfo();
        }

        if ("items".equals(lowerName)) {
            return createItemList();
        }

        // === 🔥 단수형인데 s로 끝나는 변수들 (예외 처리) ===
        Set<String> singularWordsEndingWithS = Set.of(
                "paymentterms", "terms", "address", "business", "notes",
                "status", "process", "access", "success", "class", "pass",
                "sales", "news", "series", "species", "means", "headquarters"
        );

        if (singularWordsEndingWithS.contains(lowerName)) {
            return createSingularValue(varName);
        }

        // === 🔥 확실한 리스트 변수들 ===
        Set<String> knownListVariables = Set.of(
                "items", "templates", "users", "products", "files", "documents",
                "records", "entries", "values", "results", "orders", "invoices"
        );

        if (knownListVariables.contains(lowerName)) {
            return createListValue(lowerName);
        }

        // === 일반적인 패턴 매칭 ===

        // 날짜/시간 타입
        if (lowerName.contains("date") || lowerName.contains("time")) {
            if (lowerName.contains("due")) {
                return LocalDate.now().plusDays(30);
            }
            return LocalDate.now();
        }

        // 숫자 타입
        if (lowerName.contains("amount") || lowerName.contains("total")) {
            return 9000000;
        }

        if (lowerName.contains("rate") || lowerName.contains("tax")) {
            return 10.0;
        }

        if (lowerName.contains("quantity") || lowerName.contains("count")) {
            return 1;
        }

        // ID/번호 타입
        if (lowerName.contains("number")) {
            if (lowerName.contains("invoice")) {
                return "INV-" + LocalDate.now().toString().replace("-", "") + "-001";
            }
            if (lowerName.contains("po")) {
                return "PO-2024-001";
            }
            if (lowerName.contains("reference")) {
                return "REF-2024-001";
            }
        }

        // 타입 관련
        if (lowerName.contains("type")) {
            if (lowerName.contains("document")) {
                return "송장";
            }
            if (lowerName.contains("payment")) {
                return "현금";
            }
            if (lowerName.contains("discount")) {
                return "금액";
            }
        }

        // Boolean 타입
        if (lowerName.startsWith("is") || lowerName.startsWith("has") || lowerName.startsWith("can") ||
                lowerName.contains("enable") || lowerName.contains("active") || lowerName.contains("valid")) {
            return true;
        }

        // === 일반적인 문자열 필드들 (정확한 매칭) ===
        String exactMatch = getExactMatch(lowerName);
        if (exactMatch != null) {
            return exactMatch;
        }

        // === 🔥 마지막으로 복수형 판단 (더 정교하게) ===
        if (isPluralVariable(lowerName)) {
            return createListValue(lowerName);
        }

        // 기본 문자열
        return "Sample " + capitalize(varName);
    }

    private static String getExactMatch(String lowerName) {
        Map<String, String> exactMatches = Map.of(
                "salesperson", "김영업",
                "projectname", "BOXWOOD 프로젝트",
                "paymentterms", "30일 이내 현금결제",  // 🔥 정확히 지정
                "paymentinfo", "농협은행 123-456-789012 (주)박스우드테크놀로지",
                "notes", "본 송장은 세금계산서를 포함하고 있습니다. 지불기한을 준수해주시기 바랍니다.",
                "title", "템플릿 목록",
                "subtitle", "사용 가능한 FreeMarker 템플릿들",
                "currentuser", "관리자",
                "documenttype", "송장",
                "invoicenumber", "INV-20250108-001"
        );

        String exactMatch = exactMatches.get(lowerName);
        return exactMatch;
    }

    /**
     * 복수형 변수인지 판단 (더 정교한 로직)
     */
    private boolean isPluralVariable(String lowerName) {
        // 확실히 단수인 것들 제외
        Set<String> singularWords = Set.of(
                "paymentterms", "terms", "address", "business", "notes", "status",
                "process", "access", "success", "class", "pass", "sales", "news"
        );

        if (singularWords.contains(lowerName)) {
            return false;
        }

        // list, data, records 등은 복수형
        if (lowerName.contains("list") || lowerName.contains("data") ||
                lowerName.contains("records") || lowerName.contains("entries")) {
            return true;
        }

        // s로 끝나면서 복수형일 가능성이 높은 것들
        if (lowerName.endsWith("s") && !lowerName.endsWith("ss")) {
            // 일반적인 복수형 패턴들
            if (lowerName.endsWith("ies") || lowerName.endsWith("ves") ||
                    lowerName.endsWith("oes") || lowerName.length() > 5) {
                return true;
            }
        }

        return false;
    }

    /**
     * 단수 문자열 값 생성
     */
    private String createSingularValue(String varName) {
        String lowerName = varName.toLowerCase();

        if (lowerName.contains("payment")) {
            return "30일 이내 현금결제";
        }
        if (lowerName.contains("note")) {
            return "추가 안내사항입니다.";
        }
        if (lowerName.contains("term")) {
            return "표준 약관";
        }
        if (lowerName.contains("address")) {
            return "서울시 강남구 테헤란로 123";
        }
        if (lowerName.contains("business")) {
            return "소프트웨어 개발";
        }

        return "Sample " + capitalize(varName);
    }

    /**
     * 리스트 값 생성
     */
    private List<?> createListValue(String lowerName) {
        if (lowerName.contains("template")) {
            return createTemplateList();
        }
        if (lowerName.contains("user")) {
            return createUserList();
        }
        if (lowerName.contains("item")) {
            return createItemList();
        }
        if (lowerName.contains("product")) {
            return createProductList();
        }

        // 기본 문자열 리스트
        return Arrays.asList("Item 1", "Item 2", "Item 3");
    }

// === 헬퍼 메서드들 ===

    private List<Map<String, Object>> createTemplateList() {
        List<Map<String, Object>> templates = new ArrayList<>();

        Map<String, Object> t1 = new LinkedHashMap<>();
        t1.put("name", "invoice.ftl");
        t1.put("description", "송장 템플릿");
        t1.put("type", "document");
        templates.add(t1);

        Map<String, Object> t2 = new LinkedHashMap<>();
        t2.put("name", "receipt.ftl");
        t2.put("description", "영수증 템플릿");
        t2.put("type", "document");
        templates.add(t2);

        return templates;
    }

    private Map<String, Object> createCompanyInfo() {
        Map<String, Object> company = new LinkedHashMap<>();
        company.put("name", "BOXWOOD Technology");
        company.put("address", "서울시 강남구 테헤란로 123");
        company.put("phone", "02-1234-5678");
        company.put("email", "info@boxwood.com");
        company.put("website", "https://www.boxwood.com");
        company.put("businessNumber", "123-45-67890");
        return company;
    }

    private Map<String, Object> createClientInfo() {
        Map<String, Object> client = new LinkedHashMap<>();
        client.put("name", "Sample Client Co.");
        client.put("contactPerson", "홍길동");
        client.put("address", "서울시 서초구 서초대로 456");
        client.put("phone", "02-9876-5432");
        client.put("email", "contact@client.com");
        client.put("businessNumber", "098-76-54321");
        return client;
    }

    private List<Map<String, Object>> createItemList() {
        List<Map<String, Object>> items = new ArrayList<>();

        Map<String, Object> item1 = new LinkedHashMap<>();
        item1.put("name", "웹사이트 개발");
        item1.put("description", "반응형 웹사이트 개발 및 유지보수");
        item1.put("quantity", 1);
        item1.put("rate", 5000000);
        items.add(item1);

        Map<String, Object> item2 = new LinkedHashMap<>();
        item2.put("name", "시스템 컨설팅");
        item2.put("description", "업무 프로세스 분석 및 시스템 설계");
        item2.put("quantity", 2);
        item2.put("rate", 2000000);
        items.add(item2);

        return items;
    }

    private List<Map<String, Object>> createUserList() {
        List<Map<String, Object>> users = new ArrayList<>();

        Map<String, Object> user1 = new LinkedHashMap<>();
        user1.put("id", 1);
        user1.put("name", "홍길동");
        user1.put("email", "hong@example.com");
        users.add(user1);

        return users;
    }

    private List<Map<String, Object>> createProductList() {
        List<Map<String, Object>> products = new ArrayList<>();

        Map<String, Object> product1 = new LinkedHashMap<>();
        product1.put("id", 1);
        product1.put("name", "웹사이트 개발");
        product1.put("price", 5000000);
        products.add(product1);

        return products;
    }


    /**
     * 계층 구조 변수를 평면화 (역호환성)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> flattenHierarchicalVariables(Map<String, Object> hierarchical) {
        Map<String, Object> flattened = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : hierarchical.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                for (Map.Entry<String, Object> nestedEntry : nestedMap.entrySet()) {
                    flattened.put(key + "." + nestedEntry.getKey(), nestedEntry.getValue());
                }
            } else {
                flattened.put(key, value);
            }
        }

        return flattened;
    }

    /**
     * 폼 관련 변수 필터링
     */
    private Map<String, Object> filterFormVariables(Map<String, Object> variables) {
        return variables.entrySet().stream()
                .filter(entry -> isFormRelatedVariable(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private Set<String> filterFormVariableNames(Set<String> variableNames) {
        return variableNames.stream()
                .filter(this::isFormRelatedVariable)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 변수 설명 생성
     */
    private String generateVariableDescription(String varName, TemplateVariableAnalysis analysis) {
        Set<ExpressionType> usages = analysis.getReferencedVariables().get(varName);
        if (usages == null || usages.isEmpty()) {
            return "Variable used in template";
        }

        List<String> usageDescriptions = usages.stream()
                .map(this::getUsageDescription)
                .collect(Collectors.toList());

        return "Used for: " + String.join(", ", usageDescriptions);
    }

    private String getUsageDescription(ExpressionType type) {
        return getType(type);
    }

    /**
     * 폼 관련 변수 판단
     */
    private boolean isFormRelatedVariable(String varName) {
        return examineIfFormRelated(varName);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 폴백 기본값 (분석 실패 시)
     */
    private Map<String, Object> createFallbackDefaults() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("company", createIntelligentDefaultValue("company", null));
        defaults.put("client", createIntelligentDefaultValue("client", null));
        defaults.put("items", createIntelligentDefaultValue("items", null));
        defaults.put("documentType", "송장");
        defaults.put("invoiceNumber", "INV-20241201-001");
        defaults.put("issueDate", LocalDate.now());
        defaults.put("dueDate", LocalDate.now().plusDays(30));
        return defaults;
    }
}