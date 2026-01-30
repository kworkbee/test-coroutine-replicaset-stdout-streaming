package me.tommy.streaming

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TestCoroutineReplicasetStdoutStreamingApplication

fun main(args: Array<String>) {
    runApplication<TestCoroutineReplicasetStdoutStreamingApplication>(*args)
}
