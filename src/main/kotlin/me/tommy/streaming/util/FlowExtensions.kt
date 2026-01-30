package me.tommy.streaming.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tommy.streaming.model.LogEntry

fun Flow<LogEntry>.chunkedByTime(windowMillis: Long): Flow<String> =
    channelFlow {
        val buffer = mutableListOf<LogEntry>()
        val mutex = Mutex()

        suspend fun flushBuffer() {
            mutex.withLock {
                if (buffer.isNotEmpty()) {
                    val logsToEmit = buffer.toList().sortedBy { it.timestamp }
                    buffer.clear()
                    logsToEmit.forEach { send(it.format()) }
                }
            }
        }

        launch {
            while (isActive) {
                delay(windowMillis)
                flushBuffer()
            }
        }

        this@chunkedByTime.collect { entry ->
            mutex.withLock {
                buffer.add(entry)
            }
        }
        flushBuffer()
    }
