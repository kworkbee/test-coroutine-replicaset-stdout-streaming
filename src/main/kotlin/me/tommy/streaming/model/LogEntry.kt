package me.tommy.streaming.model

data class LogEntry(
    val podName: String,
    val timestamp: String,
    val body: String,
) {
    fun format(): String = "[$podName] [$timestamp] $body"

    companion object {
        fun parse(
            podName: String,
            line: String,
        ): LogEntry {
            val parts = line.split(" ", limit = 2)
            return if (parts.size == 2) {
                LogEntry(podName, parts[0], parts[1])
            } else {
                LogEntry(podName, "unknown", line)
            }
        }
    }
}
