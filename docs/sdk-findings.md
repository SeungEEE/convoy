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

| 확인 | 결과 |
|---|---|
| 안내 중 `cancelRoute` 없이 새 `requestRoute` | **된다.** 기존 경로가 자동 취소되고(`onRouteCancelled`, 내비 상태 `FREE_DRIVE`) 새 경로가 시작된다(`onRouteStarted`, `ROUTE_DRIVE`). `getDestinationInfo`도 새 목적지로 바뀐다. 요청부터 경로 확정까지 약 5~7초 걸린다 |
| `onDestinationArrived` | **에뮬레이터에서 유도하지 못했다.** 위치가 고정이라 차가 움직이지 않는다. 현재 위치와 같은 좌표(남은 거리 2m)와 40m 떨어진 좌표(10m)를 목적지로 줬지만 20초 넘게 도착 이벤트가 오지 않았다. 도착 시연은 가상 멤버(`FakeConvoyServer`)로 대신해야 한다 |
| 외부 통신 | **된다.** 앱에서 호스트 PC(`http://10.0.2.2:<포트>`)와 인터넷(`https://example.com`)에 HTTP 200. `INTERNET` 퍼미션과 평문 통신은 `usesCleartextTraffic`이 필요하다. 호스트에서 `python3 -m http.server`로 시험했다 |
| `requestRoute` 후 내비 앱이 전면을 가져가는가 | 가져가지 않았다. 내 앱이 전면인 상태에서 경로가 시작된 뒤 12초 동안 내 앱이 앞에 남았다 |
| `requestNavigationScreen(Foreground)` | `pleos.car.permission.NAVI_CUSTOM_MAP`이 없으면 `SecurityException`. 이 퍼미션을 매니페스트에 넣으면 에뮬레이터에서 `granted=true`이고, 호출하면 **Pleos 내비 앱(`ai.umos.maps.android.navigation.app`)이 전면으로 올라와 내 앱이 있던 오른쪽 앱 영역을 대체한다** |
| 에뮬레이터 2대 동시 실행 | 시험하지 않았다 |

### 화면 구성
- 에뮬레이터 화면은 **왼쪽 차량 패널(기어, 속도, 차량 그림)과 오른쪽 앱 영역**이다. 내 앱과 내비 앱은 같은 오른쪽 영역을 번갈아 쓴다. 나란히 놓이지 않았다
- 내비 앱이 전면일 때 오른쪽 영역에 목적지명, 남은 거리, 도착 시각, 내 위치 마커, 안내 종료 버튼이 표시된다. 지도 위에 다른 차를 그리는 SDK API는 찾지 못했다
- `NaviHelper.getInternalApi().updateOccupiedRegion(Rect)`는 존재하지만 문서가 없고 동작은 확인하지 못했다

### 기획에 미치는 영향
- 목적지 동기화는 멤버 차에서 `requestRoute`를 그대로 호출하면 된다(`cancelRoute` 불필요). 확정까지 수 초 걸리므로 UI에 "재탐색 중" 상태가 필요하다
- 도착 공유는 `onDestinationArrived`가 실제로 오는지 에뮬레이터에서 확인할 수 없다. 실차에서만 확인 가능하며, 데모는 가상 멤버로 한다
- 친구·일행 위치를 지도에 표시하는 기능은 SDK로는 불가능하다. Convoy 자체 화면은 목록과 상태 표시 중심이어야 한다
