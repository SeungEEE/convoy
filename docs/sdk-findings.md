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
- 경로를 요청하면 Pleos 내비 앱이 전면으로 올라와 안내를 시작한다. 내 앱은 백그라운드가 되지만 콜백은 계속 왔다
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
- 미확인: 에뮬레이터 2대 동시 실행, `onDestinationArrived` 실제 수신, `addWaypoint`, Compose 동작, 외부 네트워크 통신

## 앱 제약 (Pleos 가이드)
- 주행 중에는 CarUxRestrictions를 확인해 제한 기능을 줄이거나 끈다. 영상 재생 금지, 애니메이션 최소화, 큰 터치 영역, 텍스트 최소화
- 응답 0.25초 이내, 앱 실행 10초 이내, CPU는 주행 중 비내비 앱 5% 미만, RAM 400MB 이하
- 레터박스와 호환성 모드 금지. `screenOrientation`을 지정하지 않으면 세로 단일 window이고, `landscape`면 2개 window를 쓴다
