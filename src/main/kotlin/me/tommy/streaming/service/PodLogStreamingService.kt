package me.tommy.streaming.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import me.tommy.streaming.infrastructure.KubernetesPodProvider
import me.tommy.streaming.model.LogEntry
import me.tommy.streaming.model.PodWatcher
import me.tommy.streaming.util.chunkedByTime
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader

@Service
class PodLogStreamingService(
    private val podProvider: KubernetesPodProvider,
) {
    fun streamReplicaSetLogs(
        namespace: String,
        replicaSetName: String,
    ): Flow<String> =
        channelFlow {
            val selector = podProvider.getReplicaSetSelector(namespace, replicaSetName)
            val initialPods = podProvider.getPodsBySelector(namespace, selector)

            val activePodNames = initialPods
                .map { it.metadata.name }
                .toMutableSet()
            val podLogsFlows = mutableMapOf<String, Job>()

            coroutineScope {
                val outputChannel =
                    Channel<LogEntry>(
                        capacity = 1000,
                        onBufferOverflow = BufferOverflow.DROP_OLDEST,
                    )

                fun startStreaming(podName: String) {
                    if (podLogsFlows.containsKey(podName)) return
                    podLogsFlows[podName] =
                        launch {
                            streamPodLogsWithTimestamp(namespace, podName).collect {
                                outputChannel.send(it)
                            }
                        }
                }

                initialPods.forEach { startStreaming(it.metadata.name) }

                val watch =
                    podProvider.watchPods(
                        namespace,
                        selector,
                        PodWatcher(
                            activePodNames = activePodNames,
                            podLogsFlows = podLogsFlows,
                            onPodAdded = { startStreaming(it) },
                        ),
                    )

                launch {
                    try {
                        outputChannel.consumeAsFlow()
                            .chunkedByTime(100)
                            .collect { send(it) }
                    } finally {
                        watch.close()
                        podLogsFlows.values.forEach { it.cancel() }
                    }
                }
            }
        }

    private fun streamPodLogsWithTimestamp(
        namespace: String,
        podName: String,
    ): Flow<LogEntry> =
        flow {
            val logWatch = podProvider.watchPodLog(namespace, podName)

            logWatch.use { watch ->
                BufferedReader(InputStreamReader(watch.output)).use { reader ->
                    reader.lineSequence().forEach { line ->
                        currentCoroutineContext().ensureActive()
                        emit(LogEntry.parse(podName, line))
                    }
                }
            }
        }.retry(3) { cause ->
            println("[DEBUG_LOG] Log stream for $podName failed, retrying...: ${cause.message}")
            delay(1000)
            true
        }.flowOn(Dispatchers.IO)
}
