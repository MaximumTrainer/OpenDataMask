package com.opendatamask.application.service

import com.opendatamask.domain.model.GeneratorPreset
import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.port.output.GeneratorPresetPort
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class DataSeeder(
    private val generatorPresetRepository: GeneratorPresetPort
) {
    private val systemPresets = listOf(
        "First Name" to GeneratorType.FIRST_NAME,
        "Last Name" to GeneratorType.LAST_NAME,
        "Full Name" to GeneratorType.FULL_NAME,
        "Email Address" to GeneratorType.EMAIL,
        "Phone Number" to GeneratorType.PHONE,
        "Street Address" to GeneratorType.STREET_ADDRESS,
        "City" to GeneratorType.CITY,
        "State" to GeneratorType.STATE,
        "ZIP Code" to GeneratorType.ZIP_CODE,
        "Country" to GeneratorType.COUNTRY,
        "Postal Code" to GeneratorType.POSTAL_CODE,
        "SSN" to GeneratorType.SSN,
        "Credit Card" to GeneratorType.CREDIT_CARD,
        "IP Address" to GeneratorType.IP_ADDRESS,
        "IPv6 Address" to GeneratorType.IPV6_ADDRESS,
        "MAC Address" to GeneratorType.MAC_ADDRESS,
        "Username" to GeneratorType.USERNAME,
        "Password" to GeneratorType.PASSWORD,
        "Date of Birth" to GeneratorType.BIRTH_DATE,
        "UUID" to GeneratorType.UUID,
        "Null Value" to GeneratorType.NULL,
        "IBAN" to GeneratorType.IBAN,
        "Organization" to GeneratorType.ORGANIZATION,
        "Account Number" to GeneratorType.ACCOUNT_NUMBER
    )

    @PostConstruct
    fun seedSystemPresets() {
        val existingNames = generatorPresetRepository.findByIsSystemTrue().map { it.name }.toSet()
        systemPresets
            .filter { (name, _) -> name !in existingNames }
            .forEach { (name, type) ->
                generatorPresetRepository.save(
                    GeneratorPreset(name = name, generatorType = type, isSystem = true)
                )
            }
    }
}