package com.opendatamask.application.service

import com.opendatamask.domain.model.HashRule
import com.opendatamask.domain.model.PIIMaskingRule
import com.opendatamask.domain.model.PartialMaskRule
import com.opendatamask.domain.model.PassThroughRule
import com.opendatamask.domain.model.RedactRule
import com.opendatamask.domain.port.output.RuleRegistryPort
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

// Holds all known PIIMaskingRule implementations.
// Built-in rules are registered at construction time.
// Call registerCustomRule() to inject additional business-specific rules at runtime.
@Service
class DefaultRuleRegistry : RuleRegistryPort {

    private val registry = ConcurrentHashMap<String, PIIMaskingRule>()

    init {
        register(PassThroughRule())
        register(RedactRule())
        register(PartialMaskRule())
        register(HashRule())
    }

    private fun register(rule: PIIMaskingRule) {
        registry[rule.ruleId] = rule
    }

    override fun getRule(ruleId: String): PIIMaskingRule? = registry[ruleId]

    override fun getAllRuleIds(): Set<String> = registry.keys.toSet()

    override fun registerCustomRule(rule: PIIMaskingRule) = register(rule)
}
