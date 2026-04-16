package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.PIIMaskingRule

// Driven port: a registry that maps rule IDs to PIIMaskingRule implementations.
// The default implementation is provided by DefaultRuleRegistry in the application layer.
// Custom rules can be registered at runtime via registerCustomRule().
interface RuleRegistryPort {
    fun getRule(ruleId: String): PIIMaskingRule?
    fun getAllRuleIds(): Set<String>
    fun registerCustomRule(rule: PIIMaskingRule)
}
