package com.opendatamask.domain.model

data class SensitivityRule(
    val sensitivityType: SensitivityType,
    val recommendedGenerator: GeneratorType,
    val columnNamePatterns: List<Regex>,
    val valuePatterns: List<Regex>,
    val confidence: ConfidenceLevel
)

fun buildRules(): List<SensitivityRule> = listOf(
    SensitivityRule(
        sensitivityType = SensitivityType.EMAIL,
        recommendedGenerator = GeneratorType.EMAIL,
        columnNamePatterns = listOf(Regex("email|e_mail|email_address", RegexOption.IGNORE_CASE)),
        valuePatterns = listOf(Regex("""\S+@\S+\.\S+""")),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.SSN,
        recommendedGenerator = GeneratorType.SSN,
        columnNamePatterns = listOf(Regex("ssn|social_security", RegexOption.IGNORE_CASE)),
        valuePatterns = listOf(Regex("""\d{3}-\d{2}-\d{4}|\d{9}""")),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.CREDIT_CARD,
        recommendedGenerator = GeneratorType.CREDIT_CARD,
        columnNamePatterns = listOf(Regex("credit_card|card_number|cc_num", RegexOption.IGNORE_CASE)),
        valuePatterns = listOf(Regex("""\d{4}[\-\s]?\d{4}[\-\s]?\d{4}[\-\s]?\d{4}""")),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.PHONE,
        recommendedGenerator = GeneratorType.PHONE,
        columnNamePatterns = listOf(Regex("phone|telephone|mobile|cell", RegexOption.IGNORE_CASE)),
        valuePatterns = listOf(Regex("""[\d\-\(\)\+]{7,15}""")),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.FIRST_NAME,
        recommendedGenerator = GeneratorType.FIRST_NAME,
        columnNamePatterns = listOf(Regex("first_name|firstname|given_name", RegexOption.IGNORE_CASE)),
        valuePatterns = emptyList(),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.LAST_NAME,
        recommendedGenerator = GeneratorType.LAST_NAME,
        columnNamePatterns = listOf(Regex("last_name|lastname|surname|family_name", RegexOption.IGNORE_CASE)),
        valuePatterns = emptyList(),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.FULL_NAME,
        recommendedGenerator = GeneratorType.FULL_NAME,
        columnNamePatterns = listOf(Regex("full_name|fullname|(?<![a-z])name(?![a-z])", RegexOption.IGNORE_CASE)),
        valuePatterns = emptyList(),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.IP_ADDRESS,
        recommendedGenerator = GeneratorType.IP_ADDRESS,
        columnNamePatterns = listOf(Regex("(?<![a-z])ip(?![a-z])|ip_address|ipv4", RegexOption.IGNORE_CASE)),
        valuePatterns = listOf(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""")),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.PASSWORD,
        recommendedGenerator = GeneratorType.PASSWORD,
        columnNamePatterns = listOf(Regex("password|passwd|pwd|hash", RegexOption.IGNORE_CASE)),
        valuePatterns = emptyList(),
        confidence = ConfidenceLevel.MEDIUM
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.USERNAME,
        recommendedGenerator = GeneratorType.USERNAME,
        columnNamePatterns = listOf(Regex("username|user_name|login|handle", RegexOption.IGNORE_CASE)),
        valuePatterns = emptyList(),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.BIRTH_DATE,
        recommendedGenerator = GeneratorType.BIRTH_DATE,
        columnNamePatterns = listOf(Regex("birth_date|birthdate|dob|date_of_birth", RegexOption.IGNORE_CASE)),
        valuePatterns = emptyList(),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.STREET_ADDRESS,
        recommendedGenerator = GeneratorType.STREET_ADDRESS,
        columnNamePatterns = listOf(Regex("(?<![a-z])address|street|(?<![a-z])addr(?![a-z])", RegexOption.IGNORE_CASE)),
        valuePatterns = emptyList(),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.ZIP_CODE,
        recommendedGenerator = GeneratorType.ZIP_CODE,
        columnNamePatterns = listOf(Regex("(?<![a-z])zip(?![a-z])|zipcode|postal_code|postcode", RegexOption.IGNORE_CASE)),
        valuePatterns = emptyList(),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.CITY,
        recommendedGenerator = GeneratorType.CITY,
        columnNamePatterns = listOf(Regex("(?<![a-z])city(?![a-z])|(?<![a-z])town(?![a-z])", RegexOption.IGNORE_CASE)),
        valuePatterns = emptyList(),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.STATE,
        recommendedGenerator = GeneratorType.STATE,
        columnNamePatterns = listOf(Regex("(?<![a-z])state(?![a-z])|province|region", RegexOption.IGNORE_CASE)),
        valuePatterns = emptyList(),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.COUNTRY,
        recommendedGenerator = GeneratorType.COUNTRY,
        columnNamePatterns = listOf(Regex("country", RegexOption.IGNORE_CASE)),
        valuePatterns = emptyList(),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.ORGANIZATION,
        recommendedGenerator = GeneratorType.ORGANIZATION,
        columnNamePatterns = listOf(Regex("company|organization|(?<![a-z])org(?![a-z])|employer", RegexOption.IGNORE_CASE)),
        valuePatterns = emptyList(),
        confidence = ConfidenceLevel.HIGH
    ),
    SensitivityRule(
        sensitivityType = SensitivityType.GENDER,
        recommendedGenerator = GeneratorType.GENDER,
        columnNamePatterns = listOf(Regex("(?<![a-z])gender(?![a-z])|(?<![a-z])sex(?![a-z])", RegexOption.IGNORE_CASE)),
        valuePatterns = emptyList(),
        confidence = ConfidenceLevel.HIGH
    )
)
