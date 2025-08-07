package com.boxwood.form.engine.test;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
public class TemplateController {

    // 메인 페이지
    @GetMapping("/")
    public String index(Model model) {
        List<Map<String, String>> templates = new ArrayList<>();

        Map<String, String> template1 = new HashMap<>();
        template1.put("name", "송장/세금계산서");
        template1.put("url", "/invoice");
        template1.put("template", "invoice.ftl");
        templates.add(template1);

        Map<String, String> template2 = new HashMap<>();
        template2.put("name", "월간 보고서");
        template2.put("url", "/monthly-report");
        template2.put("template", "monthly-report.ftl");
        templates.add(template2);

        Map<String, String> template3 = new HashMap<>();
        template3.put("name", "시스템 알림");
        template3.put("url", "/system-notification");
        template3.put("template", "system-notification.ftl");
        templates.add(template3);

        Map<String, String> template4 = new HashMap<>();
        template4.put("name", "사용자 프로필");
        template4.put("url", "/user-profile");
        template4.put("template", "user-profile.ftl");
        templates.add(template4);

        Map<String, String> template5 = new HashMap<>();
        template5.put("name", "환영 이메일");
        template5.put("url", "/welcome-email");
        template5.put("template", "welcome-email.ftl");
        templates.add(template5);

        model.addAttribute("templates", templates);

        // 디버깅용 로그 추가
        System.out.println("Index controller called - templates size: " + templates.size());

        return "index";
    }

    // 송장/청구서 페이지 - 계산을 컨트롤러에서 완료
    @GetMapping("/invoice")
    public String invoice(Model model) {
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

        // 항목 리스트 - 각 항목별 소계 미리 계산
        List<Map<String, Object>> items = new ArrayList<>();

        Map<String, Object> item1 = new HashMap<>();
        item1.put("name", "웹사이트 개발");
        item1.put("description", "반응형 웹사이트 개발 및 유지보수");
        item1.put("quantity", 1);
        item1.put("rate", 5000000);
        item1.put("itemTotal", 1 * 5000000); // 항목별 소계 미리 계산
        items.add(item1);

        Map<String, Object> item2 = new HashMap<>();
        item2.put("name", "SEO 최적화");
        item2.put("description", "검색엔진 최적화 서비스");
        item2.put("quantity", 3);
        item2.put("rate", 300000);
        item2.put("itemTotal", 3 * 300000); // 항목별 소계 미리 계산
        items.add(item2);

        Map<String, Object> item3 = new HashMap<>();
        item3.put("name", "기술 지원");
        item3.put("description", "3개월 기술지원 서비스");
        item3.put("quantity", 1);
        item3.put("rate", 1200000);
        item3.put("itemTotal", 1 * 1200000); // 항목별 소계 미리 계산
        items.add(item3);

        // 전체 소계 계산
        int subtotal = 0;
        for (Map<String, Object> item : items) {
            subtotal += (Integer) item.get("itemTotal");
        }

        // 할인 계산
        int discount = 200000;
        String discountType = "금액"; // "금액" 또는 "비율"
        int discountAmount;

        if ("비율".equals(discountType)) {
            discountAmount = subtotal * discount / 100;
        } else {
            discountAmount = discount;
        }

        // 할인 후 금액
        int finalSubtotal = subtotal - discountAmount;

        // 세금 계산
        int taxRate = 10;
        int taxAmount = finalSubtotal * taxRate / 100;

        // 최종 총액
        int totalAmount = finalSubtotal + taxAmount;

        // 모델에 계산된 값들 추가
        model.addAttribute("documentType", "세금계산서");
        model.addAttribute("invoiceNumber", "INV-2025-001");
        model.addAttribute("issueDate", java.sql.Date.valueOf(LocalDate.now()));
        model.addAttribute("dueDate", java.sql.Date.valueOf(LocalDate.now().plusDays(30)));
        model.addAttribute("company", company);
        model.addAttribute("client", client);
        model.addAttribute("salesperson", "이영업");
        model.addAttribute("projectName", "ABC 기업 웹사이트 리뉴얼");
        model.addAttribute("referenceNumber", "REF-2025-001");
        model.addAttribute("poNumber", "PO-ABC-001");
        model.addAttribute("paymentTerms", "30일 이내");
        model.addAttribute("items", items);

        // 계산된 금액들
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("discount", discount);
        model.addAttribute("discountType", discountType);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("finalSubtotal", finalSubtotal);
        model.addAttribute("taxRate", taxRate);
        model.addAttribute("taxAmount", taxAmount);
        model.addAttribute("totalAmount", totalAmount);

        model.addAttribute("paymentInfo", "은행: 국민은행<br>계좌번호: 123-456789-12-345<br>예금주: 테크솔루션 주식회사");
        model.addAttribute("notes", "결제 기한을 준수해 주시기 바랍니다.<br>문의사항은 언제든 연락주세요.");

        return "invoice";
    }

    // 월간 보고서 페이지
    @GetMapping("/monthly-report")
    public String monthlyReport(Model model) {
        // KPI 데이터
        List<Map<String, Object>> kpis = new ArrayList<>();

        Map<String, Object> kpi1 = new HashMap<>();
        kpi1.put("name", "매출액");
        kpi1.put("value", "1,250");
        kpi1.put("unit", "만원");
        kpi1.put("change", "+15%");
        kpis.add(kpi1);

        Map<String, Object> kpi2 = new HashMap<>();
        kpi2.put("name", "신규 고객");
        kpi2.put("value", "23");
        kpi2.put("unit", "명");
        kpi2.put("change", "+8%");
        kpis.add(kpi2);

        Map<String, Object> kpi3 = new HashMap<>();
        kpi3.put("name", "프로젝트 완료율");
        kpi3.put("value", "89");
        kpi3.put("unit", "%");
        kpi3.put("change", "-3%");
        kpis.add(kpi3);

        Map<String, Object> kpi4 = new HashMap<>();
        kpi4.put("name", "고객 만족도");
        kpi4.put("value", "4.8");
        kpi4.put("unit", "/5");
        kpi4.put("change", "+0.2");
        kpis.add(kpi4);

        // 주요 성과
        List<Map<String, Object>> achievements = new ArrayList<>();

        Map<String, Object> achievement1 = new HashMap<>();
        achievement1.put("title", "대형 프로젝트 수주");
        achievement1.put("description", "XYZ 그룹의 ERP 시스템 구축 프로젝트를 성공적으로 수주했습니다.");
        achievement1.put("metrics", "계약금액: 5억원, 기간: 12개월");
        achievements.add(achievement1);

        Map<String, Object> achievement2 = new HashMap<>();
        achievement2.put("title", "신기술 도입 완료");
        achievement2.put("description", "AI 기반 데이터 분석 시스템을 성공적으로 도입했습니다.");
        achievement2.put("metrics", "효율성 30% 증대");
        achievements.add(achievement2);

        Map<String, Object> achievement3 = new HashMap<>();
        achievement3.put("title", "팀 확장 완료");
        achievement3.put("description", "개발팀에 시니어 개발자 3명을 새롭게 영입했습니다.");
        achievement3.put("metrics", "개발 역량 40% 향상");
        achievements.add(achievement3);

        // 프로젝트 현황
        List<Map<String, Object>> projects = new ArrayList<>();

        Map<String, Object> project1 = new HashMap<>();
        project1.put("name", "모바일 앱 개발");
        project1.put("manager", "김개발");
        project1.put("progress", 75);
        project1.put("status", "progress");
        project1.put("dueDate", "2025-09-15");
        projects.add(project1);

        Map<String, Object> project2 = new HashMap<>();
        project2.put("name", "데이터베이스 마이그레이션");
        project2.put("manager", "박디비");
        project2.put("progress", 100);
        project2.put("status", "complete");
        project2.put("dueDate", "2025-07-30");
        projects.add(project2);

        Map<String, Object> project3 = new HashMap<>();
        project3.put("name", "ERP 시스템 구축");
        project3.put("manager", "이시스템");
        project3.put("progress", 45);
        project3.put("status", "progress");
        project3.put("dueDate", "2025-12-31");
        projects.add(project3);

        Map<String, Object> project4 = new HashMap<>();
        project4.put("name", "클라우드 인프라 구축");
        project4.put("manager", "최클라우드");
        project4.put("progress", 20);
        project4.put("status", "pending");
        project4.put("dueDate", "2025-11-15");
        projects.add(project4);

        // 이슈 및 개선사항
        List<Map<String, Object>> issues = new ArrayList<>();

        Map<String, Object> issue1 = new HashMap<>();
        issue1.put("title", "서버 성능 저하");
        issue1.put("description", "트래픽 증가로 인한 응답 시간 지연 발생");
        issue1.put("action", "서버 증설 및 로드밸런싱 구축 예정");
        issues.add(issue1);

        Map<String, Object> issue2 = new HashMap<>();
        issue2.put("title", "인력 부족");
        issue2.put("description", "프로젝트 증가 대비 개발 인력 부족 상황");
        issue2.put("action", "시니어 개발자 2명 추가 채용 진행 중");
        issues.add(issue2);

        // 다음 달 계획
        List<Map<String, Object>> nextMonthPlans = new ArrayList<>();

        Map<String, Object> plan1 = new HashMap<>();
        plan1.put("title", "새로운 팀원 채용");
        plan1.put("description", "백엔드 개발자 2명, 프론트엔드 개발자 1명 채용");
        plan1.put("deadline", "2025-09-30");
        nextMonthPlans.add(plan1);

        Map<String, Object> plan2 = new HashMap<>();
        plan2.put("title", "신제품 출시 준비");
        plan2.put("description", "AI 기반 분석 도구 베타 버전 출시");
        plan2.put("deadline", "2025-09-15");
        nextMonthPlans.add(plan2);

        Map<String, Object> plan3 = new HashMap<>();
        plan3.put("title", "고객 만족도 조사");
        plan3.put("description", "기존 고객들을 대상으로 서비스 만족도 조사 실시");
        plan3.put("deadline", "2025-09-20");
        nextMonthPlans.add(plan3);

        model.addAttribute("reportYear", 2025);
        model.addAttribute("reportMonth", 8);
        model.addAttribute("departmentName", "개발팀");
        model.addAttribute("reporterName", "이팀장");
        model.addAttribute("companyName", "테크솔루션");
        model.addAttribute("kpis", kpis);
        model.addAttribute("achievements", achievements);
        model.addAttribute("projects", projects);
        model.addAttribute("issues", issues);
        model.addAttribute("nextMonthPlans", nextMonthPlans);

        return "monthly-report";
    }

    // 시스템 알림 페이지
    @GetMapping("/system-notification")
    public String systemNotification(Model model) {
        List<String> welcomeBenefits = Arrays.asList(
                "첫 달 서비스 이용료 50% 할인",
                "전담 고객지원 서비스 제공",
                "무료 기술 컨설팅 1회",
                "프리미엄 템플릿 무제한 사용",
                "24시간 기술 지원 서비스",
                "무료 교육 프로그램 참여 기회"
        );

        model.addAttribute("companyName", "테크솔루션");
        model.addAttribute("userName", "홍길동");
        model.addAttribute("welcomeBenefits", welcomeBenefits);
        model.addAttribute("userEmail", "hong@techsolution.co.kr");
        model.addAttribute("registrationDate", LocalDateTime.now());
        model.addAttribute("membershipLevel", "프리미엄");
        model.addAttribute("department", "개발팀");
        model.addAttribute("loginUrl", "https://app.techsolution.co.kr/login");
        model.addAttribute("supportEmail", "support@techsolution.co.kr");

        return "system-notification";
    }

    // 사용자 프로필 페이지
    @GetMapping("/user-profile")
    public String userProfile(Model model) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", "김개발자");
        user.put("position", "시니어 개발자");
        user.put("department", "개발팀");
        user.put("employeeId", "EMP-2023-001");
        user.put("hireDate", LocalDate.of(2023, 3, 15));
        user.put("team", "백엔드팀");
        user.put("location", "서울 본사");
        user.put("manager", "이팀장");
        user.put("email", "kim.dev@techsolution.co.kr");
        user.put("phone", "010-1234-5678");
        user.put("extension", "1234");
        user.put("slackId", "kimdev");
        user.put("linkedIn", "https://linkedin.com/in/kimdev");
        user.put("bio", "10년 경력의 백엔드 개발자입니다. Java/Spring 전문가이며, 마이크로서비스 아키텍처 설계 경험이 풍부합니다. 새로운 기술 학습에 열정적이며, 팀원들과의 협업을 중시합니다.");
        user.put("lastUpdated", LocalDate.now());

        // 스킬 리스트
        List<String> skills = Arrays.asList(
                "Java", "Spring Boot", "Spring Framework", "Microservices",
                "Docker", "Kubernetes", "MySQL", "PostgreSQL", "Redis",
                "AWS", "Git", "Jenkins", "REST API", "GraphQL"
        );
        user.put("skills", skills);

        // 프로젝트 리스트
        List<Map<String, Object>> projects = new ArrayList<>();

        Map<String, Object> project1 = new HashMap<>();
        project1.put("name", "ERP 시스템 개발");
        project1.put("description", "대기업 ERP 시스템 백엔드 API 개발 및 아키텍처 설계");
        project1.put("status", "active");
        projects.add(project1);

        Map<String, Object> project2 = new HashMap<>();
        project2.put("name", "레거시 시스템 마이그레이션");
        project2.put("description", "기존 모놀리틱 시스템을 마이크로서비스 아키텍처로 전환");
        project2.put("status", "completed");
        projects.add(project2);

        Map<String, Object> project3 = new HashMap<>();
        project3.put("name", "실시간 데이터 처리 시스템");
        project3.put("description", "대용량 실시간 데이터 처리를 위한 스트리밍 파이프라인 구축");
        project3.put("status", "active");
        projects.add(project3);

        user.put("projects", projects);

        // 성과 리스트
        List<String> achievements = Arrays.asList(
                "2024년 우수사원상", "기술혁신상", "AWS 솔루션 아키텍트 자격증",
                "Oracle Java 인증", "프로젝트 리더십 인증"
        );
        user.put("achievements", achievements);

        model.addAttribute("user", user);

        return "user-profile";
    }

    // 환영 이메일 페이지
    @GetMapping("/welcome-email")
    public String welcomeEmail(Model model) {
        List<String> welcomeBenefits = Arrays.asList(
                "신규 회원 20% 할인 쿠폰",
                "무료 온보딩 세션 1회",
                "프리미엄 기능 1개월 무료 체험",
                "전용 계정 매니저 배정",
                "무료 기술 상담 서비스",
                "커뮤니티 베타 테스터 자격"
        );

        model.addAttribute("companyName", "테크솔루션");
        model.addAttribute("userName", "박신입");
        model.addAttribute("welcomeBenefits", welcomeBenefits);
        model.addAttribute("userEmail", "park.new@techsolution.co.kr");
        model.addAttribute("registrationDate", LocalDateTime.now());
        model.addAttribute("membershipLevel", "스탠다드");
        model.addAttribute("department", "마케팅팀");
        model.addAttribute("loginUrl", "https://app.techsolution.co.kr/login");
        model.addAttribute("supportEmail", "support@techsolution.co.kr");

        return "welcome-email";
    }

}