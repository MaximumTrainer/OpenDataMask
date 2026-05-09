package com.opendatamask.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class JobProgressEvent(
    val jobId: Long,
    val tableName: String?,
    val status: String,
    val rowsProcessed: Long,
    val tablesProcessed: Int,
    val tablesTotal: Int,
    val message: String?
)

@Component
class JobProgressEmitterRegistry {
    private val logger = LoggerFactory.getLogger(JobProgressEmitterRegistry::class.java)
    private val emitters = ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>()

    fun register(jobId: Long): SseEmitter {
        val emitter = SseEmitter(300_000L) // 5-minute timeout
        emitters.getOrPut(jobId) { CopyOnWriteArrayList() }.add(emitter)
        emitter.onCompletion { remove(jobId, emitter) }
        emitter.onTimeout { remove(jobId, emitter) }
        emitter.onError { remove(jobId, emitter) }
        return emitter
    }

    fun publish(event: JobProgressEvent) {
        val list = emitters[event.jobId] ?: return
        val dead = mutableListOf<SseEmitter>()
        for (emitter in list) {
            try {
                emitter.send(SseEmitter.event().name("progress").data(event))
            } catch (e: Exception) {
                logger.debug("SSE emitter for job ${event.jobId} failed, removing", e)
                dead.add(emitter)
            }
        }
        list.removeAll(dead)
    }

    fun complete(jobId: Long) {
        emitters[jobId]?.forEach { emitter ->
            try { emitter.complete() } catch (_: Exception) {}
        }
        emitters.remove(jobId)
    }

    private fun remove(jobId: Long, emitter: SseEmitter) {
        emitters[jobId]?.remove(emitter)
    }
}
