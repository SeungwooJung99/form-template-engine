<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${reportYear}년 ${reportMonth}월 월간 보고서</title>
    <style>
        body { font-family: 'Malgun Gothic', Arial, sans-serif; margin: 20px; line-height: 1.6; color: #333; }
        .header { text-align: center; margin-bottom: 30px; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border-radius: 10px; }
        .report-title { font-size: 28px; margin: 0; font-weight: bold; }
        .report-info { margin-top: 10px; font-size: 16px; opacity: 0.9; }
        .section { margin: 30px 0; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px; background-color: #fafafa; }
        .section-title { color: #2c3e50; font-size: 22px; margin-bottom: 15px; padding-bottom: 8px; border-bottom: 2px solid #3498db; }
        .metric-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin: 20px 0; }
        .metric-card { background: white; padding: 15px; border-radius: 8px; border-left: 4px solid #3498db; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
        .metric-value { font-size: 24px; font-weight: bold; color: #2980b9; }
        .metric-label { font-size: 14px; color: #7f8c8d; margin-top: 5px; }
        .achievement-item { background: #e8f5e8; padding: 12px; margin: 8px 0; border-left: 4px solid #27ae60; border-radius: 4px; }
        .issue-item { background: #fef5e7; padding: 12px; margin: 8px 0; border-left: 4px solid #f39c12; border-radius: 4px; }
        .table { width: 100%; border-collapse: collapse; margin: 15px 0; }
        .table th, .table td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        .table th { background-color: #34495e; color: white; font-weight: bold; }
        .table tr:nth-child(even) { background-color: #f9f9f9; }
        .footer { margin-top: 40px; text-align: center; font-size: 12px; color: #7f8c8d; padding: 20px; border-top: 1px solid #eee; }
        .status-complete { color: #27ae60; font-weight: bold; }
        .status-progress { color: #f39c12; font-weight: bold; }
        .status-pending { color: #e74c3c; font-weight: bold; }
    </style>
</head>
<body>
<div class="header">
    <h1 class="report-title">${departmentName} 월간 보고서</h1>
    <div class="report-info">
        ${reportYear}년 ${reportMonth}월 | 보고자: ${reporterName} | 작성일: ${.now?string("yyyy-MM-dd")}
    </div>
</div>

<!-- 핵심 지표 섹션 -->
<div class="section">
    <h2 class="section-title">📊 핵심 성과 지표 (KPI)</h2>
    <div class="metric-grid">
        <#list kpis as kpi>
            <div class="metric-card">
                <div class="metric-value">${kpi.value}${kpi.unit!""}</div>
                <div class="metric-label">${kpi.name}</div>
                <#if kpi.change??>
                    <div style="color: ${kpi.change?starts_with('+')?then('#27ae60', '#e74c3c')};">
                        ${kpi.change}
                    </div>
                </#if>
            </div>
        </#list>
    </div>
</div>

<!-- 주요 성과 섹션 -->
<div class="section">
    <h2 class="section-title">🏆 주요 성과 및 달성사항</h2>
    <#if achievements?? && achievements?has_content>
        <#list achievements as achievement>
            <div class="achievement-item">
                <strong>${achievement.title}</strong>
                <p>${achievement.description}</p>
                <#if achievement.metrics??>
                    <small>📈 ${achievement.metrics}</small>
                </#if>
            </div>
        </#list>
    <#else>
        <p>이번 달 주요 성과가 기록되지 않았습니다.</p>
    </#if>
</div>

<!-- 프로젝트 현황 섹션 -->
<div class="section">
    <h2 class="section-title">🚀 프로젝트 현황</h2>
    <table class="table">
        <thead>
        <tr>
            <th>프로젝트명</th>
            <th>담당자</th>
            <th>진행률</th>
            <th>상태</th>
            <th>예상 완료일</th>
        </tr>
        </thead>
        <tbody>
        <#list projects as project>
            <tr>
                <td>${project.name}</td>
                <td>${project.manager}</td>
                <td>${project.progress}%</td>
                <td class="status-${project.status}">
                    <#switch project.status>
                        <#case "complete">완료<#break>
                        <#case "progress">진행중<#break>
                        <#case "pending">대기<#break>
                        <#default>${project.status}
                    </#switch>
                </td>
                <td>${project.dueDate!"미정"}</td>
            </tr>
        </#list>
        </tbody>
    </table>
</div>

<!-- 이슈 및 개선사항 섹션 -->
<div class="section">
    <h2 class="section-title">⚠️ 주요 이슈 및 개선사항</h2>
    <#if issues?? && issues?has_content>
        <#list issues as issue>
            <div class="issue-item">
                <strong>${issue.title}</strong>
                <p>${issue.description}</p>
                <#if issue.action??>
                    <small><strong>대응방안:</strong> ${issue.action}</small>
                </#if>
            </div>
        </#list>
    <#else>
        <p>이번 달 특별한 이슈가 없었습니다.</p>
    </#if>
</div>

<!-- 다음 달 계획 섹션 -->
<div class="section">
    <h2 class="section-title">📅 다음 달 주요 계획</h2>
    <#if nextMonthPlans?? && nextMonthPlans?has_content>
        <ul>
            <#list nextMonthPlans as plan>
                <li>
                    <strong>${plan.title}</strong>
                    <#if plan.description??>
                        <br><span style="color: #666;">${plan.description}</span>
                    </#if>
                    <#if plan.deadline??>
                        <br><small>📅 목표일: ${plan.deadline}</small>
                    </#if>
                </li>
            </#list>
        </ul>
    <#else>
        <p>다음 달 계획이 아직 수립되지 않았습니다.</p>
    </#if>
</div>

<div class="footer">
    <p>본 보고서는 ${.now?string("yyyy-MM-dd HH:mm")}에 자동 생성되었습니다.</p>
    <p>${companyName} | ${departmentName}</p>
</div>
</body>
</html>