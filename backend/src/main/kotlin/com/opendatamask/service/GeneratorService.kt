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
