<!-- invoice.ftl 상단에 추가할 안전한 숫자 포맷팅 매크로 -->

<#-- 안전한 숫자 포맷팅 매크로 -->
<#macro formatNumber value defaultValue=0>
    <#if value??>
        <#if value?is_number>
            ${value?string("#,##0")}<#t>
        <#elseif value?is_string>
            <#attempt>
                ${value?replace(",", "")?number?string("#,##0")}<#t>
                <#recover>
                    ${defaultValue?string("#,##0")}<#t>
            </#attempt>
        <#else>
            ${defaultValue?string("#,##0")}<#t>
        </#if>
    <#else>
        ${defaultValue?string("#,##0")}<#t>
    </#if>
</#macro>

<#-- 안전한 숫자 추출 함수 -->
<#function safeNumber value defaultValue=0>
    <#if value??>
        <#if value?is_number>
            <#return value>
        <#elseif value?is_string>
            <#attempt>
                <#return value?replace(",", "")?number>
                <#recover>
                    <#return defaultValue>
            </#attempt>
        <#else>
            <#return defaultValue>
        </#if>
    <#else>
        <#return defaultValue>
    </#if>
</#function>


<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${documentType!"송장"} - ${invoiceNumber!"N/A"}</title>
    <style>
        body {
            font-family: 'Malgun Gothic', Arial, sans-serif;
            margin: 0;
            padding: 20px;
            color: #333;
        }

        .invoice-container {
            max-width: 800px;
            margin: 0 auto;
            background: white;
        }

        .header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 30px;
            padding-bottom: 20px;
            border-bottom: 3px solid #2c3e50;
        }

        .company-info {
            flex: 1;
        }

        .company-name {
            font-size: 28px;
            font-weight: bold;
            color: #2c3e50;
            margin: 0;
        }

        .company-details {
            margin-top: 10px;
            color: #666;
            line-height: 1.5;
        }

        .invoice-info {
            text-align: right;
        }

        .invoice-title {
            font-size: 32px;
            font-weight: bold;
            color: #e74c3c;
            margin: 0;
        }

        .invoice-number {
            font-size: 18px;
            color: #666;
            margin: 5px 0;
        }

        .billing-section {
            display: flex;
            justify-content: space-between;
            margin: 30px 0;
        }

        .billing-info {
            flex: 1;
            padding: 20px;
            margin-right: 20px;
            background-color: #f8f9fa;
            border-radius: 8px;
        }

        .billing-info:last-child {
            margin-right: 0;
        }

        .billing-title {
            font-weight: bold;
            color: #2c3e50;
            margin-bottom: 10px;
            font-size: 16px;
        }

        .billing-details {
            line-height: 1.6;
        }

        .items-table {
            width: 100%;
            border-collapse: collapse;
            margin: 30px 0;
        }

        .items-table th {
            background-color: #34495e;
            color: white;
            padding: 15px;
            text-align: left;
            font-weight: bold;
        }

        .items-table td {
            padding: 12px 15px;
            border-bottom: 1px solid #eee;
        }

        .items-table tr:hover {
            background-color: #f8f9fa;
        }

        .quantity, .rate, .amount {
            text-align: right;
        }

        .totals-section {
            margin-top: 30px;
        }

        .totals-table {
            width: 300px;
            margin-left: auto;
            border-collapse: collapse;
        }

        .totals-table td {
            padding: 8px 15px;
            border-bottom: 1px solid #ddd;
        }

        .totals-table .total-row {
            background-color: #2c3e50;
            color: white;
            font-weight: bold;
            font-size: 18px;
        }

        .notes-section {
            margin: 30px 0;
            padding: 20px;
            background-color: #f8f9fa;
            border-left: 4px solid #3498db;
        }

        .footer {
            margin-top: 40px;
            text-align: center;
            padding: 20px;
            border-top: 1px solid #eee;
            color: #666;
            font-size: 12px;
        }

        .payment-info {
            margin: 20px 0;
            padding: 15px;
            background-color: #e8f5e8;
            border-radius: 5px;
        }

        .highlight {
            background-color: #fff3cd;
            padding: 10px;
            border-radius: 4px;
            margin: 10px 0;
        }

        @media print {
            body {
                margin: 0;
            }

            .invoice-container {
                box-shadow: none;
            }
        }
    </style>
</head>
<body>
<div class="invoice-container">
    <!-- 헤더 섹션 -->
    <div class="header">
        <div class="company-info">
            <div class="company-name">
                <#if company?? && company?is_hash>
                    ${company.name!"-"}
                <#else>
                    ${company!"-"}
                </#if>
            </div>
            <div class="company-details">
                <#if company?? && company?is_hash>
                    ${company.address!"-"}<br>
                    <#if company.phone??>전화: ${company.phone!"N/A"}</#if>
                    <#if company.email??>이메일: ${company.email!"N/A"}</#if><br>
                    <#if company.website??>웹사이트: ${company.website!"N/A"}</#if>
                    <#if company.businessNumber??>사업자번호: ${company.businessNumber!"N/A"}</#if>
                <#else>
                    회사 정보 없음
                </#if>
            </div>
        </div>
        <div class="invoice-info">
            <div class="invoice-title">${documentType!"송장"}</div>
            <div class="invoice-number">${documentType!"송장"} 번호: ${invoiceNumber!"-"}</div>
            <div>발행일:
                <#if issueDate??>
                    <#if issueDate?is_date>
                        ${issueDate?string("yyyy-MM-dd")}
                    <#else>
                        ${issueDate?string}
                    </#if>
                <#else>
                    미지정
                </#if>
            </div>
            <#if dueDate??>
                <div>지불기한:
                    <#if dueDate?is_date>
                        ${dueDate?string("yyyy-MM-dd")}
                    <#else>
                        ${dueDate?string}
                    </#if>
                </div>
            </#if>
        </div>
    </div>

    <!-- 청구 정보 섹션 -->
    <div class="billing-section">
        <div class="billing-info">
            <div class="billing-title">청구 대상</div>
            <div class="billing-details">
                <#if client?? && client?is_hash>
                    <#if client.name??>
                        <#assign clientName = client.name>
                    </#if>
<#--                    <strong>${client.name!"-"}</strong><br>-->
                    <#if client.contactPerson??>담당자: ${client.contactPerson!'-'}<br></#if>
                    ${client.address!"-"}<br>
                    <#if client.phone??>전화: ${client.phone!'-'}<br></#if>
                    <#if client.email??>이메일: ${client.email!'-'}<br></#if>
                    <#if client.businessNumber??>사업자번호: ${client.businessNumber!'-'}</#if>
                <#else>
                    <strong>${client!"-"}</strong><br>
                    클라이언트 정보 없음
                </#if>
            </div>
        </div>
        <div class="billing-info">
            <div class="billing-title">발급 정보</div>
            <div class="billing-details">
                <strong>담당자:</strong> ${salesperson!"-"}<br>
                <strong>프로젝트:</strong> ${projectName!"-"}<br>
                <#if referenceNumber??>
                    <strong>참조번호:</strong> ${referenceNumber!0}<br>
                </#if>
                <#if poNumber??>
                    <strong>PO번호:</strong> ${poNumber!'010-0000-0000'}<br>
                </#if>
                <strong>결제조건:</strong> ${paymentTerms!"현금"}
            </div>
        </div>
    </div>

    <!-- 항목 테이블 - 수정된 버전 -->
    <table class="items-table">
        <thead>
        <tr>
            <th style="width: 50%;">품목/서비스</th>
            <th style="width: 15%;" class="quantity">수량</th>
            <th style="width: 15%;" class="rate">단가</th>
            <th style="width: 20%;" class="amount">금액</th>
        </tr>
        </thead>
        <tbody>
        <#assign subtotal = 0>
        <#if items?? && items?has_content>
            <#list items as item>
                <#assign itemQuantity = safeNumber(item.quantity!0)>
                <#assign itemRate = safeNumber(item.rate!0)>
                <#assign itemTotal = itemQuantity * itemRate>
                <#assign subtotal = subtotal + itemTotal>
                <tr>
                    <td>
                        <strong>
                            <#if item?? && item?is_hash>
                                ${item.name!"Unknown Item"}
                            <#else>
                                ${item!"Unknown Item"}
                            </#if>
                        </strong>
                        <#if item?? && item?is_hash && item.description??>
                            <br><small style="color: #666;">${item.description!''}</small>
                        </#if>
                    </td>
                    <td class="quantity"><@formatNumber itemQuantity /></td>
                    <td class="rate">₩<@formatNumber itemRate /></td>
                    <td class="amount">₩<@formatNumber itemTotal /></td>
                </tr>
            </#list>
        <#else>
            <tr>
                <td colspan="4" style="text-align: center; color: #666; padding: 30px;">
                    항목이 없습니다.
                </td>
            </tr>
        </#if>
        </tbody>
    </table>

    <!-- 합계 섹션 - 수정된 버전 -->
    <div class="totals-section">
        <table class="totals-table">
            <tr>
                <td><strong>소계</strong></td>
                <td style="text-align: right;">₩<@formatNumber subtotal /></td>
            </tr>
            <#if discount?? && safeNumber(discount, 0) gt 0>
                <tr>
                    <td>할인 ${discountType!"금액"}</td>
                    <td style="text-align: right;">-₩<@formatNumber safeNumber(discountAmount!0) /></td>
                </tr>
            </#if>
            <#if taxRate?? && safeNumber(taxRate, 0) gt 0>
                <tr>
                    <td>부가세 (<@formatNumber safeNumber(taxRate) />%)</td>
                    <td style="text-align: right;">₩<@formatNumber safeNumber(taxAmount!0) /></td>
                </tr>
            </#if>
            <tr class="total-row">
                <td><strong>총 금액</strong></td>
                <td style="text-align: right;"><strong>₩<@formatNumber safeNumber(totalAmount!0) /></strong></td>
            </tr>
        </table>
    </div>

    <!-- 결제 정보 -->
    <#if paymentInfo?? && paymentInfo?has_content>
        <div class="payment-info">
            <strong>🏦 결제 정보</strong><br>
            ${paymentInfo}
        </div>
    </#if>

    <!-- 특별 안내사항 -->
    <#if notes?? && notes?has_content>
        <div class="notes-section">
            <strong>📋 안내사항</strong><br>
            ${notes}
        </div>
    </#if>

    <!-- 중요 안내 -->
    <div class="highlight">
        <strong>⚠️ 중요 안내:</strong>
        본 ${documentType!"송장"}은 ${.now?string("yyyy-MM-dd HH:mm")}에 발행되었습니다.
        <#if dueDate??>
            <#if dueDate?is_date>
                지불기한은 ${dueDate?string("yyyy년 MM월 dd일")}입니다.
            <#else>
                지불기한은 ${dueDate?string}입니다.
            </#if>
        </#if>
    </div>

    <!-- 푸터 -->
    <div class="footer">
        <p>본 문서는 전자적으로 생성되었으며 서명이 필요하지 않습니다.</p>
        <p>
            <#if company?? && company?is_hash>
                ${company.name!"Unknown Company"} | 문의: ${company.email!"-"}
            <#else>
                ${company!"Unknown Company"} | 문의: -
            </#if>
        </p>
    </div>
</div>
</body>
</html>