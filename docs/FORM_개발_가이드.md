# FreeMarker Form 개발 완전 가이드

## 📋 목차

1. [개요](#개요)
2. [기본 원칙](#기본-원칙)
3. [Form 구조 설계](#form-구조-설계)
4. [변수 네이밍 규칙](#변수-네이밍-규칙)
5. [Input 요소별 구현 가이드](#input-요소별-구현-가이드)
6. [검증 및 오류 처리](#검증-및-오류-처리)
7. [보안 고려사항](#보안-고려사항)
8. [접근성 고려사항](#접근성-고려사항)
9. [실제 Form 예시](#실제-form-예시)
10. [안티패턴과 권장패턴](#안티패턴과-권장패턴)
11. [디버깅 및 테스트](#디버깅-및-테스트)
12. [성능 최적화](#성능-최적화)
13. [체크리스트](#체크리스트)

---

## 🎯 개요

이 가이드는 FreeMarker(.ftl) 템플릿을 사용하여 **안전하고 유지보수 가능한 Form**을 개발하기 위한 실무 가이드라인입니다.

### 가이드의 목적

- ✅ **일관된 Form 개발 표준** 제공
- ✅ **보안 취약점 방지**
- ✅ **접근성 준수**
- ✅ **변수 추출 최적화** (Enhanced Mock Environment 기준)
- ✅ **유지보수성 향상**

### 대상 독자

- FreeMarker 초보 개발자
- Form 개발 담당자
- 코드 리뷰어
- QA 엔지니어

---

## 🏗️ 기본 원칙

### 1. **안전 우선 (Safety First)**

```ftl
<!-- ✅ 항상 null 체크와 기본값 -->
<input type="text" name="username" value="${user.username!''}" />

<!-- ❌ 위험한 직접 접근 -->
<input type="text" name="username" value="${user.username}" />
```

### 2. **명확한 구조 (Clear Structure)**

```ftl
<!-- ✅ 계층적 변수 구조 -->
${form.user.personalInfo.name}
${form.user.contactInfo.email}

<!-- ❌ 평면적 구조 -->
${userName}
${userEmail}
```

### 3. **검증 가능성 (Validatable)**

```ftl
<!-- ✅ 검증 정보를 포함한 구조 -->
<#if form.errors?? && form.errors.username??>
    <div class="error">${form.errors.username}</div>
</#if>
```

### 4. **재사용성 (Reusability)**

```ftl
<!-- ✅ 매크로를 활용한 재사용 -->
<@inputField name="username" label="사용자명" required=true />
```

---

## 🏢 Form 구조 설계

### 권장 Form 데이터 구조

```ftl
<#-- 
Form 데이터 모델 구조:
form: {
    data: {           // 실제 form 데이터
        user: {
            personalInfo: {name, birthDate, gender},
            contactInfo: {email, phone, address},
            preferences: {language, theme, notifications}
        }
    },
    meta: {           // Form 메타 정보
        action: "create|update|view",
        csrf: "token_value",
        readonly: boolean
    },
    validation: {     // 검증 관련
        errors: {fieldName: "error message"},
        rules: {fieldName: {required: true, maxLength: 50}},
        touched: {fieldName: boolean}
    },
    ui: {            // UI 상태
        collapsed: {sectionName: boolean},
        loading: boolean,
        step: number
    }
}
-->
```

### Form 기본 템플릿 구조

```ftl
<#-- Form 헤더: 변수 정의 -->
<#-- @variables: form.data, form.meta, form.validation, form.ui -->

<#-- 매크로 import -->
<#include "common/form-macros.ftl">

<#-- Form 시작 -->
<form id="${form.meta.formId!'defaultForm'}" 
      action="${form.meta.action!'/submit'}" 
      method="POST"
      <#if form.meta.enctype??>enctype="${form.meta.enctype}"</#if>>
    
    <#-- CSRF Token -->
    <#if form.meta.csrf??>
        <input type="hidden" name="_csrf" value="${form.meta.csrf}" />
    </#if>
    
    <#-- Form Sections -->
    <@formSection title="개인정보" name="personalInfo">
        <!-- 개인정보 필드들 -->
    </@formSection>
    
    <@formSection title="연락처" name="contactInfo">
        <!-- 연락처 필드들 -->
    </@formSection>
    
    <#-- Form Actions -->
    <@formActions />
    
</form>
```

---

## 📝 변수 네이밍 규칙

### 1. **계층적 네이밍**

```ftl
<!-- ✅ 권장: 명확한 계층 구조 -->
${form.data.user.personalInfo.name}
${form.data.user.contactInfo.email}
${form.data.order.billing.address}
${form.data.order.shipping.address}

<!-- ❌ 비권장: 평면적 구조 -->
${userName}
${userEmail}  
${billingAddress}
${shippingAddress}
```

### 2. **의미있는 이름**

```ftl
<!-- ✅ 명확한 의미 -->
${form.validation.errors.email}
${form.ui.collapsed.personalInfo}
${form.data.preferences.notifications.email}

<!-- ❌ 애매한 의미 -->
${error}
${collapsed}
${emailFlag}
```

### 3. **일관된 패턴**

```ftl
<!-- ✅ 일관된 패턴 -->
${form.data.user.*}           <!-- 모든 사용자 데이터 -->
${form.validation.errors.*}   <!-- 모든 검증 오류 -->
${form.ui.loading.*}          <!-- 모든 로딩 상태 -->

<!-- ❌ 일관성 없는 패턴 -->
${userData}
${validationError}
${isLoading}
```

---

## 🎛️ Input 요소별 구현 가이드

### 1. **Text Input**

#### 기본 구현

```ftl
<#macro textInput name label value="" placeholder="" required=false readonly=false maxLength=255>
    <div class="form-group">
        <label for="${name}" class="form-label">
            ${label}
            <#if required><span class="required">*</span></#if>
        </label>
        
        <input type="text" 
               id="${name}" 
               name="${name}"
               value="${value}"
               <#if placeholder?has_content>placeholder="${placeholder}"</#if>
               <#if required>required aria-required="true"</#if>
               <#if readonly>readonly</#if>
               <#if maxLength gt 0>maxlength="${maxLength}"</#if>
               class="form-control <#if form.validation.errors[name]??>is-invalid</#if>"
               aria-describedby="${name}-help ${name}-error" />
        
        <#if form.validation.errors[name]??>
            <div id="${name}-error" class="invalid-feedback" role="alert">
                ${form.validation.errors[name]}
            </div>
        </#if>
        
        <#if form.ui.help[name]??>
            <div id="${name}-help" class="form-text">
                ${form.ui.help[name]}
            </div>
        </#if>
    </div>
</#macro>

<!-- 사용 예시 -->
<@textInput name="username" 
            label="사용자명" 
            value="${form.data.user.username!''}"
            placeholder="사용자명을 입력하세요"
            required=true 
            maxLength=50 />
```

### 2. **Email Input**

```ftl
<#macro emailInput name label value="" required=false>
    <div class="form-group">
        <label for="${name}" class="form-label">
            ${label}
            <#if required><span class="required">*</span></#if>
        </label>
        
        <input type="email" 
               id="${name}" 
               name="${name}"
               value="${value}"
               <#if required>required aria-required="true"</#if>
               pattern="[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$"
               class="form-control <#if form.validation.errors[name]??>is-invalid</#if>"
               aria-describedby="${name}-error" />
        
        <#-- 오류 표시 -->
        <#if form.validation.errors[name]??>
            <div id="${name}-error" class="invalid-feedback" role="alert">
                ${form.validation.errors[name]}
            </div>
        </#if>
    </div>
</#macro>

<!-- 사용 예시 -->
<@emailInput name="email" 
             label="이메일 주소" 
             value="${form.data.user.contactInfo.email!''}"
             required=true />
```

### 3. **Password Input**

```ftl
<#macro passwordInput name label required=false showToggle=true minLength=8>
    <div class="form-group">
        <label for="${name}" class="form-label">
            ${label}
            <#if required><span class="required">*</span></#if>
        </label>
        
        <div class="password-input-wrapper">
            <input type="password" 
                   id="${name}" 
                   name="${name}"
                   <#if required>required aria-required="true"</#if>
                   <#if minLength gt 0>minlength="${minLength}"</#if>
                   class="form-control <#if form.validation.errors[name]??>is-invalid</#if>"
                   aria-describedby="${name}-error ${name}-help" />
            
            <#if showToggle>
                <button type="button" class="password-toggle" 
                        aria-label="비밀번호 보기/숨기기"
                        onclick="togglePassword('${name}')">
                    <i class="eye-icon"></i>
                </button>
            </#if>
        </div>
        
        <#if minLength gt 0>
            <div id="${name}-help" class="form-text">
                최소 ${minLength}자 이상 입력하세요.
            </div>
        </#if>
        
        <#if form.validation.errors[name]??>
            <div id="${name}-error" class="invalid-feedback" role="alert">
                ${form.validation.errors[name]}
            </div>
        </#if>
    </div>
</#macro>

<!-- 사용 예시 -->
<@passwordInput name="password" 
                label="비밀번호" 
                required=true 
                minLength=8 />
```

### 4. **Select (Dropdown)**

```ftl
<#macro selectInput name label options=[] value="" required=false multiple=false>
    <div class="form-group">
        <label for="${name}" class="form-label">
            ${label}
            <#if required><span class="required">*</span></#if>
        </label>
        
        <select id="${name}" 
                name="${name}"
                <#if required>required aria-required="true"</#if>
                <#if multiple>multiple</#if>
                class="form-select <#if form.validation.errors[name]??>is-invalid</#if>"
                aria-describedby="${name}-error">
            
            <#if !required && !multiple>
                <option value="">선택하세요</option>
            </#if>
            
            <#list options as option>
                <option value="${option.value!''}"
                        <#if option.value == value || (multiple && value?seq_contains(option.value))>selected</#if>
                        <#if option.disabled!false>disabled</#if>>
                    ${option.label!''}
                </option>
            </#list>
        </select>
        
        <#if form.validation.errors[name]??>
            <div id="${name}-error" class="invalid-feedback" role="alert">
                ${form.validation.errors[name]}
            </div>
        </#if>
    </div>
</#macro>

<!-- 사용 예시 -->
<@selectInput name="country" 
              label="국가" 
              options=form.data.options.countries
              value="${form.data.user.address.country!''}"
              required=true />
```

### 5. **Radio Button Group**

```ftl
<#macro radioGroup name label options=[] value="" required=false inline=false>
    <fieldset class="form-group">
        <legend class="form-label">
            ${label}
            <#if required><span class="required">*</span></#if>
        </legend>
        
        <div class="radio-group <#if inline>radio-inline</#if>" 
             role="radiogroup"
             aria-describedby="${name}-error">
            
            <#list options as option>
                <div class="form-check <#if inline>form-check-inline</#if>">
                    <input type="radio" 
                           id="${name}-${option.value!''}" 
                           name="${name}"
                           value="${option.value!''}"
                           <#if option.value == value>checked</#if>
                           <#if required && option?index == 0>required aria-required="true"</#if>
                           class="form-check-input <#if form.validation.errors[name]??>is-invalid</#if>" />
                    
                    <label for="${name}-${option.value!''}" class="form-check-label">
                        ${option.label!''}
                    </label>
                </div>
            </#list>
        </div>
        
        <#if form.validation.errors[name]??>
            <div id="${name}-error" class="invalid-feedback" role="alert">
                ${form.validation.errors[name]}
            </div>
        </#if>
    </fieldset>
</#macro>

<!-- 사용 예시 -->
<@radioGroup name="gender" 
             label="성별" 
             options=form.data.options.genders
             value="${form.data.user.personalInfo.gender!''}"
             required=true 
             inline=true />
```

### 6. **Checkbox**

```ftl
<#macro checkboxInput name label checked=false value="true" required=false>
    <div class="form-group">
        <div class="form-check">
            <input type="checkbox" 
                   id="${name}" 
                   name="${name}"
                   value="${value}"
                   <#if checked>checked</#if>
                   <#if required>required aria-required="true"</#if>
                   class="form-check-input <#if form.validation.errors[name]??>is-invalid</#if>"
                   aria-describedby="${name}-error" />
            
            <label for="${name}" class="form-check-label">
                ${label}
                <#if required><span class="required">*</span></#if>
            </label>
        </div>
        
        <#if form.validation.errors[name]??>
            <div id="${name}-error" class="invalid-feedback" role="alert">
                ${form.validation.errors[name]}
            </div>
        </#if>
    </div>
</#macro>

<!-- 사용 예시 -->
<@checkboxInput name="agreeTerms" 
                label="이용약관에 동의합니다" 
                checked=(form.data.user.agreements.terms!false)
                required=true />
```

### 7. **Textarea**

```ftl
<#macro textareaInput name label value="" placeholder="" required=false rows=3 maxLength=1000>
    <div class="form-group">
        <label for="${name}" class="form-label">
            ${label}
            <#if required><span class="required">*</span></#if>
        </label>
        
        <textarea id="${name}" 
                  name="${name}"
                  rows="${rows}"
                  <#if placeholder?has_content>placeholder="${placeholder}"</#if>
                  <#if required>required aria-required="true"</#if>
                  <#if maxLength gt 0>maxlength="${maxLength}"</#if>
                  class="form-control <#if form.validation.errors[name]??>is-invalid</#if>"
                  aria-describedby="${name}-error ${name}-counter">${value}</textarea>
        
        <#if maxLength gt 0>
            <div id="${name}-counter" class="form-text text-end">
                <span class="char-count">0</span>/${maxLength}
            </div>
        </#if>
        
        <#if form.validation.errors[name]??>
            <div id="${name}-error" class="invalid-feedback" role="alert">
                ${form.validation.errors[name]}
            </div>
        </#if>
    </div>
</#macro>

<!-- 사용 예시 -->
<@textareaInput name="description" 
                label="상세 설명" 
                value="${form.data.product.description!''}"
                placeholder="제품에 대한 상세한 설명을 입력하세요"
                rows=5 
                maxLength=500 />
```

### 8. **File Upload**

```ftl
<#macro fileInput name label accept="" multiple=false required=false maxSize="">
    <div class="form-group">
        <label for="${name}" class="form-label">
            ${label}
            <#if required><span class="required">*</span></#if>
        </label>
        
        <input type="file" 
               id="${name}" 
               name="${name}"
               <#if accept?has_content>accept="${accept}"</#if>
               <#if multiple>multiple</#if>
               <#if required>required aria-required="true"</#if>
               <#if maxSize?has_content>data-max-size="${maxSize}"</#if>
               class="form-control <#if form.validation.errors[name]??>is-invalid</#if>"
               aria-describedby="${name}-error ${name}-help" />
        
        <#if accept?has_content || maxSize?has_content>
            <div id="${name}-help" class="form-text">
                <#if accept?has_content>허용 파일: ${accept}</#if>
                <#if maxSize?has_content>최대 크기: ${maxSize}</#if>
            </div>
        </#if>
        
        <#if form.validation.errors[name]??>
            <div id="${name}-error" class="invalid-feedback" role="alert">
                ${form.validation.errors[name]}
            </div>
        </#if>
    </div>
</#macro>

<!-- 사용 예시 -->
<@fileInput name="profileImage" 
            label="프로필 이미지" 
            accept="image/*"
            maxSize="5MB" />
```

---

## ✅ 검증 및 오류 처리

### 1. **클라이언트 사이드 검증**

```ftl
<#-- 검증 규칙을 JSON으로 출력 -->
<script>
window.validationRules = {
    <#if form.validation.rules??>
        <#list form.validation.rules?keys as fieldName>
            "${fieldName}": {
                <#assign rules = form.validation.rules[fieldName]>
                <#if rules.required!false>"required": true,</#if>
                <#if rules.minLength??>"minLength": ${rules.minLength},</#if>
                <#if rules.maxLength??>"maxLength": ${rules.maxLength},</#if>
                <#if rules.pattern??>"pattern": "${rules.pattern}",</#if>
                <#if rules.email!false>"email": true,</#if>
                <#if rules.number!false>"number": true,</#if>
                <#if rules.min??>"min": ${rules.min},</#if>
                <#if rules.max??>"max": ${rules.max},</#if>
                "messages": {
                    <#if rules.messages??>
                        <#list rules.messages?keys as messageKey>
                            "${messageKey}": "${rules.messages[messageKey]}"<#if messageKey?has_next>,</#if>
                        </#list>
                    </#if>
                }
            }<#if fieldName?has_next>,</#if>
        </#list>
    </#if>
};
</script>
```

### 2. **오류 표시 매크로**

```ftl
<#macro fieldError fieldName>
    <#if form.validation.errors?? && form.validation.errors[fieldName]??>
        <div class="invalid-feedback d-block" role="alert">
            <i class="error-icon" aria-hidden="true"></i>
            ${form.validation.errors[fieldName]}
        </div>
    </#if>
</#macro>

<#macro globalErrors>
    <#if form.validation.globalErrors?? && form.validation.globalErrors?has_content>
        <div class="alert alert-danger" role="alert">
            <h4 class="alert-heading">오류가 발생했습니다</h4>
            <ul class="mb-0">
                <#list form.validation.globalErrors as error>
                    <li>${error}</li>
                </#list>
            </ul>
        </div>
    </#if>
</#macro>

<#macro successMessage>
    <#if form.ui.successMessage??>
        <div class="alert alert-success" role="alert">
            <i class="success-icon" aria-hidden="true"></i>
            ${form.ui.successMessage}
        </div>
    </#if>
</#macro>
```

### 3. **실시간 검증**

```ftl
<#-- 실시간 검증을 위한 데이터 속성 -->
<input type="email" 
       name="email"
       data-validate="true"
       data-validate-on="blur change"
       data-validate-endpoint="/api/validate/email"
       data-debounce="300" />
```

---

## 🔒 보안 고려사항

### 1. **CSRF 보호**

```ftl
<#-- 모든 Form에 CSRF 토큰 포함 -->
<form method="POST" action="/submit">
    <#if form.meta.csrf??>
        <input type="hidden" name="_csrf" value="${form.meta.csrf}" />
    <#else>
        <#-- 개발 환경에서 경고 -->
        <!-- WARNING: CSRF token is missing! -->
    </#if>
    
    <!-- form fields -->
</form>
```

### 2. **XSS 방지**

```ftl
<#-- HTML 이스케이핑 -->
<input type="text" value="${user.name?html}" />

<#-- 속성값 이스케이핑 -->
<input type="text" placeholder="${placeholder?js_string}" />

<#-- URL 이스케이핑 -->
<a href="${url?url}">링크</a>
```

### 3. **입력값 제한**

```ftl
<#-- 최대 길이 제한 -->
<input type="text" maxlength="100" />

<#-- 패턴 제한 -->
<input type="text" pattern="[A-Za-z0-9]+" />

<#-- 파일 업로드 제한 -->
<input type="file" accept="image/*" data-max-size="5MB" />
```

---

## ♿ 접근성 고려사항

### 1. **Semantic HTML**

```ftl
<#-- fieldset과 legend 사용 -->
<fieldset>
    <legend>개인정보</legend>
    <!-- related form fields -->
</fieldset>

<#-- 적절한 input type 사용 -->
<input type="email" />      <!-- 이메일 -->
<input type="tel" />        <!-- 전화번호 -->
<input type="date" />       <!-- 날짜 -->
<input type="number" />     <!-- 숫자 -->
```

### 2. **ARIA 속성**

```ftl
<#-- 필수 필드 표시 -->
<input type="text" aria-required="true" required />

<#-- 오류 연결 -->
<input type="text" aria-describedby="username-error" aria-invalid="true" />
<div id="username-error" role="alert">사용자명이 필요합니다.</div>

<#-- 도움말 연결 -->
<input type="password" aria-describedby="password-help" />
<div id="password-help">최소 8자 이상 입력하세요.</div>
```

### 3. **키보드 접근성**

```ftl
<#-- tabindex 순서 관리 -->
<input type="text" tabindex="1" />
<input type="email" tabindex="2" />
<button type="submit" tabindex="3">제출</button>

<#-- 키보드 단축키 -->
<label for="username" accesskey="u">사용자명 (<u>U</u>)</label>
<input type="text" id="username" />
```

---

## 📋 실제 Form 예시

### 사용자 등록 Form

```ftl
<#-- 사용자 등록 Form (user-registration.ftl) -->
<#-- @variables: form.data.user, form.validation, form.meta, form.ui -->

<#include "common/form-macros.ftl">

<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>사용자 등록</title>
    <link rel="stylesheet" href="/css/forms.css">
</head>
<body>
    <div class="container">
        <h1>사용자 등록</h1>
        
        <#-- 전역 오류 메시지 -->
        <@globalErrors />
        
        <#-- 성공 메시지 -->
        <@successMessage />
        
        <form id="userRegistrationForm" 
              action="/users/register" 
              method="POST"
              novalidate>
            
            <#-- CSRF Token -->
            <input type="hidden" name="_csrf" value="${form.meta.csrf!''}" />
            
            <#-- 개인정보 섹션 -->
            <fieldset class="form-section">
                <legend>개인정보</legend>
                
                <div class="row">
                    <div class="col-md-6">
                        <@textInput name="firstName" 
                                    label="이름" 
                                    value="${form.data.user.personalInfo.firstName!''}"
                                    placeholder="이름을 입력하세요"
                                    required=true 
                                    maxLength=50 />
                    </div>
                    <div class="col-md-6">
                        <@textInput name="lastName" 
                                    label="성" 
                                    value="${form.data.user.personalInfo.lastName!''}"
                                    placeholder="성을 입력하세요"
                                    required=true 
                                    maxLength=50 />
                    </div>
                </div>
                
                <div class="row">
                    <div class="col-md-6">
                        <@emailInput name="email" 
                                     label="이메일 주소" 
                                     value="${form.data.user.contactInfo.email!''}"
                                     required=true />
                    </div>
                    <div class="col-md-6">
                        <@textInput name="phone" 
                                    label="전화번호" 
                                    value="${form.data.user.contactInfo.phone!''}"
                                    placeholder="010-0000-0000"
                                    pattern="[0-9]{3}-[0-9]{4}-[0-9]{4}" />
                    </div>
                </div>
                
                <@radioGroup name="gender" 
                             label="성별" 
                             options=form.data.options.genders
                             value="${form.data.user.personalInfo.gender!''}"
                             inline=true />
                
                <@selectInput name="country" 
                              label="국가" 
                              options=form.data.options.countries
                              value="${form.data.user.address.country!''}"
                              required=true />
            </fieldset>
            
            <#-- 계정 정보 섹션 -->
            <fieldset class="form-section">
                <legend>계정 정보</legend>
                
                <@textInput name="username" 
                            label="사용자명" 
                            value="${form.data.user.account.username!''}"
                            placeholder="영문, 숫자 조합 6-20자"
                            required=true 
                            pattern="[A-Za-z0-9]{6,20}"
                            maxLength=20 />
                
                <@passwordInput name="password" 
                                label="비밀번호" 
                                required=true 
                                minLength=8 />
                
                <@passwordInput name="confirmPassword" 
                                label="비밀번호 확인" 
                                required=true 
                                minLength=8 />
            </fieldset>
            
            <#-- 선택사항 섹션 -->
            <fieldset class="form-section">
                <legend>선택사항</legend>
                
                <@fileInput name="profileImage" 
                            label="프로필 이미지" 
                            accept="image/*"
                            maxSize="5MB" />
                
                <@textareaInput name="bio" 
                                label="자기소개" 
                                value="${form.data.user.profile.bio!''}"
                                placeholder="간단한 자기소개를 작성해주세요"
                                rows=4 
                                maxLength=500 />
                
                <div class="form-group">
                    <label class="form-label">알림 설정</label>
                    <@checkboxInput name="notifications.email" 
                                    label="이메일 알림 받기" 
                                    checked=(form.data.user.preferences.notifications.email!false) />
                    <@checkboxInput name="notifications.sms" 
                                    label="SMS 알림 받기" 
                                    checked=(form.data.user.preferences.notifications.sms!false) />
                </div>
            </fieldset>
            
            <#-- 약관 동의 -->
            <fieldset class="form-section">
                <legend>약관 동의</legend>
                
                <@checkboxInput name="agreements.terms" 
                                label="이용약관에 동의합니다 (필수)" 
                                checked=(form.data.user.agreements.terms!false)
                                required=true />
                
                <@checkboxInput name="agreements.privacy" 
                                label="개인정보 처리방침에 동의합니다 (필수)" 
                                checked=(form.data.user.agreements.privacy!false)
                                required=true />
                
                <@checkboxInput name="agreements.marketing" 
                                label="마케팅 정보 수신에 동의합니다 (선택)" 
                                checked=(form.data.user.agreements.marketing!false) />
            </fieldset>
            
            <#-- Form Actions -->
            <div class="form-actions">
                <button type="button" class="btn btn-secondary" onclick="history.back()">
                    취소
                </button>
                <button type="submit" class="btn btn-primary" id="submitBtn">
                    <span class="spinner-border spinner-border-sm d-none" role="status" aria-hidden="true"></span>
                    등록하기
                </button>
            </div>
        </form>
    </div>
    
    <script src="/js/form-validation.js"></script>
    <script src="/js/user-registration.js"></script>
</body>
</html>
```

---

## ❌ 안티패턴과 권장패턴

### 1. **변수 접근**

#### ❌ 안티패턴

```ftl
<!-- 직접 접근 (NullPointerException 위험) -->
<input type="text" value="${user.name}" />

<!-- 평면적 구조 -->
<input type="text" value="${userName}" />
<input type="text" value="${userEmail}" />

<!-- 하드코딩된 값 -->
<select>
    <option value="KR">한국</option>
    <option value="US">미국</option>
</select>
```

#### ✅ 권장패턴

```ftl
<!-- 안전한 접근 -->
<input type="text" value="${form.data.user.name!''}" />

<!-- 계층적 구조 -->
<input type="text" value="${form.data.user.contactInfo.name!''}" />
<input type="text" value="${form.data.user.contactInfo.email!''}" />

<!-- 동적 옵션 -->
<select>
    <#list form.data.options.countries as country>
        <option value="${country.code}">${country.name}</option>
    </#list>
</select>
```

### 2. **오류 처리**

#### ❌ 안티패턴

```ftl
<!-- 전역 오류만 사용 -->
<#if error??>
    <div class="error">${error}</div>
</#if>

<!-- 불명확한 오류 위치 -->
<div class="errors">
    <#list errors as error>
        <div>${error}</div>
    </#list>
</div>
```

#### ✅ 권장패턴

```ftl
<!-- 필드별 오류 -->
<input type="text" name="username" />
<#if form.validation.errors.username??>
    <div class="invalid-feedback" role="alert">
        ${form.validation.errors.username}
    </div>
</#if>

<!-- 접근성을 고려한 오류 연결 -->
<input type="text" 
       name="username" 
       aria-describedby="username-error"
       aria-invalid="${form.validation.errors.username??}" />
<div id="username-error" role="alert">
    ${form.validation.errors.username!''}
</div>
```

### 3. **Form 구조**

#### ❌ 안티패턴

```ftl
<!-- 단일 큰 Form -->
<form>
    <!-- 100개의 필드가 한 번에... -->
</form>

<!-- 매크로 없이 반복 코드 -->
<div class="form-group">
    <label>이름</label>
    <input type="text" name="name" />
    <div class="error">...</div>
</div>
<div class="form-group">
    <label>이메일</label>
    <input type="email" name="email" />
    <div class="error">...</div>
</div>
<!-- 계속 반복... -->
```

#### ✅ 권장패턴

```ftl
<!-- 논리적 섹션 분할 -->
<form>
    <fieldset>
        <legend>개인정보</legend>
        <!-- 관련 필드들 -->
    </fieldset>
    
    <fieldset>
        <legend>연락처</legend>
        <!-- 관련 필드들 -->
    </fieldset>
</form>

<!-- 매크로 활용 -->
<@textInput name="name" label="이름" required=true />
<@emailInput name="email" label="이메일" required=true />
```

---

## 🔍 디버깅 및 테스트

### 1. **디버깅 도구**

```ftl
<#-- 개발 모드에서만 표시되는 디버그 정보 -->
<#if form.meta.debug!false>
    <div class="debug-panel" style="border: 2px solid orange; padding: 10px; margin: 10px 0;">
        <h3>🔍 Debug Information</h3>
        
        <h4>Form Data:</h4>
        <pre>${form.data?json}</pre>
        
        <h4>Validation Errors:</h4>
        <pre>${form.validation.errors?json}</pre>
        
        <h4>UI State:</h4>
        <pre>${form.ui?json}</pre>
        
        <h4>Meta Information:</h4>
        <pre>${form.meta?json}</pre>
    </div>
</#if>
```

### 2. **테스트 데이터 생성**

```ftl
<#-- 테스트용 자동 완성 버튼 (개발 환경에서만) -->
<#if form.meta.environment == "development">
    <div class="test-tools" style="background: #f0f0f0; padding: 10px; margin: 10px 0;">
        <h4>🧪 Test Tools</h4>
        <button type="button" onclick="fillTestData()" class="btn btn-sm btn-warning">
            Fill Test Data
        </button>
        <button type="button" onclick="clearForm()" class="btn btn-sm btn-secondary">
            Clear Form
        </button>
        <button type="button" onclick="validateForm()" class="btn btn-sm btn-info">
            Validate Form
        </button>
    </div>
    
    <script>
    function fillTestData() {
        document.getElementById('firstName').value = 'John';
        document.getElementById('lastName').value = 'Doe';
        document.getElementById('email').value = 'john.doe@example.com';
        document.getElementById('username').value = 'johndoe123';
        // ... 더 많은 테스트 데이터
    }
    
    function clearForm() {
        document.getElementById('userRegistrationForm').reset();
    }
    
    function validateForm() {
        // 클라이언트 사이드 검증 실행
        validateFormFields();
    }
    </script>
</#if>
```

### 3. **로깅**

```ftl
<#-- 사용자 행동 로깅 -->
<script>
// Form 제출 로깅
document.getElementById('userRegistrationForm').addEventListener('submit', function(e) {
    console.log('Form submitted with data:', new FormData(e.target));
    
    // 분석 도구로 전송
    if (typeof gtag !== 'undefined') {
        gtag('event', 'form_submit', {
            'form_id': 'userRegistrationForm',
            'form_step': '${form.ui.currentStep!1}'
        });
    }
});

// 필드별 사용자 상호작용 로깅
document.querySelectorAll('input, select, textarea').forEach(field => {
    field.addEventListener('blur', function() {
        console.log(`Field ${this.name} completed`);
    });
});
</script>
```

---

## ⚡ 성능 최적화

### 1. **조건부 렌더링**

```ftl
<#-- 불필요한 필드는 조건부로만 렌더링 -->
<#if form.data.user.type == "premium">
    <@textInput name="referralCode" 
                label="추천인 코드" 
                value="${form.data.user.referralCode!''}" />
</#if>

<#-- 큰 옵션 리스트는 지연 로딩 -->
<#if form.data.options.cities?size gt 100>
    <select name="city" data-lazy-load="/api/cities" data-country="${form.data.user.country!''}">
        <option value="">도시를 선택하세요</option>
    </select>
<#else>
    <@selectInput name="city" 
                  label="도시" 
                  options=form.data.options.cities />
</#if>
```

### 2. **CSS 및 JS 최적화**

```ftl
<#-- 필요한 CSS만 로드 -->
<#if form.ui.richTextEditor!false>
    <link rel="stylesheet" href="/css/editor.css">
</#if>

<#-- 필요한 JS만 로드 -->
<#if form.validation.clientSide!false>
    <script src="/js/form-validation.js" defer></script>
</#if>

<#if form.ui.fileUpload!false>
    <script src="/js/file-upload.js" defer></script>
</#if>
```

### 3. **캐싱 최적화**

```ftl
<#-- 정적 옵션 데이터는 캐시 활용 -->
<#assign countriesCache = form.data.options.countries>
<#if countriesCache??>
    <!-- 캐시된 데이터 사용 -->
<#else>
    <!-- 새로 로드 -->
</#if>
```

---

## ✅ 체크리스트

### 개발 완료 전 확인 사항

#### 🔍 **기능성**

- [ ] 모든 필수 필드가 정상 작동하는가?
- [ ] 클라이언트/서버 사이드 검증이 모두 작동하는가?
- [ ] 오류 메시지가 적절히 표시되는가?
- [ ] Form 제출이 정상적으로 처리되는가?

#### 🔒 **보안**

- [ ] CSRF 토큰이 포함되어 있는가?
- [ ] XSS 방지를 위한 이스케이핑이 적용되었는가?
- [ ] 입력값 길이 제한이 설정되어 있는가?
- [ ] 파일 업로드 제한이 적절한가?

#### ♿ **접근성**

- [ ] 모든 input에 적절한 label이 연결되어 있는가?
- [ ] ARIA 속성이 올바르게 사용되었는가?
- [ ] 키보드만으로 모든 기능을 사용할 수 있는가?
- [ ] 색상에만 의존하지 않고 정보를 전달하는가?

#### 📱 **반응형**

- [ ] 모바일 환경에서 정상 작동하는가?
- [ ] 터치 입력이 편리한가?
- [ ] 화면 크기에 따라 적절히 배치되는가?

#### 🎨 **사용자 경험**

- [ ] 직관적인 Form 흐름인가?
- [ ] 오류 발생 시 명확한 안내가 제공되는가?
- [ ] 로딩 상태가 적절히 표시되는가?
- [ ] 성공/실패 피드백이 명확한가?

#### 🔧 **코드 품질**

- [ ] 변수 네이밍이 규칙에 맞는가?
- [ ] 매크로가 적절히 활용되었는가?
- [ ] 주석이 충분히 작성되었는가?
- [ ] 테스트 케이스가 작성되었는가?

#### ⚡ **성능**

- [ ] 불필요한 필드 렌더링이 없는가?
- [ ] 큰 데이터셋이 적절히 처리되는가?
- [ ] CSS/JS가 최적화되어 있는가?

---

## 📚 추가 리소스

### 참고 문서

- [FreeMarker 공식 매뉴얼](https://freemarker.apache.org/docs/)
- [Web Content Accessibility Guidelines (WCAG)](https://www.w3.org/WAI/WCAG21/quickref/)
- [HTML5 Form Validation](https://developer.mozilla.org/en-US/docs/Learn/Forms/Form_validation)

### 도구 및 라이브러리

- [Form Validation Library](https://github.com/your-org/form-validation)
- [Accessibility Testing Tools](https://www.w3.org/WAI/ER/tools/)
- [CSS Framework for Forms](https://getbootstrap.com/docs/5.0/forms/overview/)

### 팀 리소스

- [Form 컴포넌트 라이브러리](/wiki/form-components)
- [디자인 시스템](/wiki/design-system)
- [코드 리뷰 가이드라인](/wiki/code-review-forms)

---

**이 가이드를 따라 개발하면 안전하고 접근 가능하며 유지보수하기 쉬운 Form을 만들 수 있습니다. 궁금한 점이 있으면 언제든지 팀에 문의하세요!** 🚀