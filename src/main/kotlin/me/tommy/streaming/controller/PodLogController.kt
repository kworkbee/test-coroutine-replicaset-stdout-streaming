package me.tommy.streaming.controller

import kotlinx.coroutines.flow.Flow
import me.tommy.streaming.service.PodLogStreamingService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class PodLogController(
    private val podLogStreamingService: PodLogStreamingService,
) {
    @GetMapping(
        value = ["/logs/{namespace}/replicaset/{replicaSetName}"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE],
    )
    fun streamReplicaSetLogs(
        @PathVariable namespace: String,
        @PathVariable replicaSetName: String,
    ): Flow<String> = podLogStreamingService.streamReplicaSetLogs(namespace, replicaSetName)
}
