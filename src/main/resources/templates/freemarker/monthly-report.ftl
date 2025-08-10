<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${(reportYear!.now?string("yyyy"))?c}년 ${(reportMonth!.now?string("M"))?c}월 월간 보고서</title>
    <style>
        body {
            font-family: 'Malgun Gothic', Arial, sans-serif;
            margin: 20px;
            line-height: 1.6;
            color: #333;
        }

        .header {
            text-align: center;
            margin-bottom: 30px;
            padding: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border-radius: 10px;
        }

        .report-title {
            font-size: 28px;
            margin: 0;
            font-weight: bold;
        }

        .report-info {
            margin-top: 10px;
            font-size: 16px;
            opacity: 0.9;
        }

        .section {
            margin: 30px 0;
            padding: 20px;
            border: 1px solid #e0e0e0;
            border-radius: 8px;
            background-color: #fafafa;
        }

        .section-title {
            color: #2c3e50;
            font-size: 22px;
            margin-bottom: 15px;
            padding-bottom: 8px;
            border-bottom: 2px solid #3498db;
        }

        .metric-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin: 20px 0;
        }

        .metric-card {
            background: white;
            padding: 15px;
            border-radius: 8px;
            border-left: 4px solid #3498db;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
        }

        .metric-value {
            font-size: 24px;
            font-weight: bold;
            color: #2980b9;
        }

        .metric-label {
            font-size: 14px;
            color: #7f8c8d;
            margin-top: 5px;
        }

        .achievement-item {
            background: #e8f5e8;
            padding: 12px;
            margin: 8px 0;
            border-left: 4px solid #27ae60;
            border-radius: 4px;
        }

        .issue-item {
            background: #fef5e7;
            padding: 12px;
            margin: 8px 0;
            border-left: 4px solid #f39c12;
            border-radius: 4px;
        }

        .table {
            width: 100%;
            border-collapse: collapse;
            margin: 15px 0;
        }

        .table th, .table td {
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }

        .table th {
            background-color: #34495e;
            color: white;
            font-weight: bold;
        }

        .table tr:nth-child(even) {
            background-color: #f9f9f9;
        }

        .footer {
            margin-top: 40px;
            text-align: center;
            font-size: 12px;
            color: #7f8c8d;
            padding: 20px;
            border-top: 1px solid #eee;
        }

        .status-complete {
            color: #27ae60;
            font-weight: bold;
        }

        .status-progress {
            color: #f39c12;
            font-weight: bold;
        }

        .status-pending {
            color: #e74c3c;
            font-weight: bold;
        }

        .no-data {
            text-align: center;
            color: #7f8c8d;
            padding: 20px;
            font-style: italic;
        }

        .demo-data {
            background: #fff3cd;
            padding: 10px;
            border-radius: 4px;
            margin-bottom: 15px;
            border-left: 4px solid #ffc107;
        }

        .demo-data small {
            color: #856404;
        }
    </style>
</head>
<body>
<div class="header">
    <h1 class="report-title">${departmentName!"Sample Department"} 월간 보고서</h1>
    <div class="report-info">
        ${(reportYear!.now?string("yyyy"))?c}년 ${(reportMonth!.now?string("M"))?c}월 |
        보고자: ${reporterName!"홍길동"} |
        작성일: ${.now?string("yyyy-MM-dd")}
    </div>
</div>

<#-- 데모 데이터 표시 여부 판단 -->
<#assign hasRealData = (kpis?? && kpis?has_content) || (achievements?? && achievements?has_content) || (projects?? && projects?has_content)>

<#if !hasRealData>
    <div class="demo-data">
        <strong>📋 미리보기 모드</strong><br>
        <small>실제 데이터가 제공되지 않아 샘플 데이터로 표시됩니다. Controller에서 데이터를 전달하면 실제 내용으로 렌더링됩니다.</small>
    </div>
</#if>

<!-- 핵심 지표 섹션 -->
<div class="section">
    <h2 class="section-title">📊 핵심 성과 지표 (KPI)</h2>
    <div class="metric-grid">
        <#if kpis?? && kpis?has_content>
            <#list kpis as kpi>
                <div class="metric-card">
                    <div class="metric-value">
                        <#if kpi?? && kpi?is_hash>
                            ${kpi.value!"N/A"}${kpi.unit!""}
                        <#else>
                            ${kpi!"N/A"}
                        </#if>
                    </div>
                    <div class="metric-label">
                        <#if kpi?? && kpi?is_hash>
                            ${kpi.name!"Unknown KPI"}
                        <#else>
                            KPI ${kpi_index + 1}
                        </#if>
                    </div>
                    <#if kpi?? && kpi?is_hash && kpi.change??>
                        <div style="color: ${kpi.change?starts_with('+')?then('#27ae60', '#e74c3c')};">
                            ${kpi.change}
                        </div>
                    </#if>
                </div>
            </#list>
        <#else>
        <#-- 샘플 KPI 데이터 -->
            <div class="metric-card">
                <div class="metric-value">85%</div>
                <div class="metric-label">목표 달성률</div>
                <div style="color: #27ae60;">+15%</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">1,234</div>
                <div class="metric-label">월간 처리 건수</div>
                <div style="color: #27ae60;">+8.5%</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">4.2</div>
                <div class="metric-label">고객 만족도</div>
                <div style="color: #e74c3c;">-0.3</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">₩2.5M</div>
                <div class="metric-label">월 매출</div>
                <div style="color: #27ae60;">+12%</div>
            </div>
        </#if>
    </div>
</div>

<!-- 주요 성과 섹션 -->
<div class="section">
    <h2 class="section-title">🏆 주요 성과 및 달성사항</h2>
    <#if achievements?? && achievements?has_content>
        <#list achievements as achievement>
            <div class="achievement-item">
                <strong>
                    <#if achievement?? && achievement?is_hash>
                        ${achievement.title!"Unknown Achievement"}
                    <#else>
                        ${achievement!"Unknown Achievement"}
                    </#if>
                </strong>
                <#if achievement?? && achievement?is_hash && achievement.description??>
                    <p>${achievement.description}</p>
                </#if>
                <#if achievement?? && achievement?is_hash && achievement.metrics??>
                    <small>📈 ${achievement.metrics}</small>
                </#if>
            </div>
        </#list>
    <#else>
    <#-- 샘플 성과 데이터 -->
        <div class="achievement-item">
            <strong>신제품 출시 성공</strong>
            <p>새로운 제품 라인 성공적 런칭으로 시장 점유율 확대 달성</p>
            <small>📈 출시 후 첫 달 매출 목표 대비 120% 달성</small>
        </div>
        <div class="achievement-item">
            <strong>고객 서비스 품질 향상</strong>
            <p>CS 프로세스 개선을 통한 고객 응답 시간 단축</p>
            <small>📈 평균 응답 시간 24시간 → 8시간으로 단축</small>
        </div>
        <div class="achievement-item">
            <strong>팀 역량 강화</strong>
            <p>전 직원 대상 교육 프로그램 완료 및 자격증 취득</p>
            <small>📈 팀원 전원 전문 자격증 취득 완료</small>
        </div>
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
        <#if projects?? && projects?has_content>
            <#list projects as project>
                <tr>
                    <td>
                        <#if project?? && project?is_hash>
                            ${project.name!"Unknown Project"}
                        <#else>
                            ${project!"Unknown Project"}
                        </#if>
                    </td>
                    <td>
                        <#if project?? && project?is_hash>
                            ${project.manager!"미지정"}
                        <#else>
                            미지정
                        </#if>
                    </td>
                    <td>
                        <#if project?? && project?is_hash>
                            ${project.progress!0}%
                        <#else>
                            0%
                        </#if>
                    </td>
                    <td>
                        <#assign projectStatus = "">
                        <#if project?? && project?is_hash && project.status??>
                            <#assign projectStatus = project.status>
                        <#else>
                            <#assign projectStatus = "pending">
                        </#if>
                        <span class="status-${projectStatus}">
                            <#switch projectStatus>
                                <#case "complete">완료<#break>
                                <#case "progress">진행중<#break>
                                <#case "pending">대기<#break>
                                <#default>${projectStatus}
                            </#switch>
                        </span>
                    </td>
                    <td>
                        <#if project?? && project?is_hash>
                            ${project.dueDate!"미정"}
                        <#else>
                            미정
                        </#if>
                    </td>
                </tr>
            </#list>
        <#else>
        <#-- 샘플 프로젝트 데이터 -->
            <tr>
                <td>웹사이트 리뉴얼</td>
                <td>김개발</td>
                <td>85%</td>
                <td class="status-progress">진행중</td>
                <td>2025-09-15</td>
            </tr>
            <tr>
                <td>모바일 앱 개발</td>
                <td>이모바일</td>
                <td>100%</td>
                <td class="status-complete">완료</td>
                <td>2025-08-30</td>
            </tr>
            <tr>
                <td>고객 관리 시스템</td>
                <td>박시스템</td>
                <td>30%</td>
                <td class="status-progress">진행중</td>
                <td>2025-10-31</td>
            </tr>
            <tr>
                <td>AI 챗봇 구축</td>
                <td>최인공</td>
                <td>0%</td>
                <td class="status-pending">대기</td>
                <td>2025-12-01</td>
            </tr>
        </#if>
        </tbody>
    </table>
</div>

<!-- 이슈 및 개선사항 섹션 -->
<div class="section">
    <h2 class="section-title">⚠️ 주요 이슈 및 개선사항</h2>
    <#if issues?? && issues?has_content>
        <#list issues as issue>
            <div class="issue-item">
                <strong>
                    <#if issue?? && issue?is_hash>
                        ${issue.title!"Unknown Issue"}
                    <#else>
                        ${issue!"Unknown Issue"}
                    </#if>
                </strong>
                <#if issue?? && issue?is_hash && issue.description??>
                    <p>${issue.description}</p>
                </#if>
                <#if issue?? && issue?is_hash && issue.action??>
                    <small><strong>대응방안:</strong> ${issue.action}</small>
                </#if>
            </div>
        </#list>
    <#else>
    <#-- 샘플 이슈 데이터 -->
        <div class="issue-item">
            <strong>서버 성능 저하</strong>
            <p>트래픽 증가로 인한 응답 속도 지연 현상 발생</p>
            <small><strong>대응방안:</strong> 서버 증설 및 로드밸런싱 적용 예정</small>
        </div>
        <div class="issue-item">
            <strong>인력 부족</strong>
            <p>프로젝트 증가 대비 개발 인력 부족으로 일정 지연</p>
            <small><strong>대응방안:</strong> 추가 인력 채용 진행 중, 외부 개발사 협력 검토</small>
        </div>
    </#if>
</div>

<!-- 다음 달 계획 섹션 -->
<div class="section">
    <h2 class="section-title">📅 다음 달 주요 계획</h2>
    <#if nextMonthPlans?? && nextMonthPlans?has_content>
        <ul>
            <#list nextMonthPlans as plan>
                <li>
                    <strong>
                        <#if plan?? && plan?is_hash>
                            ${plan.title!"Unknown Plan"}
                        <#else>
                            ${plan!"Unknown Plan"}
                        </#if>
                    </strong>
                    <#if plan?? && plan?is_hash && plan.description??>
                        <br><span style="color: #666;">${plan.description}</span>
                    </#if>
                    <#if plan?? && plan?is_hash && plan.deadline??>
                        <br><small>📅 목표일: ${plan.deadline}</small>
                    </#if>
                </li>
            </#list>
        </ul>
    <#else>
    <#-- 샘플 계획 데이터 -->
        <ul>
            <li>
                <strong>신규 서비스 기획</strong>
                <br><span style="color: #666;">차세대 플랫폼을 위한 요구사항 분석 및 기본 설계</span>
                <br><small>📅 목표일: 2025-09-30</small>
            </li>
            <li>
                <strong>팀 역량 강화 교육</strong>
                <br><span style="color: #666;">최신 기술 스택 교육 및 워크샵 진행</span>
                <br><small>📅 목표일: 2025-09-15</small>
            </li>
            <li>
                <strong>시스템 성능 최적화</strong>
                <br><span style="color: #666;">기존 시스템의 병목 지점 개선 및 성능 향상</span>
                <br><small>📅 목표일: 2025-10-15</small>
            </li>
            <li>
                <strong>고객 피드백 반영</strong>
                <br><span style="color: #666;">수집된 고객 의견을 바탕으로 서비스 개선</span>
                <br><small>📅 목표일: 2025-09-20</small>
            </li>
        </ul>
    </#if>
</div>

<div class="footer">
    <p>본 보고서는 ${.now?string("yyyy-MM-dd HH:mm")}에 자동 생성되었습니다.</p>
    <p>${companyName!"Sample Company"} | ${departmentName!"Sample Department"}</p>
    <p><small>
            데이터 상태:
            <#if hasRealData>
                실제 데이터
            <#else>
                샘플 데이터 (미리보기)
            </#if>
        </small></p>
</div>
</body>
</html>