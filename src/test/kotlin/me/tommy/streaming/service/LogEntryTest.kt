package me.tommy.streaming.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import me.tommy.streaming.model.LogEntry

class LogEntryTest : DescribeSpec({
    describe("LogEntry.parse") {
        it("타임스탬프와 본문이 있는 올바른 로그 라인을 파싱해야 한다") {
            val podName = "test-pod"
            val line = "2024-03-21T10:00:00.123456Z Hello World"

            val entry = LogEntry.parse(podName, line)

            entry.podName shouldBe podName
            entry.timestamp shouldBe "2024-03-21T10:00:00.123456Z"
            entry.body shouldBe "Hello World"
        }

        it("타임스탬프만 있는 경우 본문은 빈 문자열이어야 한다") {
            val podName = "test-pod"
            val line = "2024-03-21T10:00:00.123456Z "

            val entry = LogEntry.parse(podName, line)

            entry.timestamp shouldBe "2024-03-21T10:00:00.123456Z"
            entry.body shouldBe ""
        }

        it("형식이 올바르지 않은 경우 timestamp를 unknown으로 설정해야 한다") {
            val podName = "test-pod"
            val line = "invalid-log-line"

            val entry = LogEntry.parse(podName, line)

            entry.podName shouldBe podName
            entry.timestamp shouldBe "unknown"
            entry.body shouldBe "invalid-log-line"
        }
    }

    describe("LogEntry.format") {
        it("지정된 형식으로 문자열을 반환해야 한다") {
            val entry = LogEntry("pod-1", "2024-03-21T10:00:00Z", "Log message")

            entry.format() shouldBe "[pod-1] [2024-03-21T10:00:00Z] Log message"
        }
    }
})
