<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${documentType!"송장"} - ${invoiceNumber}</title>
    <style>
        body { font-family: 'Malgun Gothic', Arial, sans-serif; margin: 0; padding: 20px; color: #333; }
        .invoice-container { max-width: 800px; margin: 0 auto; background: white; }
        .header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 30px; padding-bottom: 20px; border-bottom: 3px solid #2c3e50; }
        .company-info { flex: 1; }
        .company-name { font-size: 28px; font-weight: bold; color: #2c3e50; margin: 0; }
        .company-details { margin-top: 10px; color: #666; line-height: 1.5; }
        .invoice-info { text-align: right; }
        .invoice-title { font-size: 32px; font-weight: bold; color: #e74c3c; margin: 0; }
        .invoice-number { font-size: 18px; color: #666; margin: 5px 0; }
        .billing-section { display: flex; justify-content: space-between; margin: 30px 0; }
        .billing-info { flex: 1; padding: 20px; margin-right: 20px; background-color: #f8f9fa; border-radius: 8px; }
        .billing-info:last-child { margin-right: 0; }
        .billing-title { font-weight: bold; color: #2c3e50; margin-bottom: 10px; font-size: 16px; }
        .billing-details { line-height: 1.6; }
        .items-table { width: 100%; border-collapse: collapse; margin: 30px 0; }
        .items-table th { background-color: #34495e; color: white; padding: 15px; text-align: left; font-weight: bold; }
        .items-table td { padding: 12px 15px; border-bottom: 1px solid #eee; }
        .items-table tr:hover { background-color: #f8f9fa; }
        .quantity, .rate, .amount { text-align: right; }
        .totals-section { margin-top: 30px; }
        .totals-table { width: 300px; margin-left: auto; border-collapse: collapse; }
        .totals-table td { padding: 8px 15px; border-bottom: 1px solid #ddd; }
        .totals-table .total-row { background-color: #2c3e50; color: white; font-weight: bold; font-size: 18px; }
        .notes-section { margin: 30px 0; padding: 20px; background-color: #f8f9fa; border-left: 4px solid #3498db; }
        .footer { margin-top: 40px; text-align: center; padding: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; }
        .payment-info { margin: 20px 0; padding: 15px; background-color: #e8f5e8; border-radius: 5px; }
        .highlight { background-color: #fff3cd; padding: 10px; border-radius: 4px; margin: 10px 0; }
        @media print { body { margin: 0; } .invoice-container { box-shadow: none; } }
    </style>
</head>
<body>
<div class="invoice-container">
    <!-- 헤더 섹션 -->
    <div class="header">
        <div class="company-info">
            <div class="company-name">${company.name}</div>
            <div class="company-details">
                ${company.address}<br>
                <#if company.phone??>전화: ${company.phone}</#if>
                <#if company.email??>이메일: ${company.email}</#if><br>
                <#if company.website??>웹사이트: ${company.website}</#if>
                <#if company.businessNumber??>사업자번호: ${company.businessNumber}</#if>
            </div>
        </div>
        <div class="invoice-info">
            <div class="invoice-title">${documentType!"송장"}</div>
            <div class="invoice-number">${documentType!"송장"} 번호: ${invoiceNumber}</div>
            <div>발행일: ${issueDate?string("yyyy-MM-dd")}</div>
            <#if dueDate??>
                <div>지불기한: ${dueDate?string("yyyy-MM-dd")}</div>
            </#if>
        </div>
    </div>

    <!-- 청구 정보 섹션 -->
    <div class="billing-section">
        <div class="billing-info">
            <div class="billing-title">청구 대상</div>
            <div class="billing-details">
                <strong>${client.name}</strong><br>
                <#if client.contactPerson??>담당자: ${client.contactPerson}<br></#if>
                ${client.address}<br>
                <#if client.phone??>전화: ${client.phone}<br></#if>
                <#if client.email??>이메일: ${client.email}<br></#if>
                <#if client.businessNumber??>사업자번호: ${client.businessNumber}</#if>
            </div>
        </div>
        <div class="billing-info">
            <div class="billing-title">발급 정보</div>
            <div class="billing-details">
                <strong>담당자:</strong> ${salesperson!"미지정"}<br>
                <strong>프로젝트:</strong> ${projectName!"일반"}<br>
                <#if referenceNumber??>
                    <strong>참조번호:</strong> ${referenceNumber}<br>
                </#if>
                <#if poNumber??>
                    <strong>PO번호:</strong> ${poNumber}<br>
                </#if>
                <strong>결제조건:</strong> ${paymentTerms!"현금"}
            </div>
        </div>
    </div>

    <!-- 항목 테이블 -->
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
        <#list items as item>
            <#assign itemTotal = item.quantity * item.rate>
            <#assign subtotal = subtotal + itemTotal>
            <tr>
                <td>
                    <strong>${item.name}</strong>
                    <#if item.description??>
                        <br><small style="color: #666;">${item.description}</small>
                    </#if>
                </td>
                <td class="quantity">${item.quantity?string("#,##0.##")}</td>
                <td class="rate">₩${item.rate?string("#,##0")}</td>
                <td class="amount">₩${itemTotal?string("#,##0")}</td>
            </tr>
        </#list>
        </tbody>
    </table>

    <!-- 합계 섹션 -->
    <div class="totals-section">
        <table class="totals-table">
            <tr>
                <td><strong>소계</strong></td>
                <td style="text-align: right;">₩${subtotal?string("#,##0")}</td>
            </tr>
            <#if discount?? && discount gt 0>
                <tr>
                    <td>할인 ${discountType!"금액"}</td>
                    <td style="text-align: right;">-₩${discountAmount?string("#,##0")}</td>
                </tr>
            </#if>
            <#if taxRate?? && taxRate gt 0>
                <tr>
                    <td>부가세 (${taxRate}%)</td>
                    <td style="text-align: right;">₩${taxAmount?string("#,##0")}</td>
                </tr>
            </#if>
            <tr class="total-row">
                <td><strong>총 금액</strong></td>
                <td style="text-align: right;"><strong>₩${totalAmount?string("#,##0")}</strong></td>
            </tr>
        </table>
    </div>

    <!-- 결제 정보 -->
    <#if paymentInfo??>
        <div class="payment-info">
            <strong>🏦 결제 정보</strong><br>
            ${paymentInfo}
        </div>
    </#if>

    <!-- 특별 안내사항 -->
    <#if notes??>
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
            지불기한은 ${dueDate?string("yyyy년 MM월 dd일")}입니다.
        </#if>
    </div>

    <!-- 푸터 -->
    <div class="footer">
        <p>본 문서는 전자적으로 생성되었으며 서명이 필요하지 않습니다.</p>
        <p>${company.name} | 문의: ${company.email!"N/A"}</p>
    </div>
</div>
</body>
</html>