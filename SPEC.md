# SPEC: Convoy 명세

기준 문서: `REQUEST.md`. 이 문서의 예시표가 곧 테스트다. 코드가 이 문서와 다르면 코드가 틀린 것이다.
(제안) 표시는 요청서에 없어서 정한 값이다. 바꿀 때는 이 문서를 먼저 고친다.
대상은 1차 제출(P0)이다. P1은 4장에 요약만 둔다.

## 1장. 요구사항

규칙은 모두 SDK와 무관한 순수 함수로 만든다(`domain/`). 입력은 SDK가 아니라 우리 타입이다.

### R1. 친구 초대 코드
- R1-1. 초대 코드는 숫자 6자리다. 그 밖의 입력이면, 시스템은 `INVALID_FORMAT`을 돌려준다.
- R1-2. 코드는 만든 지 600초(10분)를 넘으면 만료된다. 600초까지는 유효하고 601초부터 `EXPIRED`다. (제안)

`validateInviteCode(code: String)`

| 입력 | 결과 |
|---|---|
| `"123456"` | `OK` |
| `"000000"` | `OK` |
| `"12345"` (5자) | `INVALID_FORMAT` |
| `"1234567"` (7자) | `INVALID_FORMAT` |
| `"12345a"` | `INVALID_FORMAT` |
| `""` | `INVALID_FORMAT` |
| `" 12345 "` (공백 포함) | `INVALID_FORMAT` |

`isInviteCodeExpired(ageSeconds: Int)`

| 경과(초) | 결과 |
|---|---|
| 0 | false |
| 599 | false |
| 600 | false |
| 601 | true |

### R2. 내 상태와 친구 지도
- R2-1. 상태는 `ACTIVE`(활동 중), `BUSY`(업무 중), `OFFLINE`이다.
- R2-2. 친구 지도에는 `ACTIVE`와 `BUSY` 친구만 표시한다. `OFFLINE`이면, 시스템은 지도에 표시하지 않고 앱을 쓰지 않는 상태와 구분되지 않게 한다.
- R2-3. 상대가 `BUSY`이면, 시스템은 따라가기 요청을 거절한다(`BUSY`). `OFFLINE`은 지도에 없으므로 요청할 수 없다.

`visibleOnMap(status)`, `canRequestFollow(targetStatus)`

| 상대 상태 | visibleOnMap | canRequestFollow |
|---|---|---|
| `ACTIVE` | true | `OK` |
| `BUSY` | true | `REJECT_BUSY` |
| `OFFLINE` | false | `REJECT_OFFLINE` |

### R3. 따라가기 요청과 수락
- R3-1. 요청은 `REQUESTED`로 시작하고, 상대가 차 화면에서 수락해야 `ACTIVE`가 된다. 수락 전에는 추적하지 않는다.
- R3-2. 상대가 거절하면 `ENDED`가 된다.
- R3-3. 따라가는 쪽이든 따라오는 쪽이든 끊으면, 시스템은 `ENDED`로 바꾸고 따라가던 경로를 취소한다.
- R3-4. 따라오는 동안 상대 화면에는 "따라오는 중" 표시와 [끊기] 버튼이 있다.

`nextFollowState(current, event)`

| 현재 | 이벤트 | 다음 |
|---|---|---|
| `REQUESTED` | `ACCEPT` | `ACTIVE` |
| `REQUESTED` | `DECLINE` | `ENDED` |
| `REQUESTED` | `CANCEL` | `ENDED` |
| `ACTIVE` | `CANCEL` (끊기) | `ENDED` |
| `ACTIVE` | `ACCEPT` | `ACTIVE` (무시) |
| `ENDED` | `ACCEPT` | `ENDED` (무시) |

### R4. 그룹 만들기와 초대
- R4-1. 그룹은 리더 1명과 멤버로 이루어지고 리더 포함 최대 5대다. 6번째 초대는 `GROUP_FULL`이다. (제안)
- R4-2. 리더는 친구로 등록된 차량만 초대할 수 있다. 친구가 아니면 `NOT_FRIEND`다.
- R4-3. 한 차는 동시에 하나의 그룹에만 속한다. 이미 속해 있으면, 시스템은 초대 수락을 `ALREADY_IN_GROUP`으로 거절한다.
- R4-4. 멤버는 차 화면에서 수락해야 그룹에 들어간다.

`canInvite(memberCount, isFriend)`

| 현재 인원(리더 포함) | 친구인가 | 결과 |
|---|---|---|
| 1 | true | `OK` |
| 4 | true | `OK` |
| 5 | true | `GROUP_FULL` |
| 1 | false | `NOT_FRIEND` |
| 5 | false | `NOT_FRIEND` (친구 여부를 먼저 본다) |

`canAcceptInvite(currentGroupId)`

| 현재 그룹 | 결과 |
|---|---|
| 없음 | `OK` |
| 있음 | `ALREADY_IN_GROUP` |

### R5. 목적지 동기화
- R5-1. 리더가 목적지를 바꾸면, 시스템은 그룹의 모든 멤버 차에서 그 목적지로 경로를 다시 요청한다(`requestRoute`). 멤버가 수락할 필요는 없다.
- R5-2. 목적지는 리더만 바꿀 수 있다. 멤버가 바꾸려 하면 `NOT_LEADER`다.
- R5-3. 새 목적지가 현재 목적지와 10m 미만 차이면 같은 목적지로 보고 재탐색하지 않는다. (제안)

`shouldSyncDestination(isLeader, distanceFromCurrentMeters)`

| 리더인가 | 현재 목적지와 거리(m) | 결과 |
|---|---|---|
| true | 500 | `SYNC` |
| true | 10 | `SYNC` |
| true | 9 | `IGNORE_SAME` |
| true | 0 | `IGNORE_SAME` |
| false | 500 | `NOT_LEADER` |

### R6. 추적 주행
- R6-1. 목적지 없이 따라가기가 `ACTIVE`이면, 시스템은 상대 차의 위치를 목적지로 경로를 요청한다.
- R6-2. 상대 위치가 마지막으로 요청한 목적지에서 200m 이상 벗어나면 다시 요청한다. 200m 미만이면 요청하지 않는다. 내비가 자주 바뀌지 않게 하기 위해서다.
- R6-3. 위치는 서버가 받은 상대 차의 값이고, 위치 조회는 2초 간격 폴링이다.

`shouldReroute(distanceFromLastTargetMeters)`

| 마지막 목적지와 거리(m) | 결과 |
|---|---|
| 0 | false |
| 199 | false |
| 200 | true |
| 201 | true |

### R7. 도착과 출발 공유
- R7-1. 기어가 P에서 D로 바뀌면, 시스템은 `DEPARTED`를 공유한다.
- R7-2. 목적지가 있으면 내비의 도착 이벤트(`onDestinationArrived`)가 `ARRIVED`다. 이때 기어 D→P는 도착으로 보지 않는다(주차나 정차일 수 있다).
- R7-3. 목적지가 없으면(추적 주행) 기어 D→P가 `ARRIVED`다.
- R7-4. 그룹의 모든 멤버가 `ARRIVED`이면, 시스템은 그룹을 종료한다.

`gearEvent(previous, current, hasDestination)`

| 이전 | 현재 | 목적지 | 결과 |
|---|---|---|---|
| `PARK` | `DRIVE` | true | `DEPARTED` |
| `PARK` | `DRIVE` | false | `DEPARTED` |
| `DRIVE` | `PARK` | false | `ARRIVED` |
| `DRIVE` | `PARK` | true | `NONE` |
| `DRIVE` | `NEUTRAL` | false | `NONE` |
| `REVERSE` | `DRIVE` | false | `NONE` |
| `UNKNOWN` | `DRIVE` | false | `NONE` |
| `PARK` | `PARK` | false | `NONE` |

`isGroupFinished(memberStates)`

| 멤버 상태 | 결과 |
|---|---|
| `[ARRIVED, ARRIVED, ARRIVED]` | true |
| `[ARRIVED, DEPARTED, ARRIVED]` | false |
| `[ARRIVED]` (혼자) | true |
| `[]` (멤버 없음) | false |

### R8. 사고 의심과 가족 연락
- R8-1. 경고등 `AIRBAG` 또는 `EMERGENCY_CALL`이 `ON` 또는 `BLINK`이면, 시스템은 사고 의심으로 보고 운전자에게 "괜찮으세요?"를 묻는다. (`ABS`, `TPMS` 같은 다른 경고등은 사고 의심이 아니다.)
- R8-2. 운전자가 [괜찮아요]를 누르면 아무것도 보내지 않는다.
- R8-3. 30초가 지나도 응답이 없으면, 시스템은 가족에게 위치와 경고등 정보를 보낸다. 응답이 30초 미만이면 유효하고, 정확히 30초가 되면 전송한다.
- R8-4. 이미 확인 중인데 같은 경고등이 다시 오면 중복해서 묻지 않는다.
- R8-5. 가족이 등록되어 있지 않으면 확인만 하고 전송하지 않는다.

`isAccidentSuspected(telltaleType, state)`

| 경고등 | 상태 | 결과 |
|---|---|---|
| `AIRBAG` | `ON` | true |
| `AIRBAG` | `BLINK` | true |
| `EMERGENCY_CALL` | `ON` | true |
| `AIRBAG` | `OFF` | false |
| `ABS` | `ON` | false |
| `TPMS` | `ON` | false |

`accidentAction(elapsedSeconds, responded, hasFamily)`

| 경과(초) | 응답함 | 가족 있음 | 결과 |
|---|---|---|---|
| 0 | false | true | `WAITING` |
| 29 | false | true | `WAITING` |
| 30 | false | true | `NOTIFY_FAMILY` |
| 31 | false | true | `NOTIFY_FAMILY` |
| 29 | true | true | `SAFE` |
| 30 | true | true | `SAFE` (이미 응답했으면 전송하지 않는다) |
| 30 | false | false | `NO_ACTION` |

### R9. 가족 등록
- R9-1. 가족 등록은 운전자 본인이 차 화면에서 수락해야 한다. 거절하거나 응답이 없으면 등록되지 않는다.
- R9-2. 운전자는 언제든 공유를 끌 수 있고, 끄면 이후 가족에게 아무것도 보내지 않는다.
- R9-3. 가족이 받는 알림은 1차에서 수신 측 앱 화면의 알림 카드다(폰 앱은 P2).

`nextFamilyState(current, event)`

| 현재 | 이벤트 | 다음 |
|---|---|---|
| `NONE` | `REQUEST` | `PENDING` |
| `PENDING` | `ACCEPT` | `ACTIVE` |
| `PENDING` | `DECLINE` | `NONE` |
| `ACTIVE` | `DISABLE` | `NONE` |
| `NONE` | `ACCEPT` | `NONE` (요청 없이 수락 불가) |

### R10. 주행 중 조작 제한
- R10-1. 설정은 정차(기어 P) 중에만 한다. 주행 중에는 상태 변경, 초대 수락, 따라가기 수락·끊기, 사고 확인 응답만 가능하다.

`canPerform(action, gear)`

| 동작 | `PARK` | `DRIVE` |
|---|---|---|
| 친구 등록 | true | false |
| 가족 등록 | true | false |
| 그룹 만들기·초대 | true | false |
| 내 상태 변경 | true | true |
| 초대 수락 | true | true |
| 따라가기 수락·끊기 | true | true |
| 사고 확인 응답 | true | true |

(`NEUTRAL`, `REVERSE`는 `DRIVE`와 같게 취급한다. `UNKNOWN`은 안전을 위해 `DRIVE`와 같게 취급한다.) (제안)

### R11. 그룹 주행 흐름
R5~R7의 규칙을 이어 붙인 흐름이다. 새 규칙은 최소로 하고, 새로 정한 값은 (제안)으로 표시한다.

- R11-1. 리더가 목적지를 바꾸면, `shouldSyncDestination`이 `SYNC`일 때만 **모든 멤버에게** `REQUEST_ROUTE`(목적지) 효과를 낸다. `NOT_LEADER`나 `IGNORE_SAME`이면 효과가 없다.
- R11-2. 멤버에게 `REQUEST_ROUTE`가 나가면 그 멤버는 `REROUTING`(재탐색 중)이 된다. 내비의 경로 시작 이벤트(`onRouteStarted`)가 오면 `DRIVING`이 된다. 확정까지 수 초 걸린다(실측 5.3~6.7초, `docs/sdk-findings.md`).
- R11-3. 멤버의 `gearEvent`가 `DEPARTED`면 그 멤버는 `DRIVING`이 되고 출발을 공유한다. 단 `REROUTING` 중이면 출발은 공유하되 상태는 `REROUTING`으로 둔다(새 경로가 시작되기 전이므로). `ROUTE_STARTED`가 오면 그때 `DRIVING`이 된다. (제안) 목적지가 있는 멤버는 `gearEvent`의 `ARRIVED`를 쓰지 않는다(R7-2).
- R11-4. 목적지가 있는 멤버는 내비 도착 이벤트(`onDestinationArrived`)가 오면 `ARRIVED`가 되고 도착을 공유한다. 도착 이벤트(`DestinationArrivedInfo`)에는 경로 식별자가 없고 `name`, `address`만 있으므로, 어댑터는 이 값이 마지막으로 요청한 목적지와 같을 때만 `NAVI_ARRIVED`를 만든다. 같은 이름의 다른 장소처럼 이 방법으로 구분되지 않는 경우는 실차에서 추가로 확인한다. (제안) 목적지가 없는 멤버는 `gearEvent`가 `ARRIVED`일 때 `ARRIVED`가 된다.
- R11-5. 멤버가 `ARRIVED`가 되어 `isGroupFinished`가 true가 되면 `FINISH_GROUP` 효과를 낸다.
- R11-6. 이미 `ARRIVED`인 멤버에게 새 목적지가 오면 다시 `REROUTING`이 된다. (제안)
- R11-7. 이 장의 멤버 상태는 R7의 `MemberState`(`DEPARTED`, `ARRIVED`)와 별도 타입이다. `isGroupFinished`에 넘길 때는 `ARRIVED`는 `ARRIVED`로, 나머지(`IDLE`, `REROUTING`, `DRIVING`)는 모두 `DEPARTED`로 바꾼다. 이미 머지된 R7의 함수와 테스트는 바꾸지 않는다. (제안)
- R11-8. 이번 에뮬레이터 시험에서는 위치가 고정되어 `onDestinationArrived`를 유도하지 못했다(`docs/sdk-findings.md`). 이것이 SDK에서 도착이 안 된다는 뜻은 아니다. 데모에서는 `FakeConvoyServer`의 가상 멤버가 `NAVI_ARRIVED` 이벤트를 만들며, 이는 SDK 도착 이벤트의 검증이 아니라 데모용 대체 이벤트다. 실제 이벤트 수신은 실차 또는 다른 위치 시뮬레이션으로 추가 확인한다.
- R11-9. 그룹이 종료(`FINISH_GROUP`)된 뒤 리더가 목적지를 바꿔도 무시한다. 새 목적지는 새 그룹을 만들어서 정한다. (제안)
- R11-10. 목적지 없이 따라가는 추적 주행(R6)의 흐름은 이 장에 포함하지 않고 R12에서 다룬다.

아래 상태, 이벤트, 효과의 이름은 R5~R7에서 직접 정해지지 않은 모델링 선택이므로 모두 (제안)이다.

멤버 상태: `IDLE`(출발 전), `REROUTING`, `DRIVING`, `ARRIVED`
이벤트: `LEADER_SET_DESTINATION`, `ROUTE_STARTED`, `GEAR_DEPARTED`, `NAVI_ARRIVED`, `GEAR_ARRIVED`
효과: `REQUEST_ROUTE`, `SHARE_DEPARTED`, `SHARE_ARRIVED`

`nextMemberState(state, event)` → (다음 상태, 효과). 모두 10행이다.

| 현재 | 이벤트 | 다음 | 효과 |
|---|---|---|---|
| `IDLE` | `LEADER_SET_DESTINATION` | `REROUTING` | `REQUEST_ROUTE` |
| `REROUTING` | `ROUTE_STARTED` | `DRIVING` | 없음 |
| `IDLE` | `GEAR_DEPARTED` | `DRIVING` | `SHARE_DEPARTED` |
| `DRIVING` | `NAVI_ARRIVED` | `ARRIVED` | `SHARE_ARRIVED` |
| `DRIVING` | `GEAR_ARRIVED` (목적지 없음) | `ARRIVED` | `SHARE_ARRIVED` |
| `ARRIVED` | `LEADER_SET_DESTINATION` | `REROUTING` | `REQUEST_ROUTE` |
| `DRIVING` | `LEADER_SET_DESTINATION` | `REROUTING` | `REQUEST_ROUTE` |
| `IDLE` | `NAVI_ARRIVED` | `IDLE` | 없음 (출발 없이 도착할 수 없다) |
| `REROUTING` | `GEAR_DEPARTED` | `REROUTING` | `SHARE_DEPARTED` (출발은 공유하되, 경로 시작 전이라 상태는 그대로) |
| `REROUTING` | `NAVI_ARRIVED` | `REROUTING` | 없음 (새 경로 시작 전의 도착은 이전 경로의 늦은 이벤트로 본다) |

표에 없는 조합은 상태를 바꾸지 않고 효과도 없다. (제안)

`FINISH_GROUP`은 이 함수가 아니라 그룹 단위 흐름(작업 19)이 `isGroupFinished`로 판단한다.

## 2장. 구조

```
com.car.convoy
├── domain/   SDK와 무관한 규칙. 위 함수와 enum, 상태 전이. 단위 테스트의 대상
├── data/     NaviPort, VehiclePort, ConvoyServer 인터페이스와 구현
│   ├── navi/     NaviHelper 어댑터 (requestRoute, cancelRoute, 위치 2초 폴링, 도착 이벤트)
│   ├── vehicle/  Vehicle 어댑터 (기어, 방향지시등, 경고등 구독)
│   └── server/   FakeConvoyServer (인메모리 목업. 가상 멤버를 시뮬레이션)
└── ui/       Jetpack Compose 화면
```

- SDK 어댑터는 인터페이스 뒤에 두고, 테스트에서는 가짜 구현을 쓴다.
- 목업 서버는 에뮬레이터 1대에서 가상 멤버(위치, 상태, 응답)를 만들어 시나리오를 시연한다. 실제 서버는 확장 계획이다.
- 시간은 `Clock` 인터페이스로 주입해, 30초와 600초 규칙을 시계 없이 테스트한다.

## 3장. 작업 목록
작업 하나는 함수 하나다. 작업마다 테스트 먼저(사람), Red 확인, 구현(AI), Green 확인 순서다.

- [x] 작업 0. Compose가 Pleos 에뮬레이터에서 뜨는지 확인한다 (안 되면 XML로 전환하고 알린다)
- [ ] 작업 1. `validateInviteCode` (R1, 7행)
- [ ] 작업 2. `isInviteCodeExpired` (R1, 4행)
- [ ] 작업 3. `visibleOnMap`, `canRequestFollow` (R2, 3행)
- [ ] 작업 4. `nextFollowState` (R3, 6행)
- [ ] 작업 5. `canInvite`, `canAcceptInvite` (R4, 7행)
- [x] 작업 6. `shouldSyncDestination` (R5, 5행)
- [x] 작업 7. `shouldReroute` (R6, 4행)
- [x] 작업 8. `gearEvent` (R7, 8행)
- [x] 작업 9. `isGroupFinished` (R7, 4행)
- [x] 작업 10. `isAccidentSuspected` (R8, 6행)
- [x] 작업 11. `accidentAction` (R8, 7행)
- [x] 작업 12. `nextFamilyState` (R9, 5행)
- [ ] 작업 13. `canPerform` (R10, 14행)
- [ ] 작업 14. `data/` 어댑터와 `FakeConvoyServer` (에뮬레이터에서 수동 확인)
- [ ] 작업 15. `ui/` 화면 (친구 지도, 그룹, 사고 확인, 가족 알림)
- [ ] 작업 16. 시나리오 시연: 목적지 변경 → 일행 재탐색 → 도착 공유, 사고 의심 → 확인 → 가족 알림
- [ ] 작업 17. `nextMemberState` (R11, 10행). (선행: 작업 14 전에 한다)
- [ ] 작업 18. 그룹 목적지 전파: 포트(`NaviPort`, `ConvoyServer`)의 가짜 구현으로, 리더가 목적지를 바꾸면 멤버 N명 모두에게 `requestRoute`가 호출되고 `NOT_LEADER`·`IGNORE_SAME`이면 호출되지 않음을 테스트한다 (R5, R11-1). (선행: 작업 14 전에 한다)
- [ ] 작업 19. 그룹 종료: 전원 `ARRIVED`면 `FINISH_GROUP`이 나오고, 종료 뒤 목적지 변경은 무시됨을 테스트한다 (R7-4, R11-5, R11-9). (선행: 작업 14 전에 한다)

## 4장. P1 (2차 PT 전, 이번 명세에 포함하지 않음)
방향지시등 공유, 근접·이탈 알림, 휴게·충전 조율(배터리 `getEvBatteryStateOfCharge`), 주행 상태 공유, 친구별 숨기기. P0 테스트가 모두 초록이 된 뒤 같은 방식으로 명세를 추가한다.
