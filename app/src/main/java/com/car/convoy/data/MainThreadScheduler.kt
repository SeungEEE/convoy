package com.car.convoy.data

import android.os.Handler
import android.os.Looper

/** 메인 스레드에서 예약한 일을 실행한다. */
class MainThreadScheduler : Scheduler {
    private val handler = Handler(Looper.getMainLooper())

    override fun schedule(delayMillis: Long, action: () -> Unit): Cancellable {
        val runnable = Runnable(action)
        handler.postDelayed(runnable, delayMillis)
        return Cancellable { handler.removeCallbacks(runnable) }
    }

    /** SDK 콜백을 메인 스레드로 옮긴다. GroupController는 한 스레드에서만 부른다. */
    fun post(action: () -> Unit) {
        handler.post(action)
    }
}
