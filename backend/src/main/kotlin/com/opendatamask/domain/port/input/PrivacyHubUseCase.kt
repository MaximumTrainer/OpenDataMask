package com.opendatamask.domain.port.input

import com.opendatamask.dto.PrivacyHubSummary
import com.opendatamask.dto.PrivacyRecommendation

interface PrivacyHubUseCase {
    fun getSummary(workspaceId: Long): PrivacyHubSummary
    fun getRecommendations(workspaceId: Long): List<PrivacyRecommendation>
    fun applyRecommendations(workspaceId: Long): Int
}
