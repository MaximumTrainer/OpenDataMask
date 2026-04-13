package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.CustomSensitivityRule
import com.opendatamask.domain.port.output.CustomSensitivityRulePort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CustomSensitivityRuleRepository : JpaRepository<CustomSensitivityRule, Long>, CustomSensitivityRulePort {
    override fun findAll(): List<CustomSensitivityRule>
    override fun findByIsActiveTrue(): List<CustomSensitivityRule>
    override fun findById(id: Long): Optional<CustomSensitivityRule>
    override fun save(rule: CustomSensitivityRule): CustomSensitivityRule
    override fun deleteById(id: Long)
    override fun existsByName(name: String): Boolean
}
