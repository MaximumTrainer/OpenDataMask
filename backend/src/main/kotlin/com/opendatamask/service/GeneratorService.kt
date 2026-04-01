package com.opendatamask.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.opendatamask.model.ColumnGenerator
import com.opendatamask.model.GeneratorType
import net.datafaker.Faker
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class GeneratorService {
    private val faker = Faker()
    private val mapper = jacksonObjectMapper()

    fun generateValue(type: GeneratorType, originalValue: Any?, params: Map<String, String>?): Any? {
        return when (type) {
            GeneratorType.NAME -> faker.name().fullName()
            GeneratorType.EMAIL -> faker.internet().emailAddress()
            GeneratorType.PHONE -> faker.phoneNumber().phoneNumber()
            GeneratorType.ADDRESS -> faker.address().fullAddress()
            GeneratorType.SSN -> faker.idNumber().ssnValid()
            GeneratorType.CREDIT_CARD -> faker.finance().creditCard()
            GeneratorType.DATE -> faker.date().past(365 * 10, TimeUnit.DAYS).toString()
            GeneratorType.UUID -> UUID.randomUUID().toString()
            GeneratorType.CONSTANT -> params?.get("value") ?: ""
            GeneratorType.NULL -> null
            GeneratorType.CUSTOM -> params?.get("value") ?: originalValue
            GeneratorType.FIRST_NAME -> faker.name().firstName()
            GeneratorType.LAST_NAME -> faker.name().lastName()
            GeneratorType.FULL_NAME -> faker.name().fullName()
            GeneratorType.STREET_ADDRESS -> faker.address().streetAddress()
            GeneratorType.CITY -> faker.address().city()
            GeneratorType.STATE -> faker.address().stateAbbr()
            GeneratorType.ZIP_CODE -> faker.address().zipCode()
            GeneratorType.COUNTRY -> faker.address().country()
            GeneratorType.POSTAL_CODE -> faker.address().zipCode()
            GeneratorType.GPS_COORDINATES -> "${faker.address().latitude()},${faker.address().longitude()}"
            GeneratorType.USERNAME -> faker.internet().emailAddress().substringBefore("@")
            GeneratorType.PASSWORD -> faker.internet().password(8, 20, true, true, true)
            GeneratorType.IBAN -> faker.finance().iban()
            GeneratorType.SWIFT_CODE -> faker.finance().bic()
            GeneratorType.MONEY_AMOUNT -> faker.commerce().price()
            GeneratorType.BTC_ADDRESS -> faker.regexify("1[A-HJ-NP-Za-km-z1-9]{33}")
            GeneratorType.PASSPORT_NUMBER -> faker.regexify("[A-Z]{2}[0-9]{7}")
            GeneratorType.DRIVERS_LICENSE -> faker.regexify("[A-Z][0-9]{7}")
            GeneratorType.BIRTH_DATE -> faker.date().birthday().toString()
            GeneratorType.GENDER -> listOf("Male", "Female", "Non-binary", "Prefer not to say").random()
            GeneratorType.ICD_CODE -> faker.regexify("[A-Z][0-9]{2}\\.[0-9]{1,2}")
            GeneratorType.MEDICAL_RECORD_NUMBER -> faker.regexify("MRN-[0-9]{8}")
            GeneratorType.HEALTH_PLAN_NUMBER -> faker.regexify("HPN-[0-9]{10}")
            GeneratorType.IP_ADDRESS -> faker.internet().ipV4Address()
            GeneratorType.IPV6_ADDRESS -> faker.internet().ipV6Address()
            GeneratorType.MAC_ADDRESS -> faker.internet().macAddress()
            GeneratorType.URL -> faker.internet().url()
            GeneratorType.VIN -> faker.vehicle().vin()
            GeneratorType.LICENSE_PLATE -> faker.vehicle().licensePlate()
            GeneratorType.ORGANIZATION -> faker.company().name()
            GeneratorType.ACCOUNT_NUMBER -> faker.regexify("[0-9]{10}")
            GeneratorType.CONDITIONAL -> originalValue
            GeneratorType.PARTIAL_MASK -> {
                val s = originalValue?.toString() ?: return null
                val keepLast = params?.get("keepLast")?.toIntOrNull() ?: 4
                val maskChar = params?.get("maskChar") ?: "*"
                if (s.length <= keepLast) s else maskChar.repeat(s.length - keepLast) + s.takeLast(keepLast)
            }
            GeneratorType.FORMAT_PRESERVING -> {
                val s = originalValue?.toString() ?: return null
                s.map { c ->
                    when {
                        c.isDigit() -> faker.number().numberBetween(0, 9).toString()[0]
                        c.isLetter() -> faker.regexify("[a-z]")[0]
                        else -> c
                    }
                }.joinToString("")
            }
            GeneratorType.SEQUENTIAL -> {
                params?.get("current")?.toLongOrNull()?.let { it + 1 } ?: 1L
            }
            GeneratorType.RANDOM_INT -> {
                val min = params?.get("min")?.toLongOrNull() ?: 1L
                val max = params?.get("max")?.toLongOrNull() ?: 999999L
                faker.number().numberBetween(min, max)
            }
        }
    }

    fun applyGenerators(row: Map<String, Any?>, generators: List<ColumnGenerator>): Map<String, Any?> {
        val result = row.toMutableMap()
        for (generator in generators) {
            val params: Map<String, String>? = generator.generatorParams?.let {
                try { mapper.readValue(it) } catch (e: Exception) { null }
            }
            result[generator.columnName] = generateValue(generator.generatorType, row[generator.columnName], params)
        }
        return result
    }

    fun generateRows(generators: List<ColumnGenerator>, count: Int): List<Map<String, Any?>> {
        return (1..count).map {
            generators.associate { generator ->
                val params: Map<String, String>? = generator.generatorParams?.let {
                    try { mapper.readValue(it) } catch (e: Exception) { null }
                }
                generator.columnName to generateValue(generator.generatorType, null, params)
            }
        }
    }
}
