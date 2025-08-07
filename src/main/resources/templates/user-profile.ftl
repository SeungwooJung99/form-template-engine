<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${user.name} - 프로필 카드</title>
    <style>
        body {
            font-family: 'Malgun Gothic', Arial, sans-serif;
            margin: 0;
            padding: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
        }
        .profile-container {
            max-width: 900px;
            margin: 0 auto;
            background: white;
            border-radius: 20px;
            overflow: hidden;
            box-shadow: 0 20px 40px rgba(0,0,0,0.1);
        }
        .profile-header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 40px;
            text-align: center;
            color: white;
            position: relative;
        }
        .profile-avatar {
            width: 120px;
            height: 120px;
            border-radius: 50%;
            border: 4px solid white;
            margin: 0 auto 20px;
            background: #f0f0f0;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 48px;
            font-weight: bold;
            color: #666;
        <#if user.avatarUrl??>
            background-image: url('${user.avatarUrl}');
            background-size: cover;
            background-position: center;
        </#if>
        }
        .profile-name {
            font-size: 32px;
            font-weight: bold;
            margin: 0;
        }
        .profile-title {
            font-size: 18px;
            opacity: 0.9;
            margin: 5px 0 0 0;
        }
        .profile-content {
            padding: 40px;
        }
        .info-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 30px;
            margin-bottom: 30px;
        }
        .info-section {
            background: #f8f9fa;
            padding: 25px;
            border-radius: 12px;
            border-left: 4px solid #007bff;
        }
        .info-title {
            font-size: 18px;
            font-weight: bold;
            color: #2c3e50;
            margin-bottom: 15px;
            display: flex;
            align-items: center;
        }
        .info-title::before {
            content: '';
            display: inline-block;
            width: 20px;
            height: 20px;
            margin-right: 10px;
            border-radius: 50%;
            background: #007bff;
        }
        .info-list {
            list-style: none;
            padding: 0;
            margin: 0;
        }
        .info-list li {
            padding: 8px 0;
            border-bottom: 1px solid #eee;
            display: flex;
            justify-content: space-between;
        }
        .info-list li:last-child {
            border-bottom: none;
        }
        .info-label {
            font-weight: 600;
            color: #555;
            min-width: 100px;
        }
        .info-value {
            color: #333;
            flex: 1;
            text-align: right;
        }
        .skills-container {
            margin: 20px 0;
        }
        .skill-tag {
            display: inline-block;
            background: #e3f2fd;
            color: #1976d2;
            padding: 6px 12px;
            margin: 4px;
            border-radius: 20px;
            font-size: 14px;
            border: 1px solid #bbdefb;
        }
        .projects-list {
            margin: 15px 0;
        }
        .project-item {
            background: white;
            padding: 15px;
            margin: 10px 0;
            border-radius: 8px;
            border-left: 4px solid #28a745;
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        }
        .project-name {
            font-weight: bold;
            color: #2c3e50;
            font-size: 16px;
        }
        .project-description {
            color: #666;
            margin: 5px 0;
            font-size: 14px;
        }
        .project-status {
            font-size: 12px;
            padding: 3px 8px;
            border-radius: 12px;
            color: white;
            display: inline-block;
            margin-top: 8px;
        }
        .status-active { background-color: #28a745; }
        .status-completed { background-color: #6c757d; }
        .status-pending { background-color: #ffc107; color: #333; }
        .footer {
            text-align: center;
            padding: 20px;
            background: #f8f9fa;
            color: #666;
            font-size: 14px;
        }
        .contact-buttons {
            display: flex;
            justify-content: center;
            gap: 10px;
            margin-top: 20px;
        }
        .contact-btn {
            display: inline-block;
            padding: 10px 20px;
            background: rgba(255,255,255,0.2);
            color: white;
            text-decoration: none;
            border-radius: 25px;
            border: 2px solid rgba(255,255,255,0.3);
            transition: all 0.3s;
        }
        .contact-btn:hover {
            background: rgba(255,255,255,0.3);
            transform: translateY(-2px);
        }
        .achievement-badge {
            background: #ffd700;
            color: #333;
            padding: 4px 8px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: bold;
            margin: 2px;
            display: inline-block;
        }
    </style>
</head>
<body>
<div class="profile-container">
    <!-- 프로필 헤더 -->
    <div class="profile-header">
        <div class="profile-avatar">
            <#if !user.avatarUrl??>
                ${user.name?substring(0,1)?upper_case}
            </#if>
        </div>
        <h1 class="profile-name">${user.name}</h1>
        <p class="profile-title">${user.position!"직책 미지정"} <#if user.department??>@ ${user.department}</#if></p>

        <div class="contact-buttons">
            <#if user.email??>
                <a href="mailto:${user.email}" class="contact-btn">📧 이메일</a>
            </#if>
            <#if user.phone??>
                <a href="tel:${user.phone}" class="contact-btn">📞 전화</a>
            </#if>
            <#if user.linkedIn??>
                <a href="${user.linkedIn}" class="contact-btn" target="_blank">💼 LinkedIn</a>
            </#if>
        </div>
    </div>

    <!-- 프로필 컨텐츠 -->
    <div class="profile-content">
        <div class="info-grid">
            <!-- 기본 정보 섹션 -->
            <div class="info-section">
                <h3 class="info-title">📋 기본 정보</h3>
                <ul class="info-list">
                    <li>
                        <span class="info-label">사원번호:</span>
                        <span class="info-value">${user.employeeId!"미지정"}</span>
                    </li>
                    <li>
                        <span class="info-label">입사일:</span>
                        <span class="info-value">${user.hireDate?string("yyyy-MM-dd")}</span>
                    </li>
                    <li>
                        <span class="info-label">근무연차:</span>
                        <span class="info-value">
                                <#assign yearsWorked = (.now?string("yyyy")?number - user.hireDate?string("yyyy")?number)>
                            ${yearsWorked}년차
                            </span>
                    </li>
                    <li>
                        <span class="info-label">소속팀:</span>
                        <span class="info-value">${user.team!"미지정"}</span>
                    </li>
                    <#if user.location??>
                        <li>
                            <span class="info-label">근무지:</span>
                            <span class="info-value">${user.location}</span>
                        </li>
                    </#if>
                    <#if user.manager??>
                        <li>
                            <span class="info-label">직속상관:</span>
                            <span class="info-value">${user.manager}</span>
                        </li>
                    </#if>
                </ul>
            </div>

            <!-- 연락처 정보 섹션 -->
            <div class="info-section">
                <h3 class="info-title">📞 연락처 정보</h3>
                <ul class="info-list">
                    <#if user.email??>
                        <li>
                            <span class="info-label">이메일:</span>
                            <span class="info-value">${user.email}</span>
                        </li>
                    </#if>
                    <#if user.phone??>
                        <li>
                            <span class="info-label">휴대폰:</span>
                            <span class="info-value">${user.phone}</span>
                        </li>
                    </#if>
                    <#if user.extension??>
                        <li>
                            <span class="info-label">내선번호:</span>
                            <span class="info-value">${user.extension}</span>
                        </li>
                    </#if>
                    <#if user.slackId??>
                        <li>
                            <span class="info-label">Slack:</span>
                            <span class="info-value">@${user.slackId}</span>
                        </li>
                    </#if>
                </ul>
            </div>
        </div>

        <!-- 스킬 섹션 -->
        <#if user.skills?? && user.skills?has_content>
            <div class="info-section">
                <h3 class="info-title">🎯 전문 기술</h3>
                <div class="skills-container">
                    <#list user.skills as skill>
                        <span class="skill-tag">${skill}</span>
                    </#list>
                </div>
            </div>
        </#if>

        <!-- 진행중인 프로젝트 섹션 -->
        <#if user.projects?? && user.projects?has_content>
            <div class="info-section">
                <h3 class="info-title">🚀 담당 프로젝트</h3>
                <div class="projects-list">
                    <#list user.projects as project>
                        <div class="project-item">
                            <div class="project-name">${project.name}</div>
                            <#if project.description??>
                                <div class="project-description">${project.description}</div>
                            </#if>
                            <div class="project-status status-${project.status!'active'}">
                                <#switch project.status!'active'>
                                    <#case "active">진행중<#break>
                                    <#case "completed">완료<#break>
                                    <#case "pending">대기<#break>
                                    <#default>${project.status}
                                </#switch>
                            </div>
                        </div>
                    </#list>
                </div>
            </div>
        </#if>

        <!-- 성과 및 인증 섹션 -->
        <#if user.achievements?? && user.achievements?has_content>
            <div class="info-section">
                <h3 class="info-title">🏆 성과 및 인증</h3>
                <div>
                    <#list user.achievements as achievement>
                        <span class="achievement-badge">${achievement}</span>
                    </#list>
                </div>
            </div>
        </#if>

        <!-- 자기소개 섹션 -->
        <#if user.bio??>
            <div class="info-section">
                <h3 class="info-title">💭 자기소개</h3>
                <p style="line-height: 1.6; color: #555; margin: 0;">${user.bio}</p>
            </div>
        </#if>
    </div>

    <!-- 푸터 -->
    <div class="footer">
        <p>프로필 카드 생성일: ${.now?string("yyyy-MM-dd HH:mm")}</p>
        <p>최종 업데이트: ${user.lastUpdated?string("yyyy-MM-dd")}</p>
    </div>
</div>
</body>
</html>