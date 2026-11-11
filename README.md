# Convoy

**친구·가족 차량을 서로 동의해 연결하고, 함께 이동할 때는 목적지를, 평소에는 안전을 나누는 차량용 그룹 내비게이션** (Pleos Connect 앱)

- **목적지로 함께**: 리더가 목적지를 바꾸면 그룹 모든 차의 현대차 내비가 함께 경로를 다시 잡습니다. 현대차 내비에서 검색해 정한 목적지를 그대로 그룹과 공유할 수 있습니다.
- **앞차 따라가기**: 목적지 없이 멤버 차를 따라갑니다. 앞차가 200m 이상 움직이면 내 내비가 경로를 다시 잡습니다.
- **도착·출발 공유**: 기어(P/D)와 내비 도착 이벤트로 멤버의 출발·도착을 공유합니다.
- **가족 안심**: 에어백·긴급호출 경고등이 켜지면 운전자에게 먼저 "괜찮으세요?"를 묻고, 30초 동안 응답이 없으면 가족에게 위치와 경고등을 알립니다.
- **주행 중 조작 제한**: 설정은 정차(P) 중에만 하고, 주행 중에는 꼭 필요한 조작만 허용합니다.

## 실행 방법

### 준비
- Android Studio(최신 안정판)와 JDK 17 이상. 개발은 JDK 24로 했습니다.
- Gradle Wrapper 9.4.1, Android Gradle Plugin 9.2.1, Kotlin 2.2.10, Jetpack Compose(BOM 2025.09.00)
- Pleos SDK는 `settings.gradle.kts`에 등록된 저장소(`https://nexus-playground.pleos.ai/repository/maven-releases/`)에서 받습니다. 별도 키는 필요 없습니다.

### 빌드와 설치
```bash
# JDK 경로는 환경에 맞게 바꿉니다.
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-24.jdk/Contents/Home

./gradlew :app:assembleDebug                      # 빌드
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.car.convoy/.MainActivity
```

### 단위 테스트
```bash
./gradlew :app:testDebugUnitTest
```
규칙(그룹 상태, 목적지 동기화, 재탐색 기준, 사고 의심 판단, 주행 중 조작 제한 등)은 SDK와 분리된 순수 함수이고 단위 테스트로 확인합니다. 명세는 [`SPEC.md`](SPEC.md)에 있습니다.

## 테스트 환경 및 필요한 설정

### 에뮬레이터
- Android Studio의 Device Manager에서 Pleos Connect 에뮬레이터 AVD(`Pleos_Connect_v2.1.0`)를 만듭니다.
- **Cold boot**로 실행합니다. Quick boot로 켜면 차량 신호(VHAL)를 쓸 수 없습니다. RAM은 4GB 이상을 권장합니다.

### Virtual Console (기어·경고등 조작)
```bash
adb -s emulator-5554 forward tcp:8088 tcp:8088
# 브라우저에서 http://127.0.0.1:8088/ 을 엽니다. 페이지를 연 뒤에야 콘솔이 차량 신호를 보냅니다.
```
- **기어**: Emulation Controls에서 P/D를 바꿉니다. P → D이면 출발로 보고, D에서는 설정이 잠깁니다.
- **경고등**: Airbag을 On으로 바꾸면 사고 의심 확인("괜찮으세요?")이 시작됩니다.

### 앱 설정과 퍼미션
- 퍼미션: `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`(경로 요청·취소), `CAR_DRIVE`(기어), `CAR_SAFETY`, `PRIVATE_CAR_SAFETY`(경고등). 에뮬레이터에서는 자동으로 허용됩니다. 실제 차량에서는 `PRIVATE_CAR_SAFETY` 승인이 필요할 수 있습니다.
- 주행 중에도 화면이 보이도록 `distractionOptimized`를 선언했습니다.
- [내비에서 검색하기]는 현대차 내비 앱(에뮬레이터 패키지 `ai.umos.maps.android.navigation.app`)을 띄웁니다. 그래서 매니페스트 `<queries>`에 이 패키지를 적었습니다.
- 사용 SDK: NaviHelper 3.0.5, Vehicle 2.3.2

### 시연용 가상 데이터
서버가 없어서 연결된 사람(민수·현우·수진·지영·서준)과 그들의 응답·출발·도착·위치는 앱 안의 가상 데이터입니다. 화면의 점선 **"시연용"** 패널에서 수락·거절, 출발·도착, 앞차 이동을 만듭니다. 내 차의 내비 경로 요청·취소, 기어, 경고등, 위치는 실제 SDK로 동작합니다.

녹화나 자동 시연에는 adb 명령도 쓸 수 있습니다(명령 사이에 2초 이상 간격을 둡니다).
```bash
adb -s emulator-5554 shell am start -n com.car.convoy/.MainActivity --es demo <명령>
# 홈:    invite:p1, accept, decline, start, disband, status:busy, family:p1, faccept, fdecline, frelease
# 주행:  dest:서울역, depart:p1, arrive:p1, retry, home, resume, end
# 따라가기: lead:p1, laccept, ldecline, move:100, unfollow
# 사고:  airbag:on, airbag:off, ok
```

## 개발에 사용한 에뮬레이터와 화면 규격
| 항목 | 값 |
|---|---|
| 에뮬레이터 | Pleos Connect Emulator 2.1.0 |
| 시스템 이미지 | Android 14 (API 34) |
| 화면 | 2560 × 1440 가로, 160dpi (1dp = 1px) |
| 앱 영역 | 앱 하나일 때 가로 약 1,700dp. 화면을 나누면(1:1, 2:1) 좁은 창이 됩니다 |
| 좁은 창 | 앱 영역 가로가 1,200dp 미만이면 1열로 쌓고 손으로 스크롤합니다 |
| 앱 SDK 수준 | minSdk 28, targetSdk 36 |

## APK
- 위치: `apk/<팀명>_Convoy_release.apk` (준비 중, 팀명이 정해지면 추가합니다)
- GitHub Release: 준비 중

## 알려진 한계
- 다른 차는 모두 가상이고, 실제 여러 차를 잇는 서버는 없습니다.
- 에뮬레이터는 내 차 위치가 고정이라, 내 차 도착과 "모두 도착했어요"는 시연하지 못합니다.
- 실제 차량에서 확인이 필요한 것: 주행 중 화면 표시(`distractionOptimized`) 허용, 사고 의심 때 화면을 앞으로 가져오기, 현대차 내비 앱 패키지 이름, 도착 판정의 주소 비교. 자세한 내용은 [`docs/sdk-findings.md`](docs/sdk-findings.md)에 있습니다.
