package com.car.convoy.data

/** 테스트용 시계. advanceBy로 시간을 흘려 예약된 일을 실행한다. */
class FakeScheduler : Scheduler {
    private class Task(val at: Long, val action: () -> Unit) { var cancelled = false }

    private var now = 0L
    private val tasks = mutableListOf<Task>()

    override fun nowMillis(): Long = now

    override fun schedule(delayMillis: Long, action: () -> Unit): Cancellable {
        val task = Task(now + delayMillis, action)
        tasks += task
        return Cancellable { task.cancelled = true }
    }

    fun advanceBy(millis: Long) {
        val until = now + millis
        while (true) {
            val next = tasks.filter { !it.cancelled && it.at <= until }.minByOrNull { it.at } ?: break
            tasks -= next
            now = next.at
            next.action()
        }
        now = until
    }
}
