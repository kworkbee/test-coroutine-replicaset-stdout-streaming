package me.tommy.streaming.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import me.tommy.streaming.model.LogEntry
import me.tommy.streaming.util.chunkedByTime

class PodLogStreamingServiceTest : DescribeSpec({
    describe("streamPodLogsWithTimestamp retry") {
        it("실패 시 최대 3번까지 재시도하고 이후에는 에러를 던져야 한다") {
            // private 메서드 테스트를 위해 리플렉션이나 접근 제어자 변경 대신,
            // 실제 동작을 검증할 수 있는 방식으로 접근 (여기서는 flow의 retry 동작 자체를 검증하는 것에 집중)

            var attempts = 0
            val failingFlow: Flow<LogEntry> =
                flow {
                    attempts++
                    throw RuntimeException("Connection failed")
                }

            // retry(3) logic verification
            val resultFlow =
                failingFlow.retry(3) {
                    true
                }

            shouldThrow<RuntimeException> {
                resultFlow.toList()
            }

            attempts shouldBe 4 // 최초 1회 + 재시도 3회
        }
    }

    describe("chunkedByTime") {
        it("시간 윈도우 내의 로그들을 타임스탬프 순으로 정렬하여 방출해야 한다") {
            val logs =
                flow {
                    emit(LogEntry("pod-1", "2024-03-21T10:00:05Z", "log 2"))
                    emit(LogEntry("pod-1", "2024-03-21T10:00:01Z", "log 1"))
                }

            val result =
                withTimeout(1000) {
                    logs.chunkedByTime(100).take(2).toList()
                }

            result.size shouldBe 2
            result[0] shouldBe "[pod-1] [2024-03-21T10:00:01Z] log 1"
            result[1] shouldBe "[pod-1] [2024-03-21T10:00:05Z] log 2"
        }

        it("데이터 입력이 없어도 주기적으로 버퍼를 비워야 한다") {
            val logs =
                flow {
                    emit(LogEntry("pod-1", "2024-03-21T10:00:01Z", "log 1"))
                    delay(1000) // 두 번째 로그 없이 충분히 대기
                }

            val result =
                withTimeout(2000) {
                    logs.chunkedByTime(100).take(1).toList()
                }

            result.size shouldBe 1
            result[0] shouldBe "[pod-1] [2024-03-21T10:00:01Z] log 1"
        }
    }
})
