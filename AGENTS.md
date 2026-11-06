# AGENTS.md

## 1. 프로젝트
Convoy는 친구·가족 차량을 서로 동의해 연결하고, 목적지와 안전을 공유하는 차량용 그룹 내비게이션 앱이다(Pleos Connect, Kotlin).
현대차그룹 차량용 앱 개발 공모전 출품작이다. 1차 접수 마감 2026-11-15, 2차 PT 12-03.

## 2. 기준 문서
- `REQUEST.md`: 무엇을 원하는가 (요청서)
- `SPEC.md`: 정확한 규칙과 예시표 (명세). 코드와 다르면 코드가 틀린 것이다
- `docs/sdk-findings.md`: SDK 시험으로 확인된 가능/불가능
- 문서끼리 충돌하면 SPEC.md가 우선이고, 모르는 것은 추측하지 말고 물어본다

## 3. 만드는 파일 (패키지 `com.car.convoy`)
- `domain/`: SDK와 무관한 규칙과 상태(사고 의심 판단, 이탈 거리, 업무 중 규칙, 그룹 상태). 단위 테스트의 주 대상
- `data/`: SDK 어댑터(NaviHelper, Vehicle)와 목업 서버. SDK는 반드시 인터페이스 뒤에 둔다
- `ui/`: Jetpack Compose 화면
- 단위 테스트는 `app/src/test/`
- `MainActivity`와 `VehicleProbe`의 시험 코드는 스파이크다. 본 코드로 가져가지 말고 필요한 부분만 새로 쓴다

## 4. 실행 방법
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-24.jdk/Contents/Home
./gradlew :app:testDebugUnitTest      # 단위 테스트
./gradlew :app:assembleDebug          # 빌드
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.car.convoy/.MainActivity
adb -s emulator-5554 forward tcp:8088 tcp:8088   # Virtual Console: http://127.0.0.1:8088/
```
- 에뮬레이터: AVD `Pleos_Connect_v2.1.0`(API 34, Cold boot, 2560x1440 가로)
- 사용 가능한 SDK는 NaviHelper와 Vehicle State뿐이다. FusedLocation, TextToSpeech, SpeechToText, Vehicle Control은 아직 열리지 않았으니 쓰지 않는다

## 5. 고치면 안 되는 파일
허락 없이 고치지 않는다.
- `REQUEST.md`, `SPEC.md`
- 사람이 쓴 테스트(`app/src/test/`). 테스트가 실패하면 기대값이 아니라 코드를 고친다
- 규칙을 바꿔야 하면 코드가 아니라 SPEC.md를 먼저 바꾸자고 제안한다

## 6. 일하는 방법
- 순서는 명세 → 테스트 → 구현이다. 테스트 먼저 쓰고 실패(Red)를 확인한 뒤 통과할 만큼만 구현(Green)한다
- 첫 테스트는 사람이 쓴다. AI가 추가하는 테스트는 새 파일에 쓴다
- SDK 시그니처를 추측하지 않는다. 모르면 AAR을 `javap`로 확인하거나 문서를 읽는다
- 판단 로직은 SDK 없이 테스트한다. SDK 호출은 인터페이스 뒤에 두고 테스트에서는 가짜를 쓴다
- UI 첫 작업으로 Compose가 에뮬레이터에서 동작하는지 확인한다. 안 되면 XML Views로 바꾸되 먼저 알린다
- 주행 중 UI는 조작을 최소화한다(큰 터치 영역, 텍스트 최소화, 버튼 하나)
- 기능 브랜치(`feat/…`)에서 작은 커밋으로 일한다. 명세·테스트 커밋과 구현 커밋을 나눈다
- **push와 머지는 하지 않는다.** main에 직접 커밋하지 않는다
- Claude는 구현하고 Codex는 리뷰한다. 같은 파일을 동시에 고치지 않는다
- 비밀 값(키, 시크릿)은 코드와 문서에 넣지 않는다

## 7. 끝났을 때 보고할 것
- 단위 테스트 결과(통과/실패 개수, 실행 출력 그대로)
- 빌드 성공 여부(`assembleDebug`)
- 에뮬레이터에서 직접 확인한 것과 확인하지 못한 것을 구분해서
