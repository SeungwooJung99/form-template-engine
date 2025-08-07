<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>환영합니다!</title>
    <style>
        body { font-family: 'Malgun Gothic', Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
        .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        .header { text-align: center; margin-bottom: 30px; padding-bottom: 20px; border-bottom: 2px solid #007bff; }
        .welcome-title { color: #007bff; font-size: 28px; margin: 0; }
        .content { line-height: 1.6; color: #333; }
        .highlight { background-color: #e3f2fd; padding: 15px; border-radius: 5px; margin: 20px 0; }
        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; text-align: center; color: #666; font-size: 14px; }
        .button { display: inline-block; background-color: #007bff; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; margin: 15px 0; }
        .info-list { background-color: #f8f9fa; padding: 15px; border-left: 4px solid #007bff; margin: 15px 0; }
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <h1 class="welcome-title">${companyName}에 오신 것을 환영합니다!</h1>
    </div>

    <div class="content">
        <p>안녕하세요 <strong>${userName}</strong>님,</p>

        <p>${companyName}의 새로운 멤버가 되어주셔서 진심으로 감사드립니다.
            ${.now?string("yyyy년 MM월 dd일")}부로 귀하의 계정이 성공적으로 생성되었습니다.</p>

        <div class="highlight">
            <h3>🎉 환영 혜택</h3>
            <p>신규 회원님께 특별한 혜택을 준비했습니다:</p>
            <ul>
                <#list welcomeBenefits as benefit>
                    <li>${benefit}</li>
                </#list>
            </ul>
        </div>

        <div class="info-list">
            <h3>📋 계정 정보</h3>
            <ul>
                <li><strong>이메일:</strong> ${userEmail}</li>
                <li><strong>가입일:</strong> ${registrationDate?string("yyyy-MM-dd HH:mm")}</li>
                <li><strong>회원 등급:</strong> ${membershipLevel!"일반"}</li>
                <#if department??>
                    <li><strong>부서:</strong> ${department}</li>
                </#if>
            </ul>
        </div>

        <p>시작하려면 아래 버튼을 클릭하세요:</p>
        <div style="text-align: center;">
            <a href="${loginUrl}" class="button">지금 시작하기</a>
        </div>

        <p>궁금한 점이 있으시면 언제든 고객지원팀(${supportEmail})으로 연락주세요.</p>

        <p>다시 한 번 환영합니다!</p>
    </div>

    <div class="footer">
        <p>&copy; ${.now?string("yyyy")} ${companyName}. All rights reserved.</p>
        <p>이 이메일은 ${userEmail}로 발송되었습니다.</p>
    </div>
</div>
</body>
</html>