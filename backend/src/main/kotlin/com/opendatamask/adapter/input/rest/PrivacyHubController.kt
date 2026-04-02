package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.port.input.dto.PrivacyHubSummary
import com.opendatamask.domain.port.input.dto.PrivacyRecommendation
import com.opendatamask.application.service.PrivacyHubService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/privacy-hub")
class PrivacyHubController(
    private val privacyHubService: PrivacyHubService
) {

    @GetMapping
    fun getSummary(@PathVariable workspaceId: Long): PrivacyHubSummary =
        privacyHubService.getSummary(workspaceId)

    @GetMapping("/recommendations")
    fun getRecommendations(@PathVariable workspaceId: Long): List<PrivacyRecommendation> =
        privacyHubService.getRecommendations(workspaceId)

    @PostMapping("/recommendations/apply")
    fun applyRecommendations(@PathVariable workspaceId: Long): ResponseEntity<Map<String, Int>> {
        val count = privacyHubService.applyRecommendations(workspaceId)
        return ResponseEntity.ok(mapOf("applied" to count))
    }
}

