package com.opendatamask.domain.port.input

import com.opendatamask.domain.port.input.dto.CustomMappingDto

interface CustomMappingUseCase {
    fun applyCustomMapping(workspaceId: Long, mapping: CustomMappingDto)
}
