package com.opendatamask.application.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.opendatamask.domain.model.ColumnGenerator
import com.opendatamask.domain.model.ConsistencyMode
import com.opendatamask.domain.model.GeneratorType
import net.datafaker.Faker
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class GeneratorService(
    @Value("\${opendatamask.encryption.key:0123456789abcdef}") private val encryptionKey: String
) {
    private val faker = Faker()
    private val mapper = jacksonObjectMapper()
    private val sequentialCounters = ConcurrentHashMap<String, AtomicLong>()

    fun computeWorkspaceSecret(workspaceId: Long): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((encryptionKey + workspaceId).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hmacSeed(workspaceSecret: String, originalValue: String): Long {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(workspaceSecret.toByteArray(), "HmacSHA256"))
        val bytes = mac.doFinal(originalValue.toByteArray())
        return ByteBuffer.wrap(bytes).long
    }

    fun generateValueConsistent(
        type: GeneratorType,
        params: Map<String, String>?,
        originalValue: String,
        workspaceSecret: String
    ): String {
        val seed = hmacSeed(workspaceSecret, originalValue)
        val seededFaker = Faker(java.util.Random(seed))
        return generateValue(type, originalValue, params, seededFaker)?.toString() ?: ""
    }

    fun resetSequentialCounters() {
        sequentialCounters.clear()
    }

    fun generateValue(
        type: GeneratorType,
        originalValue: Any?,
        params: Map<String, String>?,
        faker: Faker = this.faker,
        columnKey: String? = null,
        rawParams: String? = null
    ): Any? {
        return when (type) {
            GeneratorType.NAME -> faker.name().fullName()
            GeneratorType.EMAIL -> faker.internet().emailAddress()
            GeneratorType.PHONE -> faker.phoneNumber().phoneNumber()
            GeneratorType.ADDRESS -> faker.address().fullAddress()
            GeneratorType.SSN -> faker.idNumber().ssnValid()
            GeneratorType.CREDIT_CARD -> faker.finance().creditCard()
            GeneratorType.DATE -> java.sql.Date(faker.date().past(365 * 10, TimeUnit.DAYS).time)
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
            GeneratorType.MONEY_AMOUNT -> java.math.BigDecimal(faker.commerce().price())
            GeneratorType.BTC_ADDRESS -> faker.regexify("1[A-HJ-NP-Za-km-z1-9]{33}")
            GeneratorType.PASSPORT_NUMBER -> faker.regexify("[A-Z]{2}[0-9]{7}")
            GeneratorType.DRIVERS_LICENSE -> faker.regexify("[A-Z][0-9]{7}")
            GeneratorType.BIRTH_DATE -> java.sql.Date(faker.date().birthday().time)
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
            GeneratorType.TITLE -> faker.name().prefix()
            GeneratorType.JOB_TITLE -> faker.job().title()
            GeneratorType.NATIONALITY -> faker.nation().nationality()
            GeneratorType.COMPANY_NAME -> faker.company().name()
            GeneratorType.DEPARTMENT -> faker.commerce().department()
            GeneratorType.CURRENCY_CODE -> faker.currency().code()
            GeneratorType.DOMAIN_NAME -> faker.internet().domainName()
            GeneratorType.USER_AGENT -> faker.internet().userAgent().replace(Regex("\\s+"), " ")
            GeneratorType.LATITUDE -> faker.address().latitude()
            GeneratorType.LONGITUDE -> faker.address().longitude()
            GeneratorType.TIME_ZONE -> faker.address().timeZone()
            GeneratorType.BOOLEAN -> faker.bool().bool()
            GeneratorType.LOREM -> faker.lorem().paragraph()
            GeneratorType.TIMESTAMP -> java.sql.Timestamp(faker.date().past(365 * 10, java.util.concurrent.TimeUnit.DAYS).time)
            GeneratorType.PARTIAL_MASK -> {
                val s = originalValue?.toString() ?: return null
                val maskChar = (params?.get("maskChar") ?: "*").firstOrNull() ?: '*'
                val revealFromIndex = if (params?.containsKey("keepLast") == true) {
                    val keepLast = params["keepLast"]?.toIntOrNull() ?: 4
                    s.length - keepLast
                } else {
                    val maskEnd = params?.get("maskEnd")?.toIntOrNull() ?: -4
                    if (maskEnd < 0) s.length + maskEnd else maskEnd
                }
                s.mapIndexed { i, c ->
                    if (i < revealFromIndex) {
                        if (c.isLetterOrDigit()) maskChar else c
                    } else c
                }.joinToString("")
            }
            GeneratorType.FORMAT_PRESERVING -> {
                val s = originalValue?.toString() ?: return null
                s.map { c ->
                    when {
                        c.isDigit() -> ('0'.code + faker.number().numberBetween(0, 10)).toChar()
                        c.isUpperCase() -> ('A'.code + faker.number().numberBetween(0, 26)).toChar()
                        c.isLowerCase() -> ('a'.code + faker.number().numberBetween(0, 26)).toChar()
                        else -> c
                    }
                }.joinToString("")
            }
            GeneratorType.SEQUENTIAL -> {
                val start = params?.get("start")?.toLongOrNull() ?: 1L
                val step = params?.get("step")?.toLongOrNull() ?: 1L
                val key = columnKey ?: "default"
                val counter = sequentialCounters.computeIfAbsent(key) { AtomicLong(start - step) }
                counter.addAndGet(step)
            }
            GeneratorType.RANDOM_INT -> {
                val min = params?.get("min")?.toLongOrNull() ?: 1L
                val max = params?.get("max")?.toLongOrNull() ?: 999999L
                faker.number().numberBetween(min, max)
            }
            GeneratorType.CONDITIONAL -> {
                val jsonParams = rawParams?.let {
                    try { mapper.readValue<Map<String, Any?>>(it) } catch (e: Exception) { null }
                }
                if (jsonParams != null) {
                    val conditions = jsonParams["conditions"] as? List<*>
                    val defaultTypeName = jsonParams["default"] as? String ?: "NULL"
                    val defaultType = try {
                        GeneratorType.valueOf(defaultTypeName)
                    } catch (e: Exception) {
                        GeneratorType.NULL
                    }
                    val originalStr = originalValue?.toString()
                    val matchedCondition = conditions
                        ?.filterIsInstance<Map<String, *>>()
                        ?.find { condition ->
                            val whenExpr = condition["when"] as? String ?: return@find false
                            evaluateWhenCondition(whenExpr, originalStr)
                        }
                    if (matchedCondition != null) {
                        val thenTypeName = matchedCondition["then"] as? String ?: "CONSTANT"
                        val thenType = try {
                            GeneratorType.valueOf(thenTypeName)
                        } catch (e: Exception) {
                            GeneratorType.CONSTANT
                        }
                        val thenValue = matchedCondition["thenValue"] as? String
                        generateValue(thenType, originalValue, thenValue?.let { mapOf("value" to it) }, faker, columnKey)
                    } else {
                        generateValue(defaultType, originalValue, params, faker, columnKey)
                    }
                } else {
                    originalValue
                }
            }
        }
    }

    private fun evaluateWhenCondition(whenExpr: String, originalValue: String?): Boolean {
        val singleQuoteMatch = Regex("value\\s*==\\s*'([^']*)'").find(whenExpr)
        val doubleQuoteMatch = Regex("""value\s*==\s*"([^"]*)"""").find(whenExpr)
        val expected = singleQuoteMatch?.groupValues?.get(1)
            ?: doubleQuoteMatch?.groupValues?.get(1)
            ?: whenExpr
        return originalValue == expected
    }

    fun applyGenerators(
        row: Map<String, Any?>,
        generators: List<ColumnGenerator>,
        workspaceSecret: String? = null
    ): Map<String, Any?> {
        val result = row.toMutableMap()

        // Pre-compute one seeded Faker per link group (CONSISTENT mode only)
        val linkGroupFakers = mutableMapOf<String, Faker>()
        if (workspaceSecret != null) {
            generators
                .filter { it.consistencyMode == ConsistencyMode.CONSISTENT && it.linkKey != null }
                .groupBy { it.linkKey!! }
                .forEach { (linkKey, linkedGenerators) ->
                    val primaryGen = linkedGenerators.first()
                    val originalValue = row[primaryGen.columnName]?.toString() ?: ""
                    val seed = hmacSeed(workspaceSecret, originalValue)
                    linkGroupFakers[linkKey] = Faker(java.util.Random(seed))
                }
        }

        for (generator in generators) {
            val params: Map<String, String>? = generator.generatorParams?.let {
                try { mapper.readValue(it) } catch (e: Exception) { null }
            }
            val columnKey = "${generator.tableConfigurationId}:${generator.columnName}"
            val originalValue = row[generator.columnName]

            val value = when {
                generator.consistencyMode == ConsistencyMode.CONSISTENT && workspaceSecret != null -> {
                    val fakerToUse = generator.linkKey?.let { linkGroupFakers[it] }
                        ?: run {
                            val seed = hmacSeed(workspaceSecret, originalValue?.toString() ?: "")
                            Faker(java.util.Random(seed))
                        }
                    generateValue(generator.generatorType, originalValue, params, fakerToUse, columnKey, generator.generatorParams)
                }
                else -> generateValue(generator.generatorType, originalValue, params, this.faker, columnKey, generator.generatorParams)
            }
            result[generator.columnName] = value
        }
        return result
    }

    fun generateRows(generators: List<ColumnGenerator>, count: Int): List<Map<String, Any?>> {
        return (1..count).map {
            generators.associate { generator ->
                val params: Map<String, String>? = generator.generatorParams?.let {
                    try { mapper.readValue(it) } catch (e: Exception) { null }
                }
                val columnKey = "${generator.tableConfigurationId}:${generator.columnName}"
                generator.columnName to generateValue(generator.generatorType, null, params, this.faker, columnKey, generator.generatorParams)
            }
        }
    }
}