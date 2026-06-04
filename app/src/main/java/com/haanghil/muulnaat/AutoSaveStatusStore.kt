package com.haanghil.muulnaat

import android.os.Handler
import android.os.Looper
import java.io.Closeable

/**
 * 포그라운드 서비스의 최신 자동 저장 상태를 같은 프로세스의 Activity에 전달합니다.
 */
object AutoSaveStatusStore {
    data class Status(
        val title: String,
        val message: String,
        val progress: Int,
        val total: Int,
        val running: Boolean,
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lock = Any()
    private val listeners = mutableSetOf<(Status) -> Unit>()
    private var latest: Status? = null

    fun publish(status: Status) {
        val targets = synchronized(lock) {
            latest = if (status.running) status else null
            listeners.toList()
        }
        targets.forEach { listener -> deliver(listener, status) }
    }

    fun subscribe(listener: (Status) -> Unit): Closeable {
        val current = synchronized(lock) {
            listeners.add(listener)
            latest
        }
        if (current != null) deliver(listener, current)
        return Closeable {
            synchronized(lock) {
                listeners.remove(listener)
            }
        }
    }

    private fun deliver(listener: (Status) -> Unit, status: Status) {
        mainHandler.post {
            val isSubscribed = synchronized(lock) { listeners.contains(listener) }
            if (isSubscribed) listener(status)
        }
    }
}
