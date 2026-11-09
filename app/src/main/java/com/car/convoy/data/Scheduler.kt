package com.car.convoy.data

/** 예약한 일을 취소한다. */
fun interface Cancellable {
    fun cancel()
}

/** 일정 시간 뒤에 일을 실행한다. 앱은 메인 스레드 Handler로, 테스트는 가짜 시계로 구현한다. */
interface Scheduler {
    fun schedule(delayMillis: Long, action: () -> Unit): Cancellable
}
