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

### R2. 내 상태와 친구 목록
- R2-1. 상태는 `ACTIVE`(활동 중), `BUSY`(업무 중), `OFFLINE`이다.
- R2-2. 친구 목록에는 `ACTIVE`와 `BUSY` 친구만 표시한다. `OFFLINE`이면, 시스템은 목록에 표시하지 않고 앱을 쓰지 않는 상태와 구분되지 않게 한다.
- R2-3. 상대가 `BUSY`이면, 시스템은 따라가기 요청을 거절한다(`BUSY`). `OFFLINE`은 목록에 없으므로 요청할 수 없다.
- R2-4. 지도에 다른 차를 그리는 SDK API를 찾지 못해 지도 대신 목록으로 보여 준다(`docs/sdk-findings.md`). 지도 표시 수단이 확인되면 다시 정한다. (제안)
- R2-5. 앱은 탭 없이 [홈]에서 시작한다. 홈에는 가족(항상 연결, R9-5), 함께 출발(이번 주행 그룹, R4-5), 연결된 사람이 있다. 그룹 주행을 시작하면 [그룹 주행] 화면으로 넘어간다(R11-20, R12). 화면 문구는 "연결된 사람", "가족", "그룹", "멤버"를 쓰고 "친구", "일행"은 쓰지 않는다. R1~R3의 친구 규칙은 그대로이며 "연결된 사람"에 적용된다. 사고 확인 화면(R8)은 어느 화면 위에도 덮는다. 이전의 친구 목록 화면·탭 규칙(작업 22)은 이것으로 바뀐다. (제안)

`visibleInFriendList(status)`, `canRequestFollow(targetStatus)`

| 상대 상태 | visibleInFriendList | canRequestFollow |
|---|---|---|
| `ACTIVE` | true | `OK` |
| `BUSY` | true | `REJECT_BUSY` |
| `OFFLINE` | false | `REJECT_OFFLINE` |

### R3. 따라가기 요청과 수락
- R3-1. 요청은 `REQUESTED`로 시작하고, 상대가 차 화면에서 수락해야 `ACTIVE`가 된다. 수락 전에는 추적하지 않는다.
- R3-2. 상대가 거절하면 `ENDED`가 된다.
- R3-3. 따라가는 쪽이든 따라오는 쪽이든 끊으면, 시스템은 `ENDED`로 바꾸고 따라가던 경로를 취소한다.
- R3-4. 따라오는 동안 상대 화면에는 "따라오는 중" 표시와 [끊기] 버튼이 있다.
- R3-5. 표에 없는 상태·이벤트 조합(예: `ENDED`에서 `CANCEL`, `ACTIVE`에서 `DECLINE`)이면, 시스템은 상태를 바꾸지 않는다.

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
- R4-5. 홈(작업 23)은 다음을 보여 준다. (제안)
  - 머리줄: 내 상태 버튼 [활동 중] [업무 중] [오프라인](R2-1, 주행 중에도 된다).
  - 연결된 사람: `visibleInFriendList`가 true인 사람만 보인다. 사람마다 이름, 상태, 가족 표시(R9-5), 그룹 표시를 보여 준다. 그룹 표시는 그룹 밖이면 [그룹에 초대], 보낸 뒤에는 "수락 대기", 수락하면 "그룹 멤버"다. [그룹에 초대]는 `canInvite`가 `OK`이고 `canPerform(CREATE_OR_INVITE_GROUP, 기어)`일 때만 누를 수 있다. 거절하면 다시 [그룹에 초대]가 된다(R4-4).
  - 함께 출발: 이번 주행 그룹의 멤버(나는 리더, 수락한 멤버, 수락 대기)와 [목적지로 함께](R11-20), [앞차 따라가기](R12). 수락한 멤버가 1명 이상이고 정차 중일 때만 시작할 수 있다. 목적지로 함께는 `canPerform(SET_GROUP_DESTINATION, 기어)`, 앞차 따라가기는 `canPerform(REQUEST_FOLLOW, 기어)`를 쓴다.
  - 주행은 한 번에 하나다. 진행 중이면 출발 버튼 대신 [진행 중인 주행으로]를 보여 준다. 그룹은 [그룹 해산](R10 "그룹 해산", 정차 중)으로만 비운다. 가족은 해산과 무관하게 남는다.
  - 그룹과 주행은 다르다. 그룹은 초대·수락으로 꾸린 멤버이고, 주행은 "없음", "목적지로 함께"(R11), "앞차 따라가기"(R12) 중 하나다. 주행은 "없음"에서만 시작하고, 끝나면 "없음"으로 돌아가며 그룹은 남는다. 따라가기 요청 대기(`REQUESTED`)도 진행 중인 주행으로 본다.
  - 초대와 해산은 주행이 "없음"일 때만 된다. 1차에는 멤버가 스스로 나가는 기능이 없으므로, 주행 중에 멤버가 0명이 되는 일은 없다.
  - `canInvite`의 인원(`memberCount`)은 나, 수락한 멤버, 수락 대기를 모두 센다.
  - 시연 중 가상 인물의 상태는 바뀌지 않는다. 진행 중에 상대 상태가 바뀔 때의 처리는 서버 연동 때 정한다.
  - 외부 서버가 없어 연결된 사람은 앱에 미리 정해 둔 가상 인물이다(활동 중, 업무 중, 오프라인 각 1명 이상). 그룹 초대와 가족 지정(R9-5)에 대한 응답은 시연용 패널의 [수락] [거절]로 만든다. 시연은 그룹 멤버도 가족도 없이 시작한다.
  - 창이 좁을 때 1열로 쌓는 기준은 R11-20과 같다. 1열 순서는 가족, 함께 출발, 시연용 패널, 연결된 사람 순이다.

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
- R6-3. 위치는 서버가 받은 상대 차의 값이고, 위치 조회는 2초 간격 폴링이다. 1차 시연에는 서버가 없으므로 앞차 위치는 R12-3의 시연용 이동으로 만든다.

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
- R8-6. 경고등 목록이 올 때마다 `isAccidentSuspected`인 항목이 하나라도 있는지 본다. 확인은 "사고 의심 아님 → 사고 의심"으로 바뀔 때만 새로 시작한다. 경고등이 켜진 채로 목록이 다시 오면 묻지 않고(R8-4), 꺼졌다가 다시 켜지면 새로 묻는다. (제안)
- R8-7. 어댑터는 SDK 경고등 종류 중 `AIRBAG`, `EMERGENCY_CALL`, `ABS`, `TPMS`만 domain으로 바꾸고 나머지는 무시한다. REQUEST.md의 "급정지 조건"은 차속을 읽을 수 없어(`docs/sdk-findings.md`) 이번 판단에 넣지 않는다. (제안)
- R8-8. 확인을 시작할 때 내비에 위치 요청을 시작하고, 30초 안에 받은 위치 중 전송 직전의 가장 최근 값을 보낸다. 30초까지 위치를 받지 못하면 "위치 확인 불가"를 보낸다. R8-3의 위치 정보는 이 규칙을 따른다. (제안)
- R8-9. 시연은 가족 없이 시작한다. 사고 의심 시연 전에 R9-5로 가족을 지정하고 수락을 받는다. 가족이 없으면 R8-5대로 보내지 않는다. (제안)
- R8-10. 가족이 받는 알림 카드(R9-3)는 받는 차가 없으므로, 같은 화면에 "가족에게 보낸 알림"으로 보여 준다(시연용 표시). 내용은 시각, 위치, 경고등 종류다. 실제로 가족에게 보낸 경우에만 카드를 보여 주고, R8-5(가족 없음)나 R9-2(공유 끔)로 보내지 않은 경우에는 보여 주지 않는다. 카드에 받는 가족의 이름을 보여 준다("민수님에게 보낸 알림"). (제안)
- R8-11. 사고가 의심되면 Convoy 화면을 앞으로 가져와 "괜찮으세요?"를 보여 준다. 차량에서 앱이 스스로 앞으로 나올 수 있는지는 작업 20에서 에뮬레이터로 확인하고, 안 되면 다시 정한다. (제안)

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
- R9-1. 가족 등록은 운전자가 연결된 사람 한 명을 가족으로 지정하고(운전자 본인의 동의), 그 사람이 자기 차 화면에서 수락해야 된다. 거절하거나 응답이 없으면 등록되지 않는다. 가족은 그룹 주행과 무관하게 항상 연결되어 있고, 그룹 주행 중이 아니어도 사고 의심 알림(R8)을 받는다. 지정은 R10 "가족 등록"이라 정차 중에만 한다.
- R9-2. 운전자는 언제든 공유를 끌 수 있고, 끄면 이후 가족에게 아무것도 보내지 않는다. 끄기([가족 해제])는 주행 중에도 된다. (제안)
- R9-3. 가족이 받는 알림은 1차에서 수신 측 앱 화면의 알림 카드다(폰 앱은 P2).
- R9-4. 표에 없는 상태·이벤트 조합(예: `NONE`에서 `ACCEPT`, `ACTIVE`에서 `REQUEST`)이면, 시스템은 상태를 바꾸지 않는다.
- R9-5. 가족 표시(작업 24). (제안)
  - 가족은 한 명이다. 홈의 가족 카드는 `NONE`이면 "가족을 정하면 사고가 의심될 때 알려요", `PENDING`이면 "민수님 수락 대기"와 [지정 취소], `ACTIVE`면 "민수 · 사고 알림 받는 중"과 [가족 해제]를 보여 준다.
  - 연결된 사람 줄마다 `NONE`이면 [가족으로 설정]을 보여 준다. 누군가 `PENDING`이나 `ACTIVE`면 다른 사람의 [가족으로 설정]은 보이되 누를 수 없다.
  - 그룹 주행 화면 머리줄에도 "가족 민수"(없으면 "가족 없음")를 보여 준다.

`nextFamilyState(current, event)`

| 현재 | 이벤트 | 다음 |
|---|---|---|
| `NONE` | `REQUEST` | `PENDING` |
| `PENDING` | `ACCEPT` | `ACTIVE` |
| `PENDING` | `DECLINE` | `NONE` |
| `ACTIVE` | `DISABLE` | `NONE` |
| `NONE` | `ACCEPT` | `NONE` (요청 없이 수락 불가) |
| `PENDING` | `DISABLE` | `NONE` (지정 취소) |

### R10. 주행 중 조작 제한
- R10-1. 설정은 정차(기어 P) 중에만 한다. 주행 중에는 상태 변경, 초대 수락, 따라가기 수락·끊기, 사고 확인 응답, 가족 해제·지정 취소, 주행 끝내기만 가능하다. 화면 이동([홈으로], [진행 중인 주행으로])은 조작이 아니므로 제한하지 않는다.

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
| 그룹 목적지 변경 | true | false |
| 따라가기 요청 | true | false |
| 가족 해제·지정 취소 | true | true |
| 주행 끝내기 | true | true |
| 그룹 해산 | true | false |

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

`nextMemberState(state, event)` → (다음 상태, 효과). 모두 11행이다.

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
| `REROUTING` | `LEADER_SET_DESTINATION` | `REROUTING` | `REQUEST_ROUTE` (재탐색 중에 목적지가 다시 바뀌면 새 목적지로 다시 요청한다) |

표에 없는 조합은 상태를 바꾸지 않고 효과도 없다. (제안)

`GEAR_ARRIVED`는 목적지가 없는 멤버에게만 보낸다. 호출하는 쪽이 `gearEvent`의 결과가 `ARRIVED`이고 그 멤버에게 목적지가 없을 때만 이 이벤트를 만든다. (제안)

`FINISH_GROUP`은 이 함수가 아니라 그룹 단위 흐름(작업 19)이 `isGroupFinished`로 판단한다.

#### 그룹 단위 흐름 (작업 18, 19)

- R11-11. R5-1의 "모든 멤버"에는 리더도 들어간다. 리더 차도 같은 목적지로 `requestRoute`를 호출하고, 리더도 위 표의 멤버 상태를 가진다. `isGroupFinished`에도 리더 상태가 들어간다. (제안)
- R11-12. 그룹에 아직 목적지가 없을 때 리더가 처음 정하는 목적지는 거리 비교 없이 `SYNC`로 본다. (제안)
- R11-13. 목적지는 SDK와 무관한 `Destination(name, address, latitude, longitude)`로 나타낸다. R5-3의 "현재 목적지와 거리"는 두 좌표 사이의 직선 거리(하버사인, 지구 반지름 6,371,000m)다. SDK의 `RouteInfo`로 바꾸는 일은 어댑터(작업 14)가 한다. (제안)
- R11-14. 목적지 전파: 리더가 목적지를 정하면 `ConvoyServer`가 리더를 포함한 그룹의 각 멤버에게 목적지를 전달하고, 받은 차는 자기 `NaviPort.requestRoute(destination)`를 호출한다. 리더 앱이 다른 차의 내비를 직접 호출하지 않는다. 데모에서는 `FakeConvoyServer`가 가상 멤버의 수신을 흉내 낸다. (제안)
- R11-15. 그룹 상태는 `ACTIVE`, `FINISHED` 두 가지다. `ACTIVE`에서 멤버 상태가 바뀐 뒤 R11-7 변환으로 `isGroupFinished`가 true가 되면, `FINISH_GROUP`을 한 번 내고 `FINISHED`가 된다. `FINISHED`에서는 목적지 변경(R11-9)을 포함한 모든 이벤트를 무시하고, `FINISH_GROUP`도 다시 내지 않는다. (제안)
- R11-16. 어댑터의 기어 변환: `GEAR_PARK`→`PARK`, `GEAR_DRIVE`와 `GEAR_1`~`GEAR_9`→`DRIVE`, `GEAR_NEUTRAL`→`NEUTRAL`, `GEAR_REVERSE`→`REVERSE`, `GEAR_UNKNOWN`과 null→`UNKNOWN`. `GEAR_1`~`GEAR_9`는 R7의 `DRIVE`와 같게 취급한다. 따라서 `GEAR_PARK`에서 `GEAR_1`로 바뀌어도 R7-1의 출발(`DEPARTED`)이다. (제안)
- R11-17. 어댑터는 `onRouteStarted`의 `RouteStartInfo.name`이 마지막으로 요청한 목적지의 `name`과 같을 때만 `ROUTE_STARTED`를 만든다. `name`에 무엇이 오는지는 작업 14에서 에뮬레이터로 확인하고, 목적지 이름이 아니면 이 규칙을 지우고 R11-3 범위로 돌린다. 이름 비교는 공백과 대소문자를 바꾸지 않는 정확한 문자열 일치다. R11-4의 도착 이벤트 비교도 같다. R11-4와 마찬가지로 같은 이름의 다른 목적지로 다시 요청한 경우는 구분하지 못한다. (제안)
- R11-18. `FakeConvoyServer`의 가상 멤버는 목적지를 받으면 6초 뒤 `ROUTE_STARTED`를 만든다(실측 5.3~6.7초). 가상 멤버의 출발(`GEAR_DEPARTED`)과 도착(`NAVI_ARRIVED`)은 시연자가 정한 때에 만든다(작업 14에서는 adb 명령, 작업 15 뒤에는 시연용 버튼). 이것은 R11-8의 데모용 대체 이벤트다. 가상 멤버가 `REROUTING` 중에 새 목적지를 받으면, 이전에 예약한 `ROUTE_STARTED`를 취소하고 새 요청 기준으로 다시 6초를 센다. (제안)
- R11-19. 내 차의 `requestRoute`가 실패하면(SDK 예외) 실패를 화면에 알린다. 이미 진행된 그룹 목적지 변경과 다른 멤버로의 전파는 되돌리지 않는다(다른 멤버의 경로는 각 차가 따로 요청하므로 내 차의 실패와 무관하다). 리더는 실패 표시가 있고 그룹이 `ACTIVE`일 때만 [다시 요청]으로 현재 그룹 목적지를 내 내비에만 다시 요청할 수 있다. 이 재요청은 `shouldSyncDestination`을 거치지 않는다(같은 목적지라 `IGNORE_SAME`이 되므로). 재요청도 실패하면 다시 실패를 알린다. (제안)
- R11-20. 그룹 주행 화면(작업 15)은 다음을 보여 준다. (제안)
  - 그룹 상태와 목적지. 그룹이 `FINISHED`면 "모두 도착했어요"를 크게 보여 준다.
  - 멤버 목록. 상태는 `IDLE` "출발 전", `REROUTING` "경로 찾는 중", `DRIVING` "주행 중", `ARRIVED` "도착"으로 표시한다.
  - 목적지 고르기: R11-21의 출처(내비에서 정한 목적지, 최근 목적지, 즐겨찾기)에서 고른다. 리더이고 `canPerform(SET_GROUP_DESTINATION, 기어)`가 true일 때만 누를 수 있다. `SET_GROUP_DESTINATION`은 R10 표의 "그룹 목적지 변경" 동작을 가리킨다.
  - [다시 요청] 버튼: `canRetryMyRoute`일 때만 보인다(R11-19).
  - 시연용 패널: 가상 멤버마다 [출발], [도착] 버튼(R11-18). "시연용"이라고 표시하고 다른 영역과 구분한다.
  - 글자와 버튼은 차량 화면 기준으로 크게 한다(버튼 높이 80dp 이상, 본문 28sp 이상, `docs/sdk-findings.md`).
  - 화면 루트에 주어진 Compose 최대 너비(`maxWidth`, dp)가 1,200dp 미만이면(세로 창, 화면 나누기) 2열을 1열로 쌓는다. 정확히 1,200dp면 2열이다. 그룹 주행 화면의 1열 순서는 왼쪽 열 다음 오른쪽 열이다(그룹 상태·멤버가 먼저 나오고, 주행 중 막히는 목적지 고르기는 아래로 간다). 넘치면 사용자가 손으로 세로 스크롤한다. 손 스크롤은 R10의 제한 대상이 아니고, 자동 스크롤은 하지 않는다. Pleos UI 가이드는 가로·세로 레이아웃을 모두 지원하라고 한다(`docs/sdk-findings.md`). (제안)
  - 목적지를 바꾸면 내비 앱이 앞으로 오는 것은 막지 않는다(`docs/sdk-findings.md`). Convoy 화면은 정차 중 설정과 멤버 상태 확인용이다.
  - 위쪽에 [홈으로]와 [주행 끝내기]가 있다. [홈으로]는 주행을 이어 둔 채 홈으로 간다. [주행 끝내기]는 그룹 주행을 끝내고(주행 "없음", R11-22) 홈으로 간다. 그룹은 남는다. 두 버튼은 주행 중에도 된다(R10-1, R10 "주행 끝내기").
- R11-21. 목적지 출처(작업 26). 시그니처는 NaviHelper 3.0.5 AAR로, 값은 에뮬레이터로 확인했다(`docs/sdk-findings.md`). (제안)
  - 내비에서 정한 목적지: Convoy 화면이 앞으로 올 때와 그룹 주행 화면에 들어올 때 `getDestinationInfo()`로 내비의 현재 목적지를 묻고, 그 뒤 3초와 6초에 한 번씩 더 묻는다. 내비에서 목적지를 정한 직후에는 내비가 경로를 잡는 중이라 빈 값이 온다(`docs/sdk-findings.md`). 내비 목적지는 이름이 비어 있지 않고 좌표가 (0, 0)이 아닐 때만 쓴다. 이름이나 좌표 중 하나라도 비면 목적지가 없는 것으로 본다. 그룹 목적지가 없거나 내비 목적지와 10m 이상 다르고, 리더가 [무시]한 곳이 아니면 "내비에서 정한 목적지" 카드에 [그룹과 함께 가기] [무시]를 보여 준다. [그룹과 함께 가기]는 그 목적지로 그룹 목적지를 바꾸는 것(R5, R11-1)과 같다. [그룹과 함께 가기]와 [무시]는 둘 다 R10 "그룹 목적지 변경"의 제한을 받는다(정차 중 리더만). 주행 중에는 카드만 보이고 두 버튼은 잠긴다. `onDestinationChanged`는 에뮬레이터에서 오지 않아 쓰지 않는다.
  - "같은 목적지"는 이름이 아니라 좌표로 판단한다. 두 좌표의 `distanceMeters`가 10m 미만이면 같다(R5-3과 같은 기준). [무시]는 그 좌표를 기억하고, 내비 목적지가 그 좌표에서 10m 이상 바뀔 때까지 다시 묻지 않는다. 내비 목적지의 주소는 짧게 오므로, 최근 목적지 중 이름이 같고 300m 미만인 것이 있으면 그 주소를 쓰고 없으면 받은 주소를 쓴다. 내비 목적지의 좌표는 경로 도착점이라 같은 장소의 최근 목적지 좌표와 약 100m 다를 수 있다(에뮬레이터 실측). "지금 목적지" 표시는 10m 미만이거나, 이름이 같고 300m 미만이면 붙인다.
  - 최근 목적지: `getRecentDestinationInfo()`에서 이름이 있고 좌표가 (0, 0)이 아닌 것을 최대 5개 보여 준다. 지금 그룹 목적지와 같은 곳(10m 미만이거나, 이름이 같고 300m 미만)에는 "지금 목적지" 표시를 붙인다.
  - 즐겨찾기: `getBookmarkInfo()`에서 이름이 있고 좌표가 (0, 0)이 아닌 것만 보여 준다. 집·회사 칸이 비어 있으면 보여 주지 않는다. "지금 목적지" 표시는 최근 목적지와 같은 기준이다.
  - 최근 목적지와 즐겨찾기가 모두 비어 있으면 미리 정한 목록(서울역, 강남역, 판교역)을 예비로 보여 준다.
  - 검색 화면은 만들지 않는다. "현대차 내비에서 검색해 목적지를 정하고 돌아오면 여기에 떠요"라는 안내만 둔다. 내비 검색 화면을 직접 여는 `requestNavigationScreen`은 `NAVI_CUSTOM_MAP` 퍼미션이 필요하고, 호출하면 내비가 Convoy 자리를 차지한다(`docs/sdk-findings.md`).
- R11-22. [주행 끝내기]는 진행 중인 그룹 세션을 버리고, 다음 [목적지로 함께] 때 새 세션(모든 멤버 `IDLE`, 목적지 없음)으로 시작한다. `FINISH_GROUP`은 내지 않는다(전원 도착일 때만 낸다, R11-15). `FINISHED` 뒤에도 [주행 끝내기]로 같은 방식으로 끝낸다. (제안)

`distanceMeters(a, b)`: 두 `Destination` 좌표 사이의 하버사인 거리(m). 허용 오차는 ±0.01m다. (제안)

| a (위도, 경도) | b (위도, 경도) | 거리(m) |
|---|---|---|
| (37.5, 127.0) | (37.5, 127.0) | 0 |
| (37.5, 127.0) | (37.50008, 127.0) | 8.8956 |
| (37.5, 127.0) | (37.50009, 127.0) | 10.0075 |
| (37.5, 127.0) | (37.5045, 127.0) | 500.3772 |

작업 18 테스트 예시 (멤버마다 가짜 `NaviPort`, 현재 목적지는 (37.5, 127.0)):

| 요청한 사람 | 그룹 인원(리더 포함) | 현재 목적지 | 새 목적지 위도 (거리) | `requestRoute` 호출 | 멤버 상태 |
|---|---|---|---|---|---|
| 리더 | 3 | 없음 | 37.5045 | 3명 각 1번 | 모두 `REROUTING` |
| 리더 | 3 | 있음 | 37.5045 (500.4m) | 3명 각 1번 | 모두 `REROUTING` |
| 리더 | 3 | 있음 | 37.50009 (10.0m) | 3명 각 1번 | 모두 `REROUTING` |
| 리더 | 3 | 있음 | 37.50008 (8.9m) | 0번 (`IGNORE_SAME`) | 그대로 |
| 멤버 | 3 | 있음 | 37.5045 (500.4m) | 0번 (`NOT_LEADER`) | 그대로 |
| 리더 | 1 (혼자) | 없음 | 37.5045 | 리더 1번 | 리더 `REROUTING` |
| 리더 (그룹 `FINISHED`) | 3 | 있음 | 37.5045 (500.4m) | 0번 | 그대로 |

작업 19 테스트 예시 (멤버 이벤트를 적용한 뒤의 그룹 판단, 멤버 상태는 R11 상태이고 `isGroupFinished`에 넘기기 전에 R11-7로 변환한다):

| 그룹 상태 | 이벤트 적용 후 멤버 상태 | 효과 | 다음 그룹 상태 |
|---|---|---|---|
| `ACTIVE` | `[ARRIVED, ARRIVED, ARRIVED]` (마지막 멤버가 방금 도착) | `FINISH_GROUP` | `FINISHED` |
| `ACTIVE` | `[ARRIVED, DRIVING, ARRIVED]` | 없음 | `ACTIVE` |
| `ACTIVE` | `[ARRIVED]` (혼자) | `FINISH_GROUP` | `FINISHED` |
| `ACTIVE` | `[REROUTING]` (혼자) | 없음 | `ACTIVE` |
| `FINISHED` | 멤버 이벤트(예: 중복 `NAVI_ARRIVED`) | 없음 | `FINISHED` |
| `FINISHED` | 같은 판단을 다시 호출 | 없음 (`FINISH_GROUP` 다시 안 냄) | `FINISHED` |

### R12. 앞차 따라가기 화면 (제안)
- R12-1. 홈의 [앞차 따라가기]를 누르면 수락한 그룹 멤버 중 따라갈 사람을 고른다. 고르면 R3의 따라가기 요청(`REQUESTED`)이 나가고, 홈의 함께 출발 카드에 "민수님 수락 대기"를 보여 준다. 수락(`ACTIVE`)하면 [앞차 따라가기] 화면으로 넘어간다. 거절(`ENDED`)하면 홈에 그대로 있는다. 상대가 업무 중이면 R2-3대로 고를 수 없다.
- R12-2. `ACTIVE`가 되면 내 내비에 앞차 위치를 목적지로 경로를 요청한다(R6-1). 목적지 이름은 "민수 차"처럼 "<이름> 차"로 한다. 그 뒤 앞차 위치가 바뀔 때마다 마지막으로 요청한 위치와의 거리(`distanceMeters`)로 `shouldReroute`를 판단해, true일 때만 다시 요청한다(R6-2).
- R12-3. 서버가 없어 앞차(가상 인물)의 위치는 시연용 패널의 [100m 이동] [300m 이동]으로 옮긴다. 시작 위치는 고정 좌표(37.4217, 127.0947)로, 에뮬레이터 내 차 위치에서 북쪽으로 약 1km다. 각 버튼은 앞차의 현재 위치에서 북쪽으로 그만큼 더 옮긴다(누적). 예: 시작 → 100m(마지막 요청에서 100m, 다시 요청 안 함) → 100m(200m, 다시 요청) → 300m(300m, 다시 요청). 앞차 화면이 열려 있는 동안 내 위치는 2초마다 요청한다(R8-8의 `LocationPort`). 내 내비에 나가는 경로 요청은 실제 SDK 호출이다.
- R12-4. 화면은 다음을 보여 준다.
  - 머리줄: "민수 차를 따라가는 중", [홈으로], [끊기]. [끊기]는 R3-3대로 `ENDED`로 바꾸고 내 내비의 경로를 취소한다(`cancelRoute`). 그리고 홈으로 간다. [끊기]와 [홈으로]는 주행 중에도 된다.
  - 앞차까지의 거리: 내 위치(R8-8의 `LocationPort`)와 앞차 위치 사이의 `distanceMeters`. 내 위치를 아직 모르면 "거리 확인 중"이라고 보여 준다.
  - 숫자 세 개: 앞차 위치 갱신 뒤 지난 시간, 경로 다시 요청 횟수, 마지막 요청 뒤 앞차가 움직인 거리.
  - "앞차가 마지막 요청 지점에서 200m 이상 움직이면 경로를 다시 잡아요"라는 안내 한 줄.
  - 내 경로 요청이 실패하면 R11-19처럼 실패를 알리고 [다시 요청]을 보여 준다.
- R12-5. 따라가기는 한 번에 한 명이고, 그룹 주행(R11)과 동시에 하지 않는다(R4-5의 주행은 한 번에 하나). [끊기]와 거절은 주행을 "없음"으로 돌리고 그룹은 남긴다. 따라가기를 하는 동안 다른 그룹 멤버의 상태는 바뀌지 않는다.

## 2장. 구조

```
com.car.convoy
├── domain/   SDK와 무관한 규칙. 위 함수와 enum, 상태 전이. 단위 테스트의 대상
│   └── port/     NaviPort, VehiclePort, ConvoyServer 인터페이스 (domain이 정하고 data가 구현한다)
├── data/     포트의 구현
│   ├── navi/     NaviHelper 어댑터 (requestRoute, cancelRoute, 위치 2초 폴링, 도착 이벤트)
│   ├── vehicle/  Vehicle 어댑터 (기어, 방향지시등, 경고등 구독)
│   └── server/   FakeConvoyServer (인메모리 목업. 가상 멤버를 시뮬레이션)
└── ui/       Jetpack Compose 화면
```

- 의존 방향은 ui → data → domain이다. domain은 data와 ui를 import하지 않는다. (제안)
- SDK 어댑터는 인터페이스 뒤에 두고, 테스트에서는 가짜 구현을 쓴다.
- 목업 서버는 에뮬레이터 1대에서 가상 멤버(위치, 상태, 응답)를 만들어 시나리오를 시연한다. 실제 서버는 확장 계획이다.
- 시간은 `Clock` 인터페이스로 주입해, 30초와 600초 규칙을 시계 없이 테스트한다.

## 3장. 작업 목록
작업 하나는 함수 하나다. 작업마다 테스트 먼저(사람), Red 확인, 구현(AI), Green 확인 순서다.

- [x] 작업 0. Compose가 Pleos 에뮬레이터에서 뜨는지 확인한다 (안 되면 XML로 전환하고 알린다)
- [x] 작업 1. `validateInviteCode` (R1, 7행)
- [x] 작업 2. `isInviteCodeExpired` (R1, 4행)
- [x] 작업 3. `visibleInFriendList`, `canRequestFollow` (R2, 3행)
- [x] 작업 4. `nextFollowState` (R3, 6행)
- [x] 작업 5. `canInvite`, `canAcceptInvite` (R4, 7행)
- [x] 작업 6. `shouldSyncDestination` (R5, 5행)
- [x] 작업 7. `shouldReroute` (R6, 4행)
- [x] 작업 8. `gearEvent` (R7, 8행)
- [x] 작업 9. `isGroupFinished` (R7, 4행)
- [x] 작업 10. `isAccidentSuspected` (R8, 6행)
- [x] 작업 11. `accidentAction` (R8, 7행)
- [x] 작업 12. `nextFamilyState` (R9, 5행)
- [x] 작업 13. `canPerform` (R10, 14행. 그룹 목적지 변경 2행은 작업 15에서, 따라가기 요청 2행은 작업 22에서, 가족 해제·지정 취소, 주행 끝내기, 그룹 해산 6행은 작업 23·24에서 추가)
- [x] 작업 14. 그룹 주행 어댑터: `NaviPort`(NaviHelper), `VehiclePort`(기어), `FakeConvoyServer` 가상 멤버, `GroupSession` 연결 (R11-16~18, 에뮬레이터에서 수동 확인: 경로 요청·경로 시작·가상 멤버 확인, 내 차 기어 출발과 내 차 도착은 미확인. `docs/sdk-findings.md`)
- [x] 작업 15. 그룹 주행 화면 (R10 그룹 목적지 변경, R11-20, 에뮬레이터에서 수동 확인)
- [ ] 작업 16. 시나리오 시연: 그룹 초대 → 가족 지정 → 목적지로 함께(내비 목적지 가져오기 포함) → 도착 공유, 앞차 따라가기 → 재요청, 사고 의심 → 가족 알림
- [x] 작업 17. `nextMemberState` (R11, 11행). (선행: 작업 14 전에 한다)
- [x] 작업 18. 그룹 목적지 전파: 포트(`NaviPort`, `ConvoyServer`)의 가짜 구현으로, 리더가 목적지를 바꾸면 멤버 N명 모두에게 `requestRoute`가 호출되고 `NOT_LEADER`·`IGNORE_SAME`이면 호출되지 않음을 테스트한다 (R5, R11-1). (선행: 작업 14 전에 한다)
- [x] 작업 19. 그룹 종료: 전원 `ARRIVED`면 `FINISH_GROUP`이 나오고, 종료 뒤 목적지 변경은 무시됨을 테스트한다 (R7-4, R11-5, R11-9). (선행: 작업 14 전에 한다)
- [x] 작업 20. 사고 의심 어댑터: 경고등 구독(R8-6·7), 30초 타이머(`Clock`), 위치(R8-8), 시간 초과 시 서버 포트로 가족 전송, 퍼미션(`CAR_SAFETY`, `PRIVATE_CAR_SAFETY`), 화면을 앞으로 가져오기 확인(R8-11) (R8, 에뮬레이터에서 수동 확인: 경고등 구독·30초 가족 전송·[괜찮아요] 확인. R8-11 화면 앞으로 가져오기는 에뮬레이터에서 보였으나 시스템 로그가 차단(BAL_BLOCK)이라 실차 보장 안 됨, `docs/sdk-findings.md`)
- [x] 작업 21. 사고 확인·가족 알림 화면: "괜찮으세요?"와 [괜찮아요], 30초 카운트다운(주행 중에도 가능, R10), 가족에게 보낸 알림 카드(R8-10), 시연 설정 가족 표시(R8-9, 작업 24에서 가족 지정으로 바뀜) (R8, 작업 20 뒤)
- [x] 작업 22. 기존 친구 목록 화면, 작업 23에서 홈으로 대체 (R2-5, R10 따라가기 요청. 지도 대신 목록. 지도에 다른 차를 그리는 SDK API가 없음). (선행: 작업 3, 4)
- [x] 작업 23. 홈: 연결된 사람, 그룹 초대·수락, 함께 출발, [그룹 해산], 탭 제거, [홈으로]·[주행 끝내기] (R2-5, R4-5, R10, R11-20, R11-22)
- [x] 작업 24. 가족 지정·수락·지정 취소와 가족 표시 (R9-1, R9-2, R9-5, R8-9, R8-10, R10). (선행: 작업 23)
- [x] 작업 25. 앞차 따라가기 화면과 내비 연결, `cancelRoute` (R3, R6, R12). (선행: 작업 23)
- [x] 작업 26. 내비 목적지 가져오기: 내비 목적지·최근·즐겨찾기 (R11-20, R11-21)

## 4장. P1 (2차 PT 전, 이번 명세에 포함하지 않음)
방향지시등 공유, 근접·이탈 알림, 휴게·충전 조율(배터리 `getEvBatteryStateOfCharge`), 주행 상태 공유, 친구별 숨기기. P0 테스트가 모두 초록이 된 뒤 같은 방식으로 명세를 추가한다.
