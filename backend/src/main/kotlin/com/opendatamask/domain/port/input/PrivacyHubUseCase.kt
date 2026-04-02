package com.opendatamask.domain.port.input

import com.opendatamask.adapter.input.rest.dto.PrivacyHubSummary
import com.opendatamask.adapter.input.rest.dto.PrivacyRecommendation

interface PrivacyHubUseCase {
    fun getSummary(workspaceId: Long): PrivacyHubSummary
    fun getRecommendations(workspaceId: Long): List<PrivacyRecommendation>
    fun applyRecommendations(workspaceId: Long): Int
}
