---
name: 기능
about: SPEC.md의 요구사항 하나를 구현한다 (명세 → 테스트 → 구현)
title: "[기능] "
labels: feature
---

## 연결된 요구사항
- SPEC.md: R__ (작업 __)

## 원하는 것
<!-- 한 문장. "~하면, 시스템은 ~한다" 형태로 -->

## 명세 상태
- [ ] SPEC.md에 규칙과 예시표가 이미 있다
- [ ] SPEC.md를 먼저 바꿔야 한다 (별도 `docs/` PR을 먼저 올린다)

## 예시표(경계값 포함)
<!-- SPEC.md 표를 링크하거나, 바뀌는 행만 적는다. 예시표 한 줄이 테스트 한 줄이다 -->

| 입력 | 기대 결과 |
|---|---|
| | |

## 범위 밖 (하지 말 것)
<!-- 이번 이슈에서 건드리지 않을 것. 예: SPEC.md, 사람이 쓴 테스트, 의존성·퍼미션 -->

## SDK·퍼미션 영향
- [ ] 없음 (순수 `domain/` 로직)
- [ ] NaviHelper 사용
- [ ] Vehicle 사용 (필요한 퍼미션: )
- [ ] 사용 불가 SDK(FusedLocation, TTS, STT, Vehicle Control)가 필요하다 → 대체안을 적는다

## 완료 기준
- [ ] 단위 테스트가 모두 통과한다 (`./gradlew :app:testDebugUnitTest`)
- [ ] `./gradlew :app:assembleDebug`가 통과한다
- [ ] 에뮬레이터에서 확인할 것: 
