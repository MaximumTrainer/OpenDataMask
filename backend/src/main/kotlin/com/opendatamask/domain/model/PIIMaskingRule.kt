package com.opendatamask.domain.model

import java.security.MessageDigest

// Core interface every PII masking rule must implement.
// Each implementation must carry a stable, unique ruleId that identifies it
// in the registry and in JSON mapping configurations.
interface PIIMaskingRule {
    val ruleId: String
    fun mask(input: Any?): Any?
}

// Sealed base class for built-in rules so exhaustive when-expressions can be used
// when dispatch on rule type is needed in the application layer.
sealed class BuiltInPIIRule(override val ruleId: String) : PIIMaskingRule

// Passes the value through unchanged. Used as the default for unmapped columns.
class PassThroughRule : BuiltInPIIRule("pass_through") {
    override fun mask(input: Any?): Any? = input
}

// Replaces every non-null value with the literal token [REDACTED].
class RedactRule : BuiltInPIIRule("redact") {
    override fun mask(input: Any?): Any? = if (input == null) null else "[REDACTED]"
}

// Masks the middle characters of a string, preserving a configurable number of
// leading and trailing characters. Useful for partial credit-card or email masking.
// When the input is shorter than keepFirst + keepLast, the original value is returned unchanged.
class PartialMaskRule(
    val keepFirst: Int = 0,
    val keepLast: Int = 4,
    val maskChar: Char = '*'
) : BuiltInPIIRule("partial_mask") {
    override fun mask(input: Any?): Any? {
        if (input == null) return null
        val str = input.toString()
        if (str.length <= keepFirst + keepLast) return str
        val maskLen = str.length - keepFirst - keepLast
        return str.take(keepFirst) + maskChar.toString().repeat(maskLen) + str.takeLast(keepLast)
    }
}

// Produces a deterministic SHA-256 hex digest of the input, with an optional salt.
class HashRule(val salt: String = "") : BuiltInPIIRule("hash") {
    override fun mask(input: Any?): Any? {
        if (input == null) return null
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(("$salt${input}").toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

// Applies a user-supplied regular expression to the string representation of the value,
// replacing every match with the given replacement string.
class RegexRule(val pattern: String, val replacement: String) : BuiltInPIIRule("regex") {
    private val compiledRegex = Regex(pattern)
    override fun mask(input: Any?): Any? {
        if (input == null) return null
        return compiledRegex.replace(input.toString(), replacement)
    }
}
