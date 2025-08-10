<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>FreeMarker 템플릿 데모</title>
    <style>
        body {
            font-family: 'Malgun Gothic', Arial, sans-serif;
            margin: 0;
            padding: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            padding: 40px;
            border-radius: 20px;
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
        }

        .header {
            text-align: center;
            margin-bottom: 40px;
            padding-bottom: 20px;
            border-bottom: 3px solid #667eea;
        }

        .main-title {
            font-size: 36px;
            font-weight: bold;
            color: #2c3e50;
            margin: 0;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }

        .subtitle {
            font-size: 18px;
            color: #666;
            margin: 10px 0 0 0;
        }

        .templates-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
            gap: 25px;
            margin: 30px 0;
        }

        .template-card {
            background: #f8f9fa;
            padding: 25px;
            border-radius: 15px;
            border-left: 5px solid #667eea;
            box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
            transition: all 0.3s ease;
            position: relative;
            overflow: hidden;
        }

        .template-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 3px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }

        .template-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
        }

        .template-name {
            font-size: 22px;
            font-weight: bold;
            color: #2c3e50;
            margin-bottom: 10px;
            display: flex;
            align-items: center;
        }

        .template-name::before {
            content: '📄';
            margin-right: 10px;
            font-size: 24px;
        }

        .template-file {
            font-size: 14px;
            color: #666;
            background: #e9ecef;
            padding: 5px 10px;
            border-radius: 15px;
            display: inline-block;
            margin-bottom: 15px;
            font-family: 'Courier New', monospace;
        }

        .template-link {
            display: inline-block;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 12px 25px;
            text-decoration: none;
            border-radius: 25px;
            font-weight: bold;
            transition: all 0.3s ease;
            box-shadow: 0 4px 10px rgba(102, 126, 234, 0.3);
        }

        .template-link:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 15px rgba(102, 126, 234, 0.4);
        }

        .info-section {
            background: #e8f4fd;
            padding: 25px;
            border-radius: 15px;
            margin: 30px 0;
            border-left: 5px solid #3498db;
        }

        .info-title {
            color: #2c3e50;
            font-size: 20px;
            font-weight: bold;
            margin-bottom: 15px;
            display: flex;
            align-items: center;
        }

        .info-title::before {
            content: '💡';
            margin-right: 10px;
        }

        .info-content {
            color: #555;
            line-height: 1.6;
        }

        .footer {
            margin-top: 40px;
            text-align: center;
            padding: 20px;
            border-top: 2px solid #eee;
            color: #666;
        }

        .badge {
            background: #28a745;
            color: white;
            padding: 3px 8px;
            border-radius: 10px;
            font-size: 12px;
            font-weight: bold;
            margin-left: 10px;
        }

        .demo-card {
            background: #fff3cd;
            border-left: 5px solid #ffc107;
        }

        .demo-card .template-name::before {
            content: '🎯';
        }
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <h1 class="main-title">🚀 FreeMarker 템플릿 데모</h1>
        <p class="subtitle">Spring Boot + FreeMarker로 구현된 다양한 템플릿들을 확인해보세요</p>
    </div>

    <div class="info-section">
        <h3 class="info-title">사용 방법</h3>
        <div class="info-content">
            아래의 각 템플릿 카드를 클릭하면 실제 렌더링된 결과를 볼 수 있습니다.
            모든 데이터는 Controller에서 Model을 통해 전달되며, 각 템플릿은 실제 비즈니스 상황에서
            사용할 수 있는 실용적인 예제들로 구성되어 있습니다.
        </div>
    </div>

    <div class="templates-grid">
        <#-- 동적 템플릿 목록 렌더링 -->
        <#if templates?? && templates?has_content>
            <#list templates as template>
                <div class="template-card">
                    <div class="template-name">
                        <#-- template이 객체인지 문자열인지 확인 -->
                        <#if template?? && template?is_hash>
                            ${template.name!"Unknown Template"}
                        <#elseif template?? && template?is_string>
                            ${template}
                        <#else>
                            Unknown Template
                        </#if>
                    </div>
                    <div class="template-file">
                        <#if template?? && template?is_hash>
                            ${template.template!"unknown.ftl"}
                        <#elseif template?? && template?is_string>
                            ${template}.ftl
                        <#else>
                            unknown.ftl
                        </#if>
                    </div>
                    <div>
                        <#assign templateUrl = "">
                        <#if template?? && template?is_hash && template.url??>
                            <#assign templateUrl = template.url>
                        <#elseif template?? && template?is_hash && template.template??>
                            <#assign templateUrl = "/api/template/preview/" + template.template>
                        <#elseif template?? && template?is_string>
                            <#assign templateUrl = "/api/template/preview/" + template>
                        <#else>
                            <#assign templateUrl = "#">
                        </#if>
                        <a href="${templateUrl}" class="template-link" target="_blank">
                            템플릿 보기 →
                        </a>
                    </div>
                </div>
            </#list>
        <#else>
        <#-- 데이터가 없을 때 기본 템플릿 카드들 표시 -->
            <div class="template-card demo-card">
                <div class="template-name">송장/세금계산서 템플릿</div>
                <div class="template-file">invoice.ftl</div>
                <div>
                    <a href="/api/template/preview/invoice.ftl" class="template-link" target="_blank">
                        템플릿 보기 →
                    </a>
                </div>
            </div>

            <div class="template-card demo-card">
                <div class="template-name">월간 보고서 템플릿</div>
                <div class="template-file">monthly-report.ftl</div>
                <div>
                    <a href="/api/template/preview/monthly-report.ftl" class="template-link" target="_blank">
                        템플릿 보기 →
                    </a>
                </div>
            </div>

            <div class="template-card demo-card">
                <div class="template-name">사용자 프로필 템플릿</div>
                <div class="template-file">user-profile.ftl</div>
                <div>
                    <a href="/api/template/preview/user-profile.ftl" class="template-link" target="_blank">
                        템플릿 보기 →
                    </a>
                </div>
            </div>

            <div class="template-card demo-card">
                <div class="template-name">환영 이메일 템플릿</div>
                <div class="template-file">welcome-email.ftl</div>
                <div>
                    <a href="/api/template/preview/welcome-email.ftl" class="template-link" target="_blank">
                        템플릿 보기 →
                    </a>
                </div>
            </div>

            <div class="template-card demo-card">
                <div class="template-name">시스템 알림 템플릿</div>
                <div class="template-file">system-notification.ftl</div>
                <div>
                    <a href="/api/template/preview/system-notification.ftl" class="template-link" target="_blank">
                        템플릿 보기 →
                    </a>
                </div>
            </div>

            <div class="template-card">
                <div class="template-name">⚠️ 동적 템플릿 로딩</div>
                <div class="template-file">templates 데이터가 제공되지 않음</div>
                <div style="margin-top: 10px;">
                    <small style="color: #666;">
                        Controller에서 templates 변수를 전달하면 동적으로 템플릿 목록이 표시됩니다.<br>
                        위의 기본 템플릿들을 참고하여 새로운 템플릿을 추가해보세요.
                    </small>
                </div>
            </div>
        </#if>
    </div>

    <div class="info-section">
        <h3 class="info-title">포함된 템플릿 기능</h3>
        <div class="info-content">
            <ul>
                <li><strong>송장/세금계산서:</strong> 완전한 비즈니스 송장 템플릿 (항목, 세금, 할인 계산 포함)</li>
                <li><strong>월간 보고서:</strong> KPI, 성과, 프로젝트 현황을 포함한 종합 보고서</li>
                <li><strong>시스템 알림:</strong> 사용자 환영 및 시스템 알림 페이지</li>
                <li><strong>사용자 프로필:</strong> 직원 정보, 스킬, 프로젝트를 보여주는 프로필 카드</li>
                <li><strong>환영 이메일:</strong> 신규 사용자를 위한 환영 이메일 템플릿</li>
                <li><strong>완전한 예제:</strong> 실제 비즈니스 환경에서 사용할 수 있는 완성도 높은 템플릿</li>
            </ul>
        </div>
    </div>

    <div class="info-section">
        <h3 class="info-title">템플릿 추가 방법</h3>
        <div class="info-content">
            <p><strong>Controller에서 templates 데이터 제공:</strong></p>
            <pre style="background: #f8f9fa; padding: 15px; border-radius: 5px; overflow-x: auto;"><code>List&lt;Map&lt;String, String&gt;&gt; templates = Arrays.asList(
    Map.of("name", "내 템플릿", "template", "my-template.ftl", "url", "/preview/my-template"),
    Map.of("name", "다른 템플릿", "template", "other.ftl", "url", "/preview/other")
);
model.addAttribute("templates", templates);</code></pre>
            <p>또는 간단히 문자열 배열:</p>
            <pre style="background: #f8f9fa; padding: 15px; border-radius: 5px;"><code>List&lt;String&gt; templates = Arrays.asList("invoice", "report", "profile");
model.addAttribute("templates", templates);</code></pre>
        </div>
    </div>

    <div class="footer">
        <p>🔧 Spring Boot + FreeMarker Template Engine</p>
        <p>생성 시간: ${.now?string("yyyy-MM-dd HH:mm:ss")}</p>
        <p><small style="color: #999;">
                템플릿 개수:
                <#if templates?? && templates?has_content>
                    ${templates?size}개 (동적)
                <#else>
                    5개 (기본값)
                </#if>
            </small></p>
    </div>
</div>

<script type="text/javascript">
    // 템플릿 링크 클릭 시 새 창 크기 조정
    document.addEventListener('DOMContentLoaded', function () {
        const templateLinks = document.querySelectorAll('.template-link');
        templateLinks.forEach(link => {
            link.addEventListener('click', function (e) {
                // API 미리보기 링크인 경우 새 창 크기 설정
                if (this.href.includes('/api/template/preview/')) {
                    e.preventDefault();
                    window.open(this.href, 'template-preview', 'width=1000,height=800,scrollbars=yes,resizable=yes');
                }
            });
        });
    });
</script>
</body>
</html>