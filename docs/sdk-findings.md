# SDK 시험 결과

2026-11-06, Pleos Connect Emulator 2.1.0(API 34)에서 확인한 내용이다. 문서에 없는 것은 "미확인"으로 적는다.
상세 배경과 기획 영향은 개인 노트(dev-notes `02-프로젝트/Convoy.md`)에 있다.

## 사용 가능 여부 (콘솔 My Project > SDK)
| SDK | 상태 |
|---|---|
| Vehicle State | 사용 가능 |
| NaviHelper | 사용 가능 |
| FusedLocation, TextToSpeech, SpeechToText, Vehicle Control | **Coming Soon (사용 불가)** |

## 빌드 설정
- `settings.gradle.kts`의 `dependencyResolutionManagement.repositories`에 `maven("https://nexus-playground.pleos.ai/repository/maven-releases/")`
- 의존성: `ai.pleos.playground:NaviHelper:3.0.5`, `ai.pleos.playground:Vehicle:2.3.2`
- AGP 9.2.1, Gradle 9.4.1, JDK 24, minSdk 28에서 빌드 성공
- 퍼미션(Manifest): `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`, `CAR_DRIVE`, `CAR_LIGHTS`, `CAR_ENERGY`, `CAR_SAFETY`, `PRIVATE_CAR_SAFETY` (모두 `pleos.car.permission.` 접두사)
- Vehicle 퍼미션은 App Market 사전 승인 범위에서만 쓸 수 있다. `PRIVATE_*`는 제한 퍼미션이라 실제 차량에서는 승인이 필요할 가능성이 높다

## NaviHelper (`ai.pleos.playground.navi`)
- 사용: `NaviHelper(context).initialize(onSuccess, onFailure)` → `addListener(NaviHelperEventListener)` → 호출 → `removeListener`, `release`
- 위치: `getCurrentLocationInfo()` → `onCurrentLocationInfo(CurrentLocationInfo)`: 위도, 경도, heading, 주소, routeId. **푸시가 아니라 폴링**이며 2초 간격으로 2초마다 응답했다
- 목적지와 ETA: `requestRoute(RouteInfo)` 후 `getDestinationInfo()` → `DestinationInfo.remainDistance`(m), `remainTime`(초)
- 경로 제어: `cancelRoute()`, `requestReRoute()`, `changeRouteOption()`, `addWaypoint()`(최대 2개), `removeWaypoint()`
- 이벤트: `onRouteStarted`, `onNavigationStatus`, `onDrivingInfo`, `onDestinationArrived`, `onDestinationChanged`, `onRouteCancelled`, `onError`
- 경로를 요청하면 안내가 시작된다. 내 앱이 이미 전면이면 내비 앱이 전면을 가져가지 않았다(아래 "그룹 주행 검증" 참고, 이전 관찰과 다르다). 내 앱이 백그라운드여도 콜백은 계속 왔다
- 다른 차의 목적지를 받는 기능은 SDK에 없다. 자체 서버로 목적지를 전달하고 각 차가 `requestRoute`를 호출한다

## Vehicle (`ai.pleos.playground.vehicle`)
| 기능 | API | 결과 |
|---|---|---|
| 기어 | `getGear().registerGearSelection()` → `onSelected(VehicleGear)` | 동작. `GEAR_PARK`, `GEAR_DRIVE` 등. 문서에는 없지만 SDK에 있다 |
| 방향지시등 | `getTurnSignal().registerTurnSignalState()` → `TurnSignalValue` | 동작. `LEFT`, `RIGHT`, `OFF` |
| 경고등 | `getSafety().registerTelltales()` → `onTelltaleInfoUpdated(Telltales)` | 동작. `PRIVATE_CAR_SAFETY` 필요 |
| EV 배터리 | `getEvBattery().getEvBatteryStateOfCharge()` | 동작 |
| 브레이크 페달, 주행거리 | `getBrake()`, `getOdometer()` | 동작 |
| 타이어 | `getTire().getLowTirePressure(WheelArea)` | 값 137.0이 옴. 의미 불명 |
| 연료 | `getFuel().getFuelPercent()` | 미지원(에뮬레이터 차량이 EV) |
| 에어백 전개 | `getSafety().hasSeatAirbagBeenDeployed()` | 미지원 |
| 차속 | - | 읽는 API를 찾지 못함 |

- 리스너 콜백의 인자는 nullable이고, 리스너는 `onFailed(Exception)`도 구현해야 한다
- 경고등 종류에 `AIRBAG`, `EMERGENCY_CALL`, `ABS`, `TPMS`, `TURN_SIGNAL_LEFT/RIGHT`, `OVER_SPEED`, `MASTER_WARNING` 등이 있다
- `Vehicle.initialize()` 후 사용하고 종료 시 `release()`

## 에뮬레이터
- AVD `Pleos_Connect_v2.1.0`: API 34, Cold boot 필수(Quick boot면 차량 신호 불가), RAM 4GB, 화면 2560x1440 가로(왼쪽 차량 정보, 오른쪽 앱 영역)
- Virtual Console: `adb forward tcp:8088 tcp:8088` 후 `http://127.0.0.1:8088/`의 Emulation Controls에서 기어, 방향지시등, 경고등 등을 바꾼다. 위치(GPS)와 차속 카드는 없다
- **위치가 성남시 수정구 시흥동(37.4127, 127.0947)에 고정**이고 heading은 0이다. `adb emu geo fix`로 바꿔도 NaviHelper 위치는 변하지 않았다
- 미확인: 에뮬레이터 2대 동시 실행, `onDestinationArrived` 실제 수신(아래 참고: 에뮬레이터에서 유도하지 못함), `addWaypoint`, `updateOccupiedRegion`의 동작

## Compose (작업 0, 2026-11-06 확인)
- **Jetpack Compose가 Pleos 에뮬레이터에서 동작한다.** 텍스트 렌더링과 버튼 클릭(상태 변경)을 확인했다
- 설정: Kotlin 컴파일러 플러그인 `org.jetbrains.kotlin.plugin.compose` 2.2.10(AGP 9.2.1 내장 Kotlin과 일치), Compose BOM 2025.09.00, `androidx.activity:activity-compose:1.10.1`, Material3, `buildFeatures { compose = true }`
- Activity는 `ComponentActivity` + `setContent`. 기존 `Theme.Convoy`(MaterialComponents)로도 문제없이 떴다
- **화면 밀도가 160dpi(mdpi)라 1dp = 1px이고 화면 폭이 약 2560dp다.** 기본 크기(버튼 높이 약 40dp, 글자 16~32sp)는 매우 작게 보인다. 주행 중 사용을 위해 터치 영역과 글자를 크게(예: 버튼 높이 80dp 이상, 본문 28sp 이상) 잡아야 한다

## 앱 제약 (Pleos 가이드)
- 주행 중에는 CarUxRestrictions를 확인해 제한 기능을 줄이거나 끈다. 영상 재생 금지, 애니메이션 최소화, 큰 터치 영역, 텍스트 최소화
- 응답 0.25초 이내, 앱 실행 10초 이내, CPU는 주행 중 비내비 앱 5% 미만, RAM 400MB 이하
- 레터박스와 호환성 모드 금지. `screenOrientation`을 지정하지 않으면 세로 단일 window이고, `landscape`면 2개 window를 쓴다

## 그룹 주행 검증 (2026-11-08, `spike/group-probe`, Pleos Emulator 2.1.0)
그룹 주행(R5 목적지 동기화, R7 도착 공유)이 SDK 위에서 되는지 확인한 결과다. 시험 코드는 `spike/group-probe`에만 있고 머지하지 않는다.
시험은 `adb shell am start -n com.car.convoy/.MainActivity --es probe <이름>`으로 한 번에 하나씩 수동으로 실행하고 `logcat -s ConvoyTest`로 읽었다. 호출 순서, 대기 시간, "취소 없이"라는 조건은 코드가 아니라 사람이 정한 것이다. 시간은 logcat 타임스탬프를 손으로 비교한 값이다.

| 확인 | 결과 |
|---|---|
| 안내 중 `cancelRoute` 없이 새 `requestRoute` | **된다.** 순서: `routeA`(강남역)로 `ROUTE_DRIVE`가 된 뒤 `cancel` 없이 `routeB`(서울역) 실행. 기존 경로가 자동 취소되고(`onRouteCancelled`, 내비 상태 `FREE_DRIVE`) 새 경로가 시작되며(`onRouteStarted`, `ROUTE_DRIVE`), `getDestinationInfo`도 서울역으로 바뀌었다. 요청부터 `onRouteStarted`까지 4회 측정에서 5.3~6.7초 |
| `onDestinationArrived` | **에뮬레이터에서 유도하지 못했다.** 위치가 고정이라 차가 움직이지 않는다. `routeHere`(현재 위치 좌표, 남은 거리 2m)와 `routeNear`(약 40m 떨어진 좌표, 남은 거리 10m)를 각각 요청하고 약 20초 동안 지켜봤지만 도착 이벤트가 오지 않았고 남은 거리도 그대로였다. SDK에서 도착이 안 된다는 뜻이 아니다. 실차 또는 위치를 움직일 수 있는 다른 방법으로 추가 확인이 필요하다 |
| 외부 통신 | 앱에서 호스트 PC의 `http://10.0.2.2:8099/ping`과 `https://example.com`에 요청했고, 두 로그 모두 응답 코드 200이 기록됐다. 호스트에서는 `ping` 파일(내용 `pong`)이 있는 디렉터리에서 `python3 -m http.server 8099 --bind 0.0.0.0`을 실행했다. `INTERNET` 퍼미션과 평문 HTTP를 위한 `usesCleartextTraffic`이 필요하다. probe는 응답 코드를 그대로 찍으므로 로그의 "OK"는 연결 성공을 뜻하고, 200 여부는 로그의 코드로 판단한다 |
| `requestRoute` 후 내비 앱이 전면을 가져가는가 | 가져가지 않았다. 내 앱이 전면인 상태에서 `routeA`를 한 번 보내고 그 뒤 12초 동안 아무 명령도 보내지 않았을 때 내 앱이 계속 앞에 있었다 |
| `requestNavigationScreen(Foreground)` | `pleos.car.permission.NAVI_CUSTOM_MAP`이 없으면 `SecurityException`. 이 퍼미션을 매니페스트에 넣으면 에뮬레이터에서 `granted=true`이고, 호출하면 **Pleos 내비 앱(`ai.umos.maps.android.navigation.app`)이 전면으로 올라와 내 앱이 있던 오른쪽 앱 영역을 대체했다** |
| 에뮬레이터 2대 동시 실행 | 시험하지 않았다 |

### 화면 구성
- 에뮬레이터 화면은 **왼쪽 차량 패널(기어, 속도, 차량 그림)과 오른쪽 앱 영역**이다. 이 시험에서 내 앱과 내비 앱은 같은 오른쪽 영역을 번갈아 썼고 나란히 놓이지 않았다
- 내비 앱이 전면일 때 오른쪽 영역에 목적지명, 남은 거리, 도착 시각, 내 위치 마커, 안내 종료 버튼이 표시됐다
- 확인한 `NaviHelper`와 `navi-data` AAR의 시그니처에서는 지도 위에 다른 차를 그리는 API를 찾지 못했다. SDK 전체에 없다는 확인은 아니다
- `NaviHelper.getInternalApi().updateOccupiedRegion(Rect)`는 존재하지만 문서가 없고 동작은 확인하지 못했다

### 기획에 미치는 영향 (확인된 것과 추론을 구분한다)
- **확인됨**: 단일 에뮬레이터에서 `requestRoute`는 `cancelRoute` 없이 새 목적지로 교체된다. 확정까지 수 초 걸린다
- **추론**: 그룹의 각 멤버 앱이 같은 목적지로 `requestRoute`를 호출하면 목적지 동기화가 된다. 여러 차량 사이의 전달은 시험하지 않았다(2대 동시 실행 미시험, 서버 통신만 확인). UI에는 "재탐색 중" 상태가 필요하다
- **미확인**: 실제 도착 이벤트. 데모에서는 가상 멤버(`FakeConvoyServer`)로 대신하되, 실제 이벤트 수신은 추가 확인이 필요하다고 기획서에 밝힌다
- **확인된 범위에서의 판단**: 일행 위치를 지도에 표시하려면 SDK가 아닌 다른 수단이 필요해 보인다. Convoy 자체 화면은 목록과 상태 표시 중심으로 계획하되, 다른 수단의 가능성은 따로 확인한다

## 작업 14 어댑터 확인 (2026-11-09, `feat/group-adapters`, Pleos Emulator 2.1.0)
`NaviHelperAdapter`, `VehicleGearSource`, `FakeConvoyServer`를 앱에 붙이고 `adb shell am start -n com.car.convoy/.MainActivity --es demo <명령>`(`dest:seoul`, `dest:gangnam`, `depart:m1`, `arrive:m1`)으로 시험했다. 로그는 `logcat -s Convoy`로 읽었다. 그룹은 나(`me`, 리더, 실제 SDK)와 가상 멤버 `m1`, `m2`다.

| 확인 | 결과 |
|---|---|
| `requestRoute` 퍼미션 | `NAVI_ROUTE`만으로는 `SecurityException: Permission [pleos.car.permission.NAVI_ROUTE_SEARCH] is required`. `NAVI_ROUTE_SEARCH`를 추가하자 동작했다 |
| `RouteStartInfo.name` (R11-17) | **요청한 `poiName`이 그대로 왔다.** "서울역"으로 요청 → `name=[서울역]`(3회). 요청부터 `onRouteStarted`까지 6.4~6.7초 |
| 재탐색 중 목적지 재변경 | `dest:gangnam` 2초 뒤 `dest:seoul`: 각 요청마다 `onRouteCancelled`가 왔고 `onRouteStarted`는 서울역 한 번만 왔다. 강남역에 대한 늦은 `onRouteStarted`는 이번에는 오지 않았다(늦은 이벤트가 절대 오지 않는다는 뜻은 아니다). 가상 멤버는 두 번째 요청 기준으로 6초 뒤 `DRIVING`이 됐다 |
| 가상 멤버 출발·도착 | `arrive:m1`, `arrive:m2`로 `ARRIVED`, `SHARE_ARRIVED`. 내 차는 도착 이벤트를 유도하지 못해(R11-8) 그룹은 `ACTIVE`로 남았다 |
| 안내 중 `requestRoute` 뒤 화면 | **안내가 진행 중인 상태에서 새 `requestRoute`를 보내면 1초 안에 내비 앱(`ai.umos.maps.android.navigation.app`)이 전면이 됐다**(2회, `dumpsys activity`의 `topResumedActivity`로 확인). 위 "그룹 주행 검증"의 "가져가지 않았다"는 안내가 없던 상태의 첫 요청이었다. 어느 조건이 차이를 만드는지는 더 확인해야 한다 |
| 기어 구독 | 앱 시작 시 `GEAR_PARK`가 왔다. `registerGearSelection`은 deprecated 경고("use the Google GEAR_SELECTION property")가 나지만 동작한다 |
| Virtual Console 기어 HTTP | 콘솔 페이지가 `PUT /vehicle/gear?gear=Drive`를 쓴다. adb forward 뒤 직접 호출하면 503이고 `/vehicle/state`의 `gear.bridge_ready`가 1분 넘게 `false`였다. 그래서 P→D 출발은 에뮬레이터에서 확인하지 못했고 단위 테스트로만 확인했다 |

## 작업 15 그룹 주행 화면 녹화 중 관찰 (2026-11-10, `feat/group-screen`, Pleos Emulator 2.1.0)
그룹 주행 화면을 에뮬레이터에서 녹화하면서(`adb shell screenrecord`, `input tap`, 시연 명령) 본 것이다.

| 관찰 | 내용 |
|---|---|
| `requestRoute` 뒤 내비 화면 | 내비 앱이 앞으로 와서 **경로 후보 3개(추천 등)와 "안내 시작" 5초 카운트다운**을 보여 준 뒤 자동으로 안내를 시작했다. 요청부터 `onRouteStarted`까지 걸리던 수 초(5.3~6.7초)는 이 카운트다운 때문으로 보인다(추론) |
| 첫 요청에도 내비가 전면 | 에뮬레이터를 새로 켠 뒤 안내가 없던 상태의 첫 `requestRoute`에서도 내비 앱이 앞으로 왔다(1회). 위 "그룹 주행 검증"의 "가져가지 않았다"와 다르다. 조건 차이는 아직 모른다 |
| 부팅 직후 첫 경로 | 부팅 직후 첫 요청은 `onRouteStarted`까지 약 13.6초 걸렸다(1회). 그 뒤 요청은 6~11초 |
| 하단 앱 아이콘으로 복귀 | 내비가 앞에 있을 때 하단의 Convoy(안드로이드) 아이콘을 누르면 Convoy 화면으로 돌아왔다. 다만 내비가 경로 후보를 보여 주는 중에는 바로 돌아오지 않은 적이 있다(1회) |
| 탭 누락 | 녹화 시작 직후의 `input tap` 한 번이 앱에 전달되지 않았다(1회). 시연 녹화에서는 목적지 선택을 시연 명령(`dest:서울역`)으로 보냈다 |

## 작업 20 사고 의심 어댑터 확인 (2026-11-10, `feat/accident-adapter`, Pleos Emulator 2.1.0)
`VehicleSource`(기어와 경고등을 Vehicle 하나로 구독), `NaviHelperAdapter`(위치 요청), `AccidentController`를 앱에 붙이고 확인했다. 그룹은 작업 14·15와 같다.

| 확인 | 결과 |
|---|---|
| 경고등 구독 | `CAR_SAFETY`, `PRIVATE_CAR_SAFETY` 퍼미션으로 `registerTelltales`가 동작했다. 앱 시작 시 목록이 한 번 왔다(사고 의심 항목 없음) |
| Virtual Console 경고등 HTTP | 콘솔 카탈로그의 쓰기 주소는 `PUT /vehicle/telltales/simulator?type=Airbag&state=On&color=Red`다. adb forward 뒤 호출하면 503 `{"error":"emulator_inactive"}`이고 `/vehicle/state`의 `telltales.bridge_ready`, `emulator_active`가 `false`였다(기어 HTTP와 같은 상태). 그래서 이번 확인은 시연 명령(`--es demo airbag:on`)으로 경고등 값을 넣었다. **SDK가 실제 경고등 변화를 보내는 경로는 이번에 확인하지 못했다** |
| 30초 무응답 → 가족 전송 | 확인 시작 10:35:46.996, 전송 10:36:16.997(30.0초). 확인 중 위치를 2초마다 요청해 30초 동안 15번 받았고(고정 위치 37.4127, 127.0947 "수정구 시흥동"), 마지막 값이 보고서에 담겼다 |
| 켜진 채 다시 옴 / 껐다 켬 (R8-6) | 이전 확인 뒤 경고등이 켜진 채 다시 오자 새로 묻지 않았고, 껐다 켜자 새 확인이 시작됐다 |
| [괜찮아요] | 3초 만에 응답하자 전송하지 않았다. 이때 이전 확인의 보고서가 `lastSentReport`에 남아 있던 버그를 발견해 고쳤다 |
| 앱이 스스로 앞으로 나오기 (R8-11) | 내비가 앞에 있는 상태에서 경고등이 들어오자 **Convoy 화면이 앞으로 나왔다**(2회, 스크린샷). 같은 조건에서 경고등이 없으면 내비가 14초 넘게 앞에 있었다(대조 1회). **그런데 시스템 로그는 `Background activity launch blocked ... (BAL_BLOCK)`로 기록했다.** 내비 앱은 `SYSTEM_ALERT_WINDOW` 권한으로 허용(`BAL_ALLOW_SAW_PERMISSION`)됐다. 보이는 결과와 로그가 어긋나므로, 실제 차량에서도 앞으로 나온다고 단정할 수 없다(추가 확인 필요) |

### Virtual Console 브리지와 주행 중 화면 제한 (2026-11-10 추가)
| 확인 | 결과 |
|---|---|
| 콘솔 브리지 활성화 | **브라우저로 `http://127.0.0.1:8088/`을 연 뒤에는** `/vehicle/state`의 `bridge_ready`, `emulator_active`가 `true`가 되고, 경고등(`PUT /vehicle/telltales/simulator?type=Airbag&state=On&color=Red`)과 기어(`PUT /vehicle/gear?gear=Drive`) HTTP가 200으로 동작했다. 페이지를 열기 전에는 503이었다(위 기록). 페이지를 여는 것이 조건인지는 추론이다 |
| 경고등 SDK 경로 | 콘솔에서 Airbag을 On으로 바꾸자 `onTelltaleInfoUpdated`로 `[AIRBAG ON]`이 왔다. HTTP로 Off → On 하자 R8-6대로 새 확인이 시작됐고 30초 뒤 가족 전송까지 이어졌다 |
| 기어 D | `PUT /vehicle/controls?brake=100&speed=0&accelerator=0`로 브레이크를 밟은 뒤 D를 보내야 앱에 `GEAR_DRIVE`가 왔다(브레이크 없이 보낸 1회는 오지 않았다). P→D에서 내 차가 `GEAR_DEPARTED`로 "주행 중"이 됐다 |
| **주행 중 화면 제한** | 기어가 D이면 Pleos가 Convoy 화면 전체를 **"주행 중에는 사용할 수 없습니다."**로 가렸다. 액티비티에 `<meta-data android:name="distractionOptimized" android:value="true"/>`를 넣자 D에서도 화면이 보였고, 목적지 버튼은 R10대로 비활성("정차 중에 리더만 바꿀 수 있어요")이었다. 실제 차량에서 이 설정이 심사 없이 허용되는지는 모른다 |

## 작업 21 사고 확인 화면 확인 (2026-11-10, `feat/accident-screen`, Pleos Emulator 2.1.0)
| 확인 | 결과 |
|---|---|
| 30초 타이머가 일찍 울림 | 확인 시작 21.8초 만에 `Handler.postDelayed`의 30초 예약이 실행됐고, `elapsedRealtime`으로 잰 경과가 30초 미만이라 아무것도 보내지 않고 끝났다(1회). 예약 시계와 측정 시계가 어긋난 것으로 보인다(추론). 일찍 울리면 남은 시간만큼 다시 기다리도록 고친 뒤 30.0초에 전송됐다(3회) |
| 앱 시작 때 이미 켜진 경고등 | 콘솔에서 Airbag이 On인 채 앱을 다시 시작하자 첫 목록에서 바로 "괜찮으세요?"가 떴다(1회, R8-6대로 "의심 아님 → 의심"). 같은 조건에서 뜨지 않은 적도 1회 있어, 시작 직후 첫 목록이 언제 오는지는 확인이 더 필요하다 |
| 버튼 배경 렌더링 | `Modifier.clip()` 뒤에 `background()`를 둔 목적지 버튼이 이번 에뮬레이터 세션에서 배경·테두리 없이 그려졌다. 이전 세션에서는 같은 코드가 그려졌다. `background(color, shape)`로 모양을 직접 주자 그려졌다 |

## 작업 22 친구 목록 화면 확인 (2026-11-10, `feat/friend-list`, Pleos Emulator 2.1.0)
| 확인 | 결과 |
|---|---|
| 목록과 요청 조건 | 가상 친구 4명 중 오프라인(서준)은 나오지 않았다. 업무 중(지영)은 [따라가기 요청]이 비활성이고 "업무 중이라 요청할 수 없어요"가 보였다(R2-2, R2-3, R2-5) |
| 따라가기 흐름 | 민수 [따라가기 요청]을 탭하자 "수락 기다리는 중"과 시연 패널이 나왔고, 다른 친구(현우)의 버튼은 비활성이 됐다. 시연 [수락] → "따라가는 중"과 [끊기]. [끊기] → 모든 버튼이 조건대로 돌아왔다 |
| 주행 중(D) | 콘솔 HTTP `PUT /vehicle/gear?gear=Drive`로 D를 보내자 이번에는 브레이크 없이 한 번에 `GEAR_DRIVE`가 왔다(1회. 작업 20 기록과 다름). D에서 [끊기]와 내 상태 변경은 됐고, 요청 버튼은 "정차 중에만 요청할 수 있어요"로 비활성이었다(R10) |
| 사고 확인 덮개 | 친구 탭에서 `--es demo airbag:on`을 넣자 "괜찮으세요?"가 친구 화면 위에 덮였다(R2-5) |
| 탭 | [친구] [그룹 주행] 탭 전환이 됐다. 기본 탭은 그룹 주행이다(기존 시연 순서 유지) |

## Pleos UI 디자인 가이드 확인 (2026-11-11, 문서 읽기)
출처: https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro 와 하위 페이지(layout, window, colors, typography, basic-button, iconography, segmented-menu(Tabs), system-popup), 주행 안전 설계 https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline
| 항목 | 내용 |
|---|---|
| 앱 창 | 앱 2개를 띄우면 Connect-S는 1:1, Connect-W는 2:1 또는 1:2로 나눈다. 앱은 가로·세로 레이아웃을 모두 지원해야 한다(가로 전용 게임 등 예외). 상태 바, App library, 공조, 키보드는 앱 위 레이어라 앱 창을 잠시 가릴 수 있다 |
| 대비·글꼴 | 텍스트·아이콘·이미지 대비 4.5:1 이상. 권장 글꼴 Asta Sans. 최소 12sp. 중요 정보 글자 높이 5mm 이상, 보조 3mm 이상 |
| 아이콘 | 48×48dp 그리드, Filled·Outlined 두 종류. 텍스트 라벨과 함께 쓴다 |
| 조작 | 입력 응답 0.25초 이내(늦으면 진행 중 표시). 한 손 조작. 터치 시 햅틱 또는 음성 피드백 권장. 키보드 입력보다 프리셋·최근 항목 우선 |
| 주행 중 | CarUxRestrictions로 제한 상태를 확인해 기능을 줄이거나 막는다. 동영상, 움직이는 이미지, 자동 스크롤 금지. 드롭다운·다단 메뉴 남용 금지 |
| 주간·야간 | "조명 환경에 따라 UI 밝기와 대비를 자동 조정"이라고만 적혀 있다. 필수라는 표현은 없고, 앱이 밤을 알아내는 방법(Android uiMode, Pleos API 등)도 가이드에 없다 |
| 확인 못 함 | 버튼 크기·모서리, 색 값, 글자 역할별 크기, 탭·팝업 치수, 상태 바·GNB 높이, 앱 창 dp 크기. 모두 이미지로만 제공된다 |

## 좁은 창 배치 확인 (2026-11-11, `feat/narrow-layout`, Pleos Emulator 2.1.0)
화면 나누기를 에뮬레이터에서 바로 만들지 못해, `adb shell wm density 240`으로 같은 화면을 약 1,140dp로 줄여 확인했다(끝나면 `wm density reset`).
| 확인 | 결과 |
|---|---|
| 1열 배치 | 그룹 화면은 머리줄이 두 줄로 나뉘고, 상태·멤버 다음에 목적지 고르기가 쌓였다. 친구 화면은 요청한 친구 줄 → 시연 패널 → 내 상태 → 나머지 친구 순이었다(R11-20, R2-5). 손 스크롤이 됐다 |
| 넓은 창 | 탭이 머리줄 "Convoy" 옆으로 들어가 한 줄이 됐고 2열은 그대로였다 |
| **창 크기 변화와 상태** | 처음에는 밀도를 바꾸자 액티비티가 다시 만들어져 그룹 목적지가 사라졌고, 그 뒤 SDK가 기어 값을 다시 보내지 않아 기어가 `UNKNOWN`으로 남았다(따라가기 요청이 막힘). 매니페스트에 `android:configChanges`(크기·밀도·주야간 등)를 넣자 액티비티가 유지되고 목적지와 기어도 그대로였다. 실제 차의 화면 나누기·주야간 전환에서도 같은지는 확인하지 못했다 |
| 밀도만 바뀔 때 | `configChanges`로 액티비티를 유지하면 `BoxWithConstraints`가 픽셀 제약이 그대로라 다시 계산하지 않아 2열로 남았다. 밀도를 `key`로 묶자 1열·2열이 양쪽으로 바뀌었다 |
| 시연 [거절] | 친구 화면에서 탭해 따라가기가 끝나고 버튼이 돌아오는 것을 확인했다(작업 22에서 못 한 것) |
| **주야간 신호** | `adb shell cmd uimode night yes`를 보내자 Pleos 시스템 화면(주행 정보, 상태 바 등)이 어두워졌다. 즉 에뮬레이터에서는 **Android `uiMode` 야간 모드**로 주야간이 전달된다(실차는 미확인). Convoy는 밝은 색만 있어 밝게 남았고, `configChanges`의 `uiMode` 덕분에 목적지 등 상태는 유지됐다. 다크 테마를 만들면 이 신호(`isSystemInDarkTheme`)를 쓰면 된다 |
| 비활성 버튼 | 투명도(0.45)로 흐리던 것을 회색 바탕과 보조 글자색으로 바꿨다. D에서 목적지·따라가기 버튼이 회색으로 바뀌고 글자는 읽힌다(Codex 리뷰, 대비 테스트로 고정) |

## 목적지 출처 시험 (2026-11-11, `spike/navi-destinations`, Pleos Emulator 2.1.0)
NaviHelper 3.0.5 AAR을 javap로 확인한 API(`getBookmarkInfo`, `getRecentDestinationInfo`, `getDestinationInfo`, `onDestinationChanged`)를 에뮬레이터에서 불러 봤다. 시험 코드는 머지하지 않는다.
| 확인 | 결과 |
|---|---|
| 최근 목적지 | `onRecentDestinationInfo`로 목록이 왔다. Convoy가 요청한 곳(강남역)과 **내비에서 직접 검색해 정한 곳(현대백화점 판교점)**이 최근 순으로 들어 있고, 이름·전체 주소·좌표·POI ID가 있다 |
| 현재 내비 목적지 | `getDestinationInfo()`를 부르면 `onDestinationInfo`로 이름, 좌표, 남은 거리(m)·시간(초)이 왔다. 주소는 "분당구 백현동"처럼 짧게 왔다. 안내 중이 아니면 이름이 빈 값이다 |
| 목적지 변경 알림 | `onDestinationChanged`는 Convoy가 `requestRoute`했을 때도, 내비 화면에서 검색해 목적지를 바꿨을 때도 오지 않았다. 내비에서 바꿨을 때는 `onRouteStarted`도 오지 않았다. 그래서 Convoy 화면으로 돌아올 때 물어보는 방식으로 한다(R11-21) |
| 즐겨찾기 | `onBookmarkInfo`로 집(`HOME`)·회사(`OFFICE`) 두 칸이 왔지만 이름이 비고 좌표가 0,0이었다(등록된 곳 없음) |
| 내비 검색 | 내비 화면의 검색으로 "판교"를 찾아 [목적지로]를 누르면 바로 안내가 바뀐다. 목적지/경유지를 묻는 창이 뜬다 |

## 작업 23 홈 확인 (2026-11-11, `feat/home`, Pleos Emulator 2.1.0)
| 확인 | 결과 |
|---|---|
| 홈 첫 화면 | 멤버 없이(나만) 시작하고, 연결된 사람 4명이 보였다(오프라인 서준은 안 보임). [목적지로 함께]는 멤버가 없어 잠겨 있었다 |
| 초대·응답 | [그룹에 초대]를 탭하자 "수락 대기"가 되고 시연 패널에 첫 수락 대기가 나왔다. [수락] → "그룹 멤버", [거절] → 다시 [그룹에 초대]. 멤버가 생기자 [목적지로 함께]가 켜졌다 |
| 주행과 그룹 | [목적지로 함께] → 그룹 주행 화면에 실제 멤버(나, 민수)가 나왔고 경로 요청·가상 멤버 경로 시작이 기존대로 됐다. [홈으로] → [진행 중인 주행으로]가 보이고 초대·[그룹 해산]은 잠겼다. [주행 끝내기] → 그룹(민수)은 남고 출발 버튼이 돌아왔다. [그룹 해산] → 처음 상태 |
| 기어 유지 | 주행마다 컨트롤러를 새로 만들어도 액티비티가 들고 있는 기어(PARK)로 시작해 버튼 판단이 맞았다(SDK는 기어를 다시 보내지 않는다) |
| 사고 확인 덮개 | 홈 위에도 "괜찮으세요?"가 덮였다(R2-5) |
| 끝낸 세션의 예약 | 목적지를 정하고 1초 뒤 [주행 끝내기] → 9초 동안 가상 멤버의 경로 시작 이벤트가 오지 않았다(예약 취소, Codex 리뷰) |
| 가족 없이 사고 의심 | R8-9대로 가족 없이 시작하자, 경고등 30초 무응답 뒤 "확인 끝: 보내지 않음"이었다(R8-5). 머리줄 가족 표시는 작업 24 전이라 "가족 공유 꺼짐"으로 나온다 |

## 작업 24 가족 지정 확인 (2026-11-11, `feat/family`, Pleos Emulator 2.1.0)
| 확인 | 결과 |
|---|---|
| 지정·수락 | 홈에서 민수 [가족으로 설정] → 가족 카드 "민수님 수락 대기"와 [지정 취소], 민수 줄 "가족 수락 대기", 다른 사람의 [가족으로 설정]은 잠김, 시연 패널에 가족 요청. [수락] → "민수 · 사고 알림 받는 중", 머리줄 "가족 민수" |
| 그룹 없이 사고 알림 | 그룹 주행 없이 홈에서 경고등 30초 무응답 → 가족에게 보냄. 가족 카드 아래 "민수님에게 보낸 알림" 카드(R8-10, R9-1) |
| 해제 | [가족 해제] → "가족 없음"으로 돌아가고 다른 사람을 다시 지정할 수 있다. 이미 보낸 알림 카드는 받은 사람 이름과 함께 남는다 |
| 확인 못 함 | 주행 중(D) [가족 해제]·[지정 취소](단위 테스트로만 확인) |

## 작업 26 내비 목적지 가져오기 관찰 (2026-11-11, `feat/navi-destinations`, Pleos Emulator 2.1.0)
| 확인 | 결과 |
|---|---|
| 목적지를 정한 직후 | 내비 검색 화면에서 "현대백화점 판교점"을 [목적지로] 정하고 바로 Convoy로 돌아오자 `getDestinationInfo()`가 빈 값(이름 없음, 0,0)을 줬다. 몇 초 뒤 다시 묻자 값이 왔다. 내비가 경로를 잡는 중이라 그런 것으로 보인다(추론). 그래서 돌아온 뒤 3초·6초에 더 묻는다(R11-21) |
| 좌표 차이 | 같은 장소인데 내비 목적지는 37.39192222, 127.11171666, 최근 목적지는 37.39283611, 127.11199166으로 약 100m 다르다. 두 값 모두 `displayLatitude`·`displayLongitude`는 null이었다. 내비 목적지는 경로 도착점, 최근 목적지는 장소 좌표인 것으로 보인다(추론). 그래서 주소 보완과 "지금 목적지" 표시는 이름이 같고 300m 미만이면 같은 곳으로 본다(R11-21) |
| 재조회 결과 | 내비에서 서울역을 정하고 바로 돌아오자 돌아온 직후·3초 뒤는 빈 값, 6초 뒤 조회에서 서울역이 왔다(1회). 여유가 거의 없으므로 실차에서 더 늦으면 간격을 늘린다 |
| 그룹과 함께 가기 | 카드의 [그룹과 함께 가기] → 그룹 목적지 서울역, 내 차 `requestRoute` → `onRouteStarted name=[서울역]` → 주행 중, 가상 멤버도 6초 뒤 주행 중. 카드가 사라지고 최근 목적지의 서울역에 "지금 목적지"가 붙었다 |
| 확인 못 함 | [무시]와 즐겨찾기(에뮬레이터에 등록된 곳이 없음)는 단위 테스트로만 확인 |
| **도착 판정 주소(실차 확인 필요)** | Convoy가 "서울 용산구 한강대로 405"로 경로를 요청해도 `getDestinationInfo()`는 "용산구 동자동"을 돌려줬다. 내비가 주소를 자기 형식으로 바꿔 들고 있다는 뜻이다. R11-4는 도착 이벤트의 이름·주소가 마지막 요청과 같을 때만 도착으로 보므로, `DestinationArrivedInfo.address`가 짧은 형식이면 리더 도착이 누락될 수 있다. 에뮬레이터는 위치가 고정이라 도착을 만들 수 없어 확인하지 못했다(Codex 리뷰). 실차에서 확인하고 다르면 R11-4 비교 기준을 다시 정한다 |

## 작업 25 앞차 따라가기 확인 (2026-11-11, `feat/follow`, Pleos Emulator 2.1.0)
| 확인 | 결과 |
|---|---|
| 선택·요청 | 홈의 "앞차 따라가기"에 수락한 멤버만 나왔고, 업무 중인 지영은 잠긴 채 "업무 중이라 따라갈 수 없어요". [민수 따라가기] → "민수님 수락 대기", 초대·해산 잠김, 시연 패널에 따라가기 요청 |
| 수락과 경로 | 시연 [수락] → 따라가기 화면, 내 내비 `requestRoute name=[민수 차]` → `onRouteStarted name=[민수 차] distance=2900`. 내 위치가 2초마다 들어와 "약 998m 앞"(가상 앞차 시작 위치가 내 차에서 북쪽 약 1km) |
| R12-3 예시 | 100m → 다시 요청 안 함, 100m(누적 200m) → 다시 요청(`onRouteStarted distance=2800`), 300m → 다시 요청(`distance=3100`). 화면 "다시 요청 2번", "약 1.5km 앞" |
| 끊기 | [끊기] → `cancelRoute` → `onRouteCancelled`, 내비가 경로 없는 화면으로 돌아갔다. 위치 요청이 멈추고 홈에서 그룹은 남았다 |
| 확인 못 함 | 경로 요청 실패와 [다시 요청], 좁은 창의 따라가기 화면, 주행 중(D) [끊기](단위 테스트·R10 규칙으로만) |
