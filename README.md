# AgiTQ-Android

Android, 웹 및 Cloudflare Worker용으로 포팅한 Scriptable v3.0-γ 전략 프로젝트입니다.

## 프로젝트 구조

- `web/` — GitHub Pages 대시보드
- `android/` — Android 앱 및 독립형 SPX/QQQ/FGI 홈 화면 위젯
- `cloudflare/` — Cloudflare Worker API 프록시 및 공통 전략 계산
- `.github/workflows/` — 웹 배포 및 APK 빌드를 위한 GitHub Actions

## Scriptable에서 보존한 전략

- 소형: CNN 공포와 탐욕 지수 게이지
- 중형: QQQ 200일 단순이동평균선(SMA) ±2.0% 밴드 + 단일 TS -25%
- 대형: SPX 200일 단순이동평균선(SMA) ±2.5% 밴드 + 다단계 TS -25% + CNN FGI
- 현재 사이클 최고점 대비 TQQQ 낙폭
- 한국어 행동/상태 메시지 및 원본 매수·매도 단계

## 초기 설정

### 1. Cloudflare Worker

`cloudflare/worker.js`를 Cloudflare Worker로 배포합니다. 공개 엔드포인트는 다음 형식이어야 합니다.

`https://YOUR-WORKER.workers.dev/api/market`

Worker는 Yahoo Finance와 CNN FGI 데이터를 가져와 검증된 하나의 JSON 응답을 웹사이트와 Android 위젯에 전달합니다. 5분 동안 엣지 캐시를 유지하며, 외부 데이터 제공처에 일시적인 장애가 발생했을 때 사용할 수 있도록 마지막 정상 응답을 7일간 보존합니다.

Yahoo와 CNN 요청은 동시에 실행됩니다. SPX/QQQ와 TQQQ의 과거 데이터는 뉴욕 거래일을 기준으로 정렬하므로 실시간 시세의 작은 타임스탬프 차이 때문에 Scriptable 전략 계산에서 현재 거래일이 누락되지 않습니다.

### 2. 웹 대시보드 연결

`web/js/api.js`를 열고 다음 주소를 찾습니다.

`https://YOUR-WORKER.workers.dev`

위 주소를 실제 Worker URL로 변경합니다.

### 3. Android 앱 및 위젯 연결

`android/app/src/main/java/com/agitq/android/AgiTQConfig.kt`를 열고 동일한 Worker URL 자리표시자를 실제 주소로 변경합니다.

GitHub 사용자 이름이나 저장소 경로가 다른 경우 `android/app/src/main/java/com/agitq/android/MainActivity.kt`에 있는 GitHub Pages URL도 변경합니다.

### 4. GitHub Pages

`main` 브랜치에 변경 사항이 푸시되면 `deploy-web.yml` 워크플로가 `web/`을 GitHub Pages에 배포합니다. GitHub에서 Pages 소스를 선택하라는 안내가 표시되면 저장소 설정에서 소스를 **GitHub Actions**로 지정합니다.

### 5. APK

`build-apk.yml` 워크플로는 Android 단위 테스트, 린트, JavaScript 구문 검사 및 `android/app/build/outputs/apk/release/app-release.apk` 빌드를 수행한 뒤 결과물을 GitHub Actions 아티팩트로 업로드합니다.

## 중요 사항

전략 로직, 한국어 문구, 색상, 차트 범위 및 신호 임계값은 제공된 Scriptable 코드와 의도적으로 최대한 동일하게 유지합니다. 원본 Scriptable 소스는 비교용으로만 사용하며 수정하지 않습니다.

Android는 검증된 데이터 스냅샷만 저장합니다. 자동 및 수동 새로고침은 API를 한 번만 요청한 뒤 동일한 스냅샷으로 위젯 3개를 모두 갱신합니다. 새로고침에 실패하면 마지막으로 정상 표시된 화면을 그대로 유지합니다. 자동 새로고침은 WorkManager를 통해서만 실행되며 미국 정규장 시간에만 동작합니다. 수동 새로고침은 언제든 사용할 수 있습니다.

릴리스 APK에는 R8 및 리소스 축소가 적용되며, 웹 대시보드는 화면이 보이지 않을 때 5분 새로고침 타이머를 일시 정지합니다. 이러한 최적화는 화면에 표시되는 Scriptable 전략 값이나 UI 비율을 변경하지 않습니다.
