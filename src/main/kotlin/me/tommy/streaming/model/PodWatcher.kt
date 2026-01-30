package me.tommy.streaming.model

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.WatcherException
import kotlinx.coroutines.Job

class PodWatcher(
    private val activePodNames: MutableSet<String>,
    private val podLogsFlows: MutableMap<String, Job>,
    private val onPodAdded: (String) -> Unit,
) : Watcher<Pod> {
    override fun eventReceived(
        action: Watcher.Action,
        resource: Pod,
    ) {
        val podName = resource.metadata.name
        when (action) {
            Watcher.Action.ADDED -> {
                if (activePodNames.add(podName)) {
                    onPodAdded(podName)
                }
            }
            Watcher.Action.DELETED -> {
                activePodNames.remove(podName)
                podLogsFlows.remove(podName)?.cancel()
            }
            else -> {}
        }
    }

    override fun onClose(cause: WatcherException?) {
        if (cause != null) {
            println("[DEBUG_LOG] Watcher closed with exception: ${cause.message}")
        } else {
            println("[DEBUG_LOG] Watcher closed normally")
        }
    }
}
