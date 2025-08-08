// 1. 템플릿 서버 - API로 HTML 반환하는 컨트롤러
package com.boxwood.form.engine.test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController  // @Controller에서 @RestController로 변경
@RequestMapping("/api/templates")  // API 경로로 변경
public class TemplateApiController {

    @Autowired
    private Configuration freemarkerConfig;  // FreeMarker Configuration 주입

    // 메인 페이지 - JSON으로 템플릿 목록 반환
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getTemplateList() {
        List<Map<String, String>> templates = new ArrayList<>();

        Map<String, String> template1 = new HashMap<>();
        template1.put("name", "송장/세금계산서");
        template1.put("endpoint", "/api/templates/invoice");
        template1.put("template", "invoice.ftl");
        templates.add(template1);

        Map<String, String> template2 = new HashMap<>();
        template2.put("name", "월간 보고서");
        template2.put("endpoint", "/api/templates/monthly-report");
        template2.put("template", "monthly-report.ftl");
        templates.add(template2);

        Map<String, String> template3 = new HashMap<>();
        template3.put("name", "시스템 알림");
        template3.put("endpoint", "/api/templates/system-notification");
        template3.put("template", "system-notification.ftl");
        templates.add(template3);

        Map<String, String> template4 = new HashMap<>();
        template4.put("name", "사용자 프로필");
        template4.put("endpoint", "/api/templates/user-profile");
        template4.put("template", "user-profile.ftl");
        templates.add(template4);

        Map<String, String> template5 = new HashMap<>();
        template5.put("name", "환영 이메일");
        template5.put("endpoint", "/api/templates/welcome-email");
        template5.put("template", "welcome-email.ftl");
        templates.add(template5);

        Map<String, Object> response = new HashMap<>();
        response.put("templates", templates);
        response.put("total", templates.size());

        return ResponseEntity.ok(response);
    }

    // 송장 HTML 생성 API
    @GetMapping("/invoice")
    public ResponseEntity<String> generateInvoiceHtml() {
        try {
            // 기존 데이터 준비 로직 (동일)
            Map<String, Object> model = prepareInvoiceData();

            // FreeMarker 템플릿으로 HTML 생성
            String html = processTemplate("invoice.ftl", model);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .header(HttpHeaders.CONTENT_ENCODING, "UTF-8")
                    .body(html);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("템플릿 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // POST 방식으로 동적 데이터 받아서 송장 생성
    @PostMapping("/invoice")
    public ResponseEntity<String> generateCustomInvoiceHtml(@RequestBody Map<String, Object> requestData) {
        try {
            // 요청 데이터와 기본 데이터 병합
            Map<String, Object> model = prepareInvoiceData();

            // 요청으로 받은 데이터로 덮어쓰기 (예: 회사정보, 고객정보 등)
            if (requestData.containsKey("company")) {
                model.put("company", requestData.get("company"));
            }
            if (requestData.containsKey("client")) {
                model.put("client", requestData.get("client"));
            }
            if (requestData.containsKey("items")) {
                model.put("items", requestData.get("items"));
                // 아이템이 변경되면 금액도 재계산
                recalculateInvoiceAmounts(model, (List<Map<String, Object>>) requestData.get("items"));
            }

            String html = processTemplate("invoice.ftl", model);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("템플릿 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // 월간 보고서 HTML 생성 API
    @GetMapping("/monthly-report")
    public ResponseEntity<String> generateMonthlyReportHtml() {
        try {
            Map<String, Object> model = prepareMonthlyReportData();
            String html = processTemplate("monthly-report.ftl", model);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("템플릿 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // 동적 월간 보고서 생성
    @PostMapping("/monthly-report")
    public ResponseEntity<String> generateCustomMonthlyReportHtml(@RequestBody Map<String, Object> requestData) {
        try {
            Map<String, Object> model = prepareMonthlyReportData();

            // 요청 데이터로 덮어쓰기
            model.putAll(requestData);

            String html = processTemplate("monthly-report.ftl", model);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("템플릿 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // 시스템 알림 HTML 생성 API
    @GetMapping("/system-notification")
    public ResponseEntity<String> generateSystemNotificationHtml() {
        try {
            Map<String, Object> model = prepareSystemNotificationData();
            String html = processTemplate("system-notification.ftl", model);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("템플릿 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // 사용자 프로필 HTML 생성 API
    @GetMapping("/user-profile")
    public ResponseEntity<String> generateUserProfileHtml() {
        try {
            Map<String, Object> model = prepareUserProfileData();
            String html = processTemplate("user-profile.ftl", model);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("템플릿 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // 환영 이메일 HTML 생성 API
    @GetMapping("/welcome-email")
    public ResponseEntity<String> generateWelcomeEmailHtml() {
        try {
            Map<String, Object> model = prepareWelcomeEmailData();
            String html = processTemplate("welcome-email.ftl", model);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("템플릿 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // 범용 템플릿 처리 API (동적으로 템플릿명과 데이터 받기)
    @PostMapping("/render/{templateName}")
    public ResponseEntity<String> renderTemplate(
            @PathVariable String templateName,
            @RequestBody Map<String, Object> model) {
        try {
            // 보안을 위해 허용된 템플릿만 처리
            Set<String> allowedTemplates = Set.of(
                    "invoice.ftl", "monthly-report.ftl", "system-notification.ftl",
                    "user-profile.ftl", "welcome-email.ftl"
            );

            if (!allowedTemplates.contains(templateName)) {
                return ResponseEntity.badRequest()
                        .body("허용되지 않는 템플릿: " + templateName);
            }

            String html = processTemplate(templateName, model);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("템플릿 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // ==== 헬퍼 메서드들 ====

    /**
     * FreeMarker 템플릿을 처리해서 HTML 문자열 반환
     */
    private String processTemplate(String templateName, Map<String, Object> model) throws Exception {
        Template template = freemarkerConfig.getTemplate(templateName);
        StringWriter stringWriter = new StringWriter();
        template.process(model, stringWriter);
        return stringWriter.toString();
    }

    /**
     * 송장 데이터 준비 (기존 로직)
     */
    private Map<String, Object> prepareInvoiceData() {
        Map<String, Object> model = new HashMap<>();

        // 회사 정보
        Map<String, Object> company = new HashMap<>();
        company.put("name", "테크솔루션 주식회사");
        company.put("address", "서울시 강남구 테헤란로 123, 456동 789호");
        company.put("phone", "02-1234-5678");
        company.put("email", "info@techsolution.co.kr");
        company.put("website", "www.techsolution.co.kr");
        company.put("businessNumber", "123-45-67890");

        // 고객 정보
        Map<String, Object> client = new HashMap<>();
        client.put("name", "ABC 주식회사");
        client.put("contactPerson", "김담당");
        client.put("address", "부산시 해운대구 센텀시티로 100");
        client.put("phone", "051-9876-5432");
        client.put("email", "contact@abc.co.kr");
        client.put("businessNumber", "987-65-43210");

        // 항목 리스트
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("name", "웹사이트 개발");
        item1.put("description", "반응형 웹사이트 개발 및 유지보수");
        item1.put("quantity", 1);
        item1.put("rate", 5000000);
        item1.put("itemTotal", 1 * 5000000);
        items.add(item1);

        // ... 더 많은 아이템들 (기존과 동일)

        // 금액 계산
        int subtotal = items.stream()
                .mapToInt(item -> (Integer) item.get("itemTotal"))
                .sum();

        int discount = 200000;
        String discountType = "금액";
        int discountAmount = discount;
        int finalSubtotal = subtotal - discountAmount;
        int taxRate = 10;
        int taxAmount = finalSubtotal * taxRate / 100;
        int totalAmount = finalSubtotal + taxAmount;

        // 모델 데이터 설정
        model.put("documentType", "세금계산서");
        model.put("invoiceNumber", "INV-2025-001");
        model.put("issueDate", java.sql.Date.valueOf(LocalDate.now()));
        model.put("dueDate", java.sql.Date.valueOf(LocalDate.now().plusDays(30)));
        model.put("company", company);
        model.put("client", client);
        model.put("items", items);
        model.put("subtotal", subtotal);
        model.put("discount", discount);
        model.put("discountType", discountType);
        model.put("discountAmount", discountAmount);
        model.put("finalSubtotal", finalSubtotal);
        model.put("taxRate", taxRate);
        model.put("taxAmount", taxAmount);
        model.put("totalAmount", totalAmount);

        return model;
    }

    /**
     * 금액 재계산 (동적 아이템 변경 시)
     */
    private void recalculateInvoiceAmounts(Map<String, Object> model, List<Map<String, Object>> items) {
        int subtotal = items.stream()
                .mapToInt(item -> {
                    int quantity = (Integer) item.get("quantity");
                    int rate = (Integer) item.get("rate");
                    int itemTotal = quantity * rate;
                    item.put("itemTotal", itemTotal);
                    return itemTotal;
                })
                .sum();

        int discount = (Integer) model.getOrDefault("discount", 0);
        int discountAmount = discount;
        int finalSubtotal = subtotal - discountAmount;
        int taxRate = (Integer) model.getOrDefault("taxRate", 10);
        int taxAmount = finalSubtotal * taxRate / 100;
        int totalAmount = finalSubtotal + taxAmount;

        model.put("subtotal", subtotal);
        model.put("discountAmount", discountAmount);
        model.put("finalSubtotal", finalSubtotal);
        model.put("taxAmount", taxAmount);
        model.put("totalAmount", totalAmount);
    }

    // 다른 데이터 준비 메서드들 (월간보고서, 시스템알림 등)
    private Map<String, Object> prepareMonthlyReportData() {
        // 기존 monthlyReport 메서드의 데이터 준비 로직
        Map<String, Object> model = new HashMap<>();
        // ... (기존 코드와 동일)
        return model;
    }

    private Map<String, Object> prepareSystemNotificationData() {
        // 기존 systemNotification 메서드의 데이터 준비 로직
        Map<String, Object> model = new HashMap<>();
        // ... (기존 코드와 동일)
        return model;
    }

    private Map<String, Object> prepareUserProfileData() {
        // 기존 userProfile 메서드의 데이터 준비 로직
        Map<String, Object> model = new HashMap<>();
        // ... (기존 코드와 동일)
        return model;
    }

    private Map<String, Object> prepareWelcomeEmailData() {
        // 기존 welcomeEmail 메서드의 데이터 준비 로직
        Map<String, Object> model = new HashMap<>();
        // ... (기존 코드와 동일)
        return model;
    }
}