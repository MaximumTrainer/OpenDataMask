package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.port.input.dto.CustomRulePreviewRequest
import com.opendatamask.domain.port.input.dto.CustomRulePreviewResult
import com.opendatamask.domain.port.input.dto.CustomSensitivityRuleRequest
import com.opendatamask.domain.port.input.dto.CustomSensitivityRuleResponse
import com.opendatamask.application.service.CustomSensitivityRuleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sensitivity-rules")
class CustomSensitivityRuleController(
    private val customSensitivityRuleService: CustomSensitivityRuleService
) {

    @GetMapping
    fun listRules(): ResponseEntity<List<CustomSensitivityRuleResponse>> =
        ResponseEntity.ok(customSensitivityRuleService.listRules())

    @GetMapping("/{id}")
    fun getRule(@PathVariable id: Long): ResponseEntity<CustomSensitivityRuleResponse> =
        ResponseEntity.ok(customSensitivityRuleService.getRule(id))

    @PostMapping
    fun createRule(
        @Valid @RequestBody request: CustomSensitivityRuleRequest
    ): ResponseEntity<CustomSensitivityRuleResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(customSensitivityRuleService.createRule(request))

    @PutMapping("/{id}")
    fun updateRule(
        @PathVariable id: Long,
        @Valid @RequestBody request: CustomSensitivityRuleRequest
    ): ResponseEntity<CustomSensitivityRuleResponse> =
        ResponseEntity.ok(customSensitivityRuleService.updateRule(id, request))

    @DeleteMapping("/{id}")
    fun deleteRule(@PathVariable id: Long): ResponseEntity<Void> {
        customSensitivityRuleService.deleteRule(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/preview")
    fun previewRule(
        @Valid @RequestBody request: CustomRulePreviewRequest
    ): ResponseEntity<List<CustomRulePreviewResult>> =
        ResponseEntity.ok(customSensitivityRuleService.previewRule(request))
}
