package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.ColumnGenerator
import com.opendatamask.domain.port.output.ColumnGeneratorPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ColumnGeneratorRepository : JpaRepository<ColumnGenerator, Long>, ColumnGeneratorPort {
    override fun findById(id: Long): Optional<ColumnGenerator>
    override fun findByTableConfigurationId(tableConfigurationId: Long): List<ColumnGenerator>
    override fun save(generator: ColumnGenerator): ColumnGenerator
    override fun deleteById(id: Long)
    override fun deleteByTableConfigurationId(tableConfigurationId: Long)
}
