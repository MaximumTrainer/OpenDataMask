package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.ColumnGenerator
import java.util.Optional

interface ColumnGeneratorPort {
    fun findById(id: Long): Optional<ColumnGenerator>
    fun findByTableConfigurationId(tableConfigurationId: Long): List<ColumnGenerator>
    fun save(generator: ColumnGenerator): ColumnGenerator
    fun deleteById(id: Long)
    fun deleteByTableConfigurationId(tableConfigurationId: Long)
}
