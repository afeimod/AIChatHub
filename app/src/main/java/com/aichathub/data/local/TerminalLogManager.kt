package com.aichathub.data.local

import com.aichathub.domain.model.LogLevel
import com.aichathub.domain.model.TerminalLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 终端日志管理器 — 内存缓存 + 持久化桥接
 * 全局单例，所有模块通过此类记录日志
 */
@Singleton
class TerminalLogManager @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _liveLogs = MutableSharedFlow<TerminalLog>(replay = 0, extraBufferCapacity = 256)
    val liveLogs: SharedFlow<TerminalLog> = _liveLogs.asSharedFlow()

    /** 内存中最近 N 条日志（用于 UI 即时显示） */
    private val memoryBuffer = mutableListOf<TerminalLog>()
    private val maxMemory = 1000

    @Synchronized
    fun getBufferedLogs(): List<TerminalLog> = memoryBuffer.toList()

    fun log(level: LogLevel, tag: String, message: String, sessionId: String? = null) {
        val entry = TerminalLog(
            level = level,
            tag = tag,
            message = message,
            sessionId = sessionId
        )
        synchronized(memoryBuffer) {
            memoryBuffer.add(entry)
            if (memoryBuffer.size > maxMemory) {
                memoryBuffer.removeAt(0)
            }
        }
        _liveLogs.tryEmit(entry)
    }

    fun info(tag: String, message: String, sessionId: String? = null) =
        log(LogLevel.INFO, tag, message, sessionId)

    fun request(tag: String, message: String, sessionId: String? = null) =
        log(LogLevel.REQUEST, tag, message, sessionId)

    fun response(tag: String, message: String, sessionId: String? = null) =
        log(LogLevel.RESPONSE, tag, message, sessionId)

    fun stream(tag: String, message: String, sessionId: String? = null) =
        log(LogLevel.STREAM, tag, message, sessionId)

    fun error(tag: String, message: String, sessionId: String? = null) =
        log(LogLevel.ERROR, tag, message, sessionId)

    fun warn(tag: String, message: String, sessionId: String? = null) =
        log(LogLevel.WARN, tag, message, sessionId)

    fun debug(tag: String, message: String, sessionId: String? = null) =
        log(LogLevel.DEBUG, tag, message, sessionId)

    @Synchronized
    fun clearMemory() {
        memoryBuffer.clear()
    }
}
