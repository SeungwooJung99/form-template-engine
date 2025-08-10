# FreeMarker 템플릿 작성 가이드라인

## 📋 개요

동적으로 생성되는 FreeMarker 템플릿에서 변수 누락으로 인한 렌더링 오류를 방지하고, 안정적인 템플릿을 작성하기 위한 가이드라인입니다.

## 🔥 핵심 원칙: **모든 변수는 기본값을 가져야 한다**

### 1. 기본 변수 사용 패턴

#### ✅ 올바른 사용법

```freemarker
<!-- 기본값과 함께 사용 -->
${변수명!"기본값"}

<!-- 조건부 렌더링 -->
<#if 변수명??>내용</#if>

<!-- 복합 조건 -->
<#if 변수명?? && 변수명?has_content>내용</#if>
```

#### ❌ 잘못된 사용법

```freemarker
<!-- 기본값 없이 사용 - 오류 발생 가능 -->
${변수명}

<!-- null 체크 없이 객체 접근 -->
${객체.속성}
```

### 2. 데이터 타입별 처리 가이드

#### 📝 문자열 변수

```freemarker
<!-- 기본 텍스트 -->
${title!"기본 제목"}
${description!"설명이 없습니다"}

<!-- 공백 문자열 허용하지 않는 경우 -->
<#assign displayTitle = (title?has_content)?then(title, "기본 제목")>
```

#### 🔢 숫자 변수

```freemarker
<!-- 안전한 숫자 매크로 정의 (템플릿 상단에 배치) -->
<#macro safeNumber value defaultValue=0>
    <#if value??>
        <#if value?is_number>
            ${value?string("#,##0")}
        <#elseif value?is_string>
            <#attempt>
                ${value?replace(",", "")?number?string("#,##0")}
            <#recover>
                ${defaultValue?string("#,##0")}
            </#attempt>
        <#else>
            ${defaultValue?string("#,##0")}
        </#if>
    <#else>
        ${defaultValue?string("#,##0")}
    </#if>
</#macro>

<!-- 사용 예시 -->
<@safeNumber amount 0 />
```

#### 📅 날짜 변수

```freemarker
<!-- 날짜 표시 -->
<#if date??>
    <#if date?is_date>
        ${date?string("yyyy-MM-dd")}
    <#else>
        ${date?string}
    </#if>
<#else>
    ${.now?string("yyyy-MM-dd")}
</#if>
```

#### ✅ Boolean 변수

```freemarker
<!-- Boolean 체크 -->
<#if (isActive!false)>활성 상태<#else>비활성 상태</#if>

<!-- 조건부 클래스 적용 -->
<div class="status ${(isActive!false)?then('active', 'inactive')}">
```

### 3. 객체 및 컬렉션 처리

#### 🏢 객체 변수

```freemarker
<!-- 안전한 객체 접근 -->
<#if company?? && company?is_hash>
    <h2>${company.name!"회사명 없음"}</h2>
    <p>${company.address!"주소 정보 없음"}</p>
    <p>전화: ${company.phone!"연락처 없음"}</p>
<#else>
    <h2>회사 정보가 없습니다</h2>
</#if>
```

#### 📋 배열/리스트 변수

```freemarker
<!-- 리스트 안전 처리 -->
<#if items?? && items?has_content>
    <#list items as item>
        <tr>
            <td>
                <#if item?? && item?is_hash>
                    ${item.name!"항목명 없음"}
                <#else>
                    ${item!"항목 정보 없음"}
                </#if>
            </td>
        </tr>
    </#list>
<#else>
    <tr>
        <td colspan="4" style="text-align: center;">
            항목이 없습니다.
        </td>
    </tr>
</#if>
```

### 4. 계산 및 수식 처리

#### 🧮 안전한 계산

```freemarker
<!-- 계산용 함수 정의 -->
<#function safeAdd a b defaultA=0 defaultB=0>
    <#assign numA = (a?is_number)?then(a, (a?is_string)?then(a?number, defaultA))>
    <#assign numB = (b?is_number)?then(b, (b?is_string)?then(b?number, defaultB))>
    <#return numA + numB>
</#function>

<!-- 사용 예시 -->
<#assign subtotal = 0>
<#if items?? && items?has_content>
    <#list items as item>
        <#assign quantity = (item.quantity!0)?is_number?then(item.quantity!0, 0)>
        <#assign rate = (item.rate!0)?is_number?then(item.rate!0, 0)>
        <#assign itemTotal = quantity * rate>
        <#assign subtotal = subtotal + itemTotal>
    </#list>
</#if>
```

### 5. 템플릿 구조 권장 사항

#### 📁 템플릿 기본 구조

```freemarker
<!DOCTYPE html>
<html>
<head>
    <!-- 1. 메타 정보 -->
    <meta charset="UTF-8">
    <title>${pageTitle!"기본 제목"}</title>
    
    <!-- 2. 스타일 정의 -->
    <style>
        /* CSS 스타일 */
    </style>
</head>
<body>
    <!-- 3. 매크로 및 함수 정의 영역 -->
    <#-- 안전한 숫자 포맷팅 매크로 -->
    <#macro formatNumber value defaultValue=0>
        <!-- 매크로 내용 -->
    </#macro>
    
    <!-- 4. 변수 전처리 영역 -->
    <#assign displayTitle = title!"기본 제목">
    <#assign currentDate = .now>
    
    <!-- 5. 메인 콘텐츠 -->
    <div class="container">
        <!-- 템플릿 내용 -->
    </div>
    
    <!-- 6. JavaScript (필요한 경우) -->
    <script>
        // JavaScript 코드
    </script>
</body>
</html>
```

### 6. 일반적인 변수명과 추천 기본값

#### 📊 비즈니스 관련

```freemarker
${companyName!"회사명"}
${documentType!"문서"}
${documentNumber!"DOC-${.now?string('yyyyMMdd')}-001"}
${issueDate!.now?string("yyyy-MM-dd")}
${dueDate!(.now?date?long + 30*24*60*60*1000)?number_to_date?string("yyyy-MM-dd")}

<!-- 금액 관련 -->
${subtotal!0}
${taxRate!10}
${discountRate!0}
${totalAmount!0}
```

#### 👥 사용자 관련

```freemarker
${userName!"사용자"}
${userEmail!"user@example.com"}
${currentUser!"게스트"}
```

#### 📋 리스트 관련

```freemarker
<#assign defaultItems = [
    {"name": "샘플 항목", "quantity": 1, "rate": 0}
]>

<#assign displayItems = items!defaultItems>
```

### 7. 에러 처리 패턴

#### 🚨 예외 처리

```freemarker
<!-- 시도-복구 패턴 -->
<#attempt>
    ${복잡한계산식}
<#recover>
    기본값 또는 오류 메시지
</#attempt>

<!-- 조건부 렌더링으로 에러 방지 -->
<#if 조건?? && 조건?is_sequence && 조건?has_content>
    <!-- 안전한 처리 -->
<#else>
    <!-- 기본값 또는 대체 콘텐츠 -->
</#if>
```

### 8. 성능 최적화

#### ⚡ 변수 캐싱

```freemarker
<!-- 반복 사용되는 계산은 변수에 저장 -->
<#assign formattedDate = (.now?string("yyyy-MM-dd HH:mm:ss"))!>
<#assign companyInfo = (company?? && company?is_hash)?then(company, {"name": "기본 회사명"})>
```

### 9. 디버깅 도구

#### 🔍 개발 모드 정보

```freemarker
<!-- 개발 모드일 때만 표시 -->
<#if debugMode!false>
    <div style="background: #f0f0f0; padding: 10px; margin: 10px 0;">
        <h3>디버그 정보</h3>
        <p>템플릿: ${.current_template_name!"unknown"}</p>
        <p>변수 개수: ${.data_model?keys?size}</p>
        <p>생성 시간: ${.now?string("yyyy-MM-dd HH:mm:ss")}</p>
    </div>
</#if>
```

### 10. 체크리스트

템플릿 작성 후 다음 항목들을 확인하세요:

- [ ] 모든 `${변수}`에 기본값 `!"기본값"` 설정
- [ ] 객체 접근 전 `??` 및 `?is_hash` 체크
- [ ] 배열 처리 전 `?has_content` 체크
- [ ] 숫자 계산에 안전한 함수/매크로 사용
- [ ] 날짜 변수에 타입 체크 및 포맷팅
- [ ] 복잡한 로직에 `<#attempt>` 사용
- [ ] 에러 상황에 대한 대체 콘텐츠 제공

## 🚀 권장 도구 및 매크로

### 범용 유틸리티 매크로

```freemarker
<#-- 안전한 문자열 출력 -->
<#macro safeString value default="">
    ${(value?has_content)?then(value, default)}
</#macro>

<#-- 안전한 숫자 출력 -->
<#macro safeNumber value default=0 format="#,##0">
    <#if value?? && value?is_number>
        ${value?string(format)}
    <#elseif value?? && value?is_string>
        <#attempt>
            ${value?number?string(format)}
        <#recover>
            ${default?string(format)}
        </#attempt>
    <#else>
        ${default?string(format)}
    </#if>
</#macro>

<#-- 조건부 CSS 클래스 -->
<#macro conditionalClass condition trueClass falseClass="">
    <#if condition!false>${trueClass}<#else>${falseClass}</#if>
</#macro>
```

---

**이 가이드라인을 따르면 변수 누락으로 인한 템플릿 렌더링 오류를 크게 줄일 수 있습니다.**