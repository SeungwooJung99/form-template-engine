<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${document.title!"동적 데이터 바인딩 예시"}</title>

    <!-- FTL에서 메타 데이터를 JSON으로 임베딩 -->
    <script id="template-metadata" type="application/json">
        {
            "templateId": "${template.id!'dynamic-form'}",
        "version": "${template.version!'1.0.0'}",
        "dataBindings": {
        <#-- 외부 서버에서 치환해야 할 변수들 정의 -->
        "user": {
            "endpoint": "/api/users/${user.id!'current'}",
                "fields": ["name", "email", "profile.avatar", "preferences.theme"],
                "fallback": {
                    "name": "${user.name!''}",
                    "email": "${user.email!''}",
                    "profile.avatar": "${user.profile.avatar!''}",
                    "preferences.theme": "${user.preferences.theme!'light'}"
                }
            },
            "notifications": {
                "endpoint": "/api/notifications/count",
                "fields": ["unread", "total"],
                "fallback": {
                    "unread": ${notifications.unread!0},
                    "total": ${notifications.total!0}
        }
    },
    "dashboard": {
        "endpoint": "/api/dashboard/stats",
        "fields": ["sales.today", "sales.month", "users.active"],
        "fallback": {
            "sales.today": ${dashboard.sales.today!0},
                    "sales.month": ${dashboard.sales.month!0},
                    "users.active": ${dashboard.users.active!0}
        }
    }
},
"lifecycle": {
    "onReady": true,
    "onDestroy": true,
    "onDataBound": true
},
"external": {
    "serverUrl": "${external.serverUrl!''}",
            "apiKey": "${external.apiKey!''}",
            "timeout": ${external.timeout!5000}
        }
    }
    </script>

    <style>
        .loading-overlay {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(255, 255, 255, 0.9);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 9999;
        }

        .spinner {
            width: 40px;
            height: 40px;
            border: 4px solid #f3f3f3;
            border-top: 4px solid #3498db;
            border-radius: 50%;
            animation: spin 1s linear infinite;
        }

        @keyframes spin {
            0% {
                transform: rotate(0deg);
            }
            100% {
                transform: rotate(360deg);
            }
        }

        .data-placeholder {
            background: #f8f9fa;
            padding: 4px 8px;
            border-radius: 4px;
            font-family: monospace;
            color: #6c757d;
        }

        .error-state {
            color: #dc3545;
            font-style: italic;
        }
    </style>
</head>
<body>
<!-- 로딩 오버레이 -->
<div id="loadingOverlay" class="loading-overlay">
    <div>
        <div class="spinner"></div>
        <p>데이터를 불러오는 중...</p>
    </div>
</div>

<!-- 메인 컨텐츠 -->
<div id="mainContent" style="display: none;">
    <header class="header">
        <h1>대시보드</h1>
        <div class="user-info">
            <img id="userAvatar"
                 src="data-bind:user.profile.avatar"
                 alt="프로필 이미지"
                 width="40" height="40"
                 style="border-radius: 50%;"/>
            <span>안녕하세요, <span data-bind="user.name" class="data-placeholder">${user.name!'사용자'}</span>님!</span>
            <span class="email" data-bind="user.email" class="data-placeholder">(${user.email!''})</span>
        </div>
        <div class="notifications">
            알림: <span data-bind="notifications.unread" class="badge">${notifications.unread!0}</span>
        </div>
    </header>

    <main class="dashboard">
        <div class="stats-grid">
            <div class="stat-card">
                <h3>오늘 매출</h3>
                <p class="amount" data-bind="dashboard.sales.today" data-format="currency">
                    ${dashboard.sales.today!0}
                </p>
            </div>

            <div class="stat-card">
                <h3>이번 달 매출</h3>
                <p class="amount" data-bind="dashboard.sales.month" data-format="currency">
                    ${dashboard.sales.month!0}
                </p>
            </div>

            <div class="stat-card">
                <h3>활성 사용자</h3>
                <p class="count" data-bind="dashboard.users.active" data-format="number">
                    ${dashboard.users.active!0}
                </p>
            </div>
        </div>

        <div class="user-preferences">
            <h3>설정</h3>
            <div class="theme-setting" data-bind="user.preferences.theme">
                현재 테마: <span class="theme-indicator">${user.preferences.theme!'light'}</span>
            </div>
        </div>
    </main>
</div>

<!-- 데이터 바인딩 및 생명주기 관리 JavaScript -->
<script>
    class DynamicDataBinder {
        constructor() {
            this.metadata = null;
            this.boundData = {};
            this.isReady = false;
            this.init();
        }

        async init() {
            try {
                // 메타데이터 로딩
                const metadataScript = document.getElementById('template-metadata');
                this.metadata = JSON.parse(metadataScript.textContent);

                console.log('🚀 Template initialized:', this.metadata.templateId);

                // 외부 서버에서 데이터 바인딩
                await this.bindExternalData();

                // onReady 이벤트 호출
                this.onReady();

            } catch (error) {
                console.error('❌ Initialization failed:', error);
                this.onError(error);
            }
        }

        async bindExternalData() {
            const {dataBindings, external} = this.metadata;

            console.log('🔄 Starting data binding...');

            for (const [key, binding] of Object.entries(dataBindings)) {
                try {
                    console.log(`📥 Fetching ${key} from ${binding.endpoint}`);

                    // 외부 서버에서 데이터 조회
                    const data = await this.fetchExternalData(binding.endpoint, external);

                    if (data) {
                        this.boundData[key] = data;
                        this.updateUIElements(key, data, binding.fields);
                        console.log(`✅ ${key} data bound successfully`);
                    } else {
                        // 폴백 데이터 사용
                        console.log(`⚠️ Using fallback data for ${key}`);
                        this.boundData[key] = binding.fallback;
                        this.updateUIElements(key, binding.fallback, binding.fields);
                    }

                } catch (error) {
                    console.error(`❌ Failed to bind ${key}:`, error);
                    // 폴백 데이터 사용
                    this.boundData[key] = binding.fallback;
                    this.updateUIElements(key, binding.fallback, binding.fields);
                }
            }

            console.log('🎉 Data binding completed');
            this.onDataBound();
        }

        async fetchExternalData(endpoint, config) {
            const url = `${config.serverUrl}${endpoint}`;

            const options = {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${config.apiKey}`,
                    'X-Template-ID': this.metadata.templateId
                },
                timeout: config.timeout
            };

            try {
                const response = await fetch(url, options);

                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }

                return await response.json();

            } catch (error) {
                console.error(`Network error for ${endpoint}:`, error);
                return null;
            }
        }

        updateUIElements(dataKey, data, fields) {
            // data-bind 속성을 가진 모든 요소 업데이트
            const elements = document.querySelectorAll(`[data-bind^="${dataKey}."]`);

            elements.forEach(element => {
                const bindPath = element.getAttribute('data-bind');
                const value = this.getNestedValue(data, bindPath.replace(`${dataKey}.`, ''));

                if (value !== undefined) {
                    this.setElementValue(element, value);
                    element.classList.remove('data-placeholder');
                    element.classList.remove('error-state');
                } else {
                    element.classList.add('error-state');
                    console.warn(`⚠️ No value found for ${bindPath}`);
                }
            });

            // 이미지 src 업데이트 (특별 처리)
            const imgElements = document.querySelectorAll(`[src^="data-bind:${dataKey}."]`);
            imgElements.forEach(img => {
                const bindPath = img.src.replace(`data-bind:${dataKey}.`, '');
                const value = this.getNestedValue(data, bindPath);
                if (value) {
                    img.src = value;
                }
            });
        }

        getNestedValue(obj, path) {
            return path.split('.').reduce((current, key) =>
                current && current[key] !== undefined ? current[key] : undefined, obj);
        }

        setElementValue(element, value) {
            const format = element.getAttribute('data-format');
            let displayValue = value;

            // 포맷팅 적용
            if (format === 'currency') {
                displayValue = new Intl.NumberFormat('ko-KR', {
                    style: 'currency',
                    currency: 'KRW'
                }).format(value);
            } else if (format === 'number') {
                displayValue = new Intl.NumberFormat('ko-KR').format(value);
            }

            if (element.tagName === 'INPUT') {
                element.value = displayValue;
            } else {
                element.textContent = displayValue;
            }
        }

        // 🎯 생명주기 이벤트들
        onReady() {
            console.log('🎯 onReady: Template is ready and data is bound');

            // 로딩 오버레이 숨기기
            document.getElementById('loadingOverlay').style.display = 'none';
            document.getElementById('mainContent').style.display = 'block';

            this.isReady = true;

            // 커스텀 이벤트 발생
            document.dispatchEvent(new CustomEvent('templateReady', {
                detail: {
                    templateId: this.metadata.templateId,
                    boundData: this.boundData
                }
            }));

            // 추가 초기화 로직
            this.initializeInteractions();
        }

        onDataBound() {
            console.log('📊 onDataBound: All data binding completed');

            // 데이터 바인딩 완료 이벤트
            document.dispatchEvent(new CustomEvent('dataBound', {
                detail: {
                    boundData: this.boundData,
                    timestamp: new Date().toISOString()
                }
            }));
        }

        onDestroy() {
            console.log('💀 onDestroy: Cleaning up template');

            // 이벤트 리스너 정리
            this.cleanupEventListeners();

            // 타이머 정리
            if (this.refreshTimer) {
                clearInterval(this.refreshTimer);
            }

            // 커스텀 이벤트 발생
            document.dispatchEvent(new CustomEvent('templateDestroy', {
                detail: {
                    templateId: this.metadata.templateId
                }
            }));
        }

        onError(error) {
            console.error('💥 onError:', error);

            // 오류 UI 표시
            document.getElementById('loadingOverlay').innerHTML = `
                <div style="text-align: center;">
                    <h3 style="color: #dc3545;">오류가 발생했습니다</h3>
                    <p>${error.message}</p>
                    <button onclick="location.reload()">다시 시도</button>
                </div>
            `;
        }

        initializeInteractions() {
            // 실시간 데이터 업데이트 (30초마다)
            this.refreshTimer = setInterval(() => {
                console.log('🔄 Refreshing data...');
                this.bindExternalData();
            }, 30000);

            // 사용자 상호작용 이벤트
            document.addEventListener('click', (e) => {
                if (e.target.hasAttribute('data-bind')) {
                    console.log('🖱️ Clicked data-bound element:', e.target.getAttribute('data-bind'));
                }
            });
        }

        cleanupEventListeners() {
            // 등록된 이벤트 리스너들 정리
            document.removeEventListener('click', this.clickHandler);
        }

        // 공개 API
        refresh() {
            console.log('🔄 Manual refresh triggered');
            return this.bindExternalData();
        }

        getData(key) {
            return this.boundData[key];
        }

        updateData(key, newData) {
            this.boundData[key] = newData;
            const binding = this.metadata.dataBindings[key];
            if (binding) {
                this.updateUIElements(key, newData, binding.fields);
            }
        }
    }

    // 전역 인스턴스 생성
    let templateBinder;

    // DOM 로딩 완료 후 초기화
    document.addEventListener('DOMContentLoaded', () => {
        templateBinder = new DynamicDataBinder();
    });

    // 페이지 언로드 시 정리
    window.addEventListener('beforeunload', () => {
        if (templateBinder) {
            templateBinder.onDestroy();
        }
    });

    // 외부에서 사용할 수 있는 전역 함수들
    window.TemplateAPI = {
        refresh: () => templateBinder?.refresh(),
        getData: (key) => templateBinder?.getData(key),
        updateData: (key, data) => templateBinder?.updateData(key, data),
        isReady: () => templateBinder?.isReady || false
    };

    // FTL에서 추가로 정의된 커스텀 로직
    <#if customJavaScript??>
    ${customJavaScript}
    </#if>
</script>

<!-- 외부 서버에서 추가 스크립트 로딩 (선택적) -->
<#if external.additionalScripts?? && external.additionalScripts?has_content>
    <#list external.additionalScripts as script>
        <script src="${script}" defer></script>
    </#list>
</#if>
</body>
</html>