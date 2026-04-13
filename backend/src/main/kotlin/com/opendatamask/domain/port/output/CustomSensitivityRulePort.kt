package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.CustomSensitivityRule

interface CustomSensitivityRulePort {
    fun findAll(): List<CustomSensitivityRule>
    fun findByIsActiveTrue(): List<CustomSensitivityRule>
    fun findById(id: Long): java.util.Optional<CustomSensitivityRule>
    fun save(rule: CustomSensitivityRule): CustomSensitivityRule
    fun deleteById(id: Long)
    fun existsByName(name: String): Boolean
}
