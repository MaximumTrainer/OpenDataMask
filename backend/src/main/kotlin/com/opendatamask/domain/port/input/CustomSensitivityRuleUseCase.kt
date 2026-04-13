package com.opendatamask.domain.port.input

import com.opendatamask.domain.port.input.dto.CustomRulePreviewRequest
import com.opendatamask.domain.port.input.dto.CustomRulePreviewResult
import com.opendatamask.domain.port.input.dto.CustomSensitivityRuleRequest
import com.opendatamask.domain.port.input.dto.CustomSensitivityRuleResponse

interface CustomSensitivityRuleUseCase {
    fun listRules(): List<CustomSensitivityRuleResponse>
    fun getRule(id: Long): CustomSensitivityRuleResponse
    fun createRule(request: CustomSensitivityRuleRequest): CustomSensitivityRuleResponse
    fun updateRule(id: Long, request: CustomSensitivityRuleRequest): CustomSensitivityRuleResponse
    fun deleteRule(id: Long)
    fun previewRule(request: CustomRulePreviewRequest): List<CustomRulePreviewResult>
}
