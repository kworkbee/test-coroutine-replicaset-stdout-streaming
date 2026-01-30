package me.tommy.streaming.infrastructure

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.Watch
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.LogWatch
import org.springframework.stereotype.Component

@Component
class KubernetesPodProvider(
    private val kubernetesClient: KubernetesClient,
) {
    fun getReplicaSetSelector(
        namespace: String,
        replicaSetName: String,
    ): Map<String, String> {
        val replicaSet =
            requireNotNull(
                kubernetesClient.apps().replicaSets()
                    .inNamespace(namespace)
                    .withName(replicaSetName)
                    .get(),
            ) { "ReplicaSet not found" }
        return replicaSet.spec.selector.matchLabels
    }

    fun getPodsBySelector(
        namespace: String,
        selector: Map<String, String>,
    ): List<Pod> {
        return kubernetesClient.pods()
            .inNamespace(namespace)
            .withLabels(selector)
            .list()
            .items
    }

    fun watchPods(
        namespace: String,
        selector: Map<String, String>,
        watcher: Watcher<Pod>,
    ): Watch {
        return kubernetesClient.pods()
            .inNamespace(namespace)
            .withLabels(selector)
            .watch(watcher)
    }

    fun watchPodLog(
        namespace: String,
        podName: String,
    ): LogWatch {
        return kubernetesClient.pods()
            .inNamespace(namespace)
            .withName(podName)
            .usingTimestamps()
            .watchLog()
    }
}
