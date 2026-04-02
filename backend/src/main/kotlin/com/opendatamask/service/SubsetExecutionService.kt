package com.opendatamask.service

import com.opendatamask.connector.DatabaseConnector
import com.opendatamask.domain.model.SubsetLimitType
import com.opendatamask.repository.ForeignKeyRelationshipRepository
import com.opendatamask.repository.SubsetTableConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SubsetExecutionService(
    private val foreignKeyRelationshipRepository: ForeignKeyRelationshipRepository,
    private val subsetTableConfigRepository: SubsetTableConfigRepository,
    private val subsetPlanService: SubsetPlanService
) {
    private val logger = LoggerFactory.getLogger(SubsetExecutionService::class.java)

    /**
     * Execute the subset plan and return rows grouped by table name, preserving FK integrity.
     */
    fun executeSubset(workspaceId: Long, sourceConnector: DatabaseConnector): Map<String, List<Map<String, Any?>>> {
        val plan = subsetPlanService.buildExecutionPlan(workspaceId)
        if (plan.isEmpty()) return emptyMap()

        val fks = foreignKeyRelationshipRepository.findByWorkspaceId(workspaceId)
        val collectedRows = mutableMapOf<String, MutableList<Map<String, Any?>>>()
        val collectedPks = mutableMapOf<String, MutableSet<Any?>>()

        // First pass: seed target tables and lookup tables
        for (step in plan) {
            val config = step.config
            when {
                config.isLookupTable -> {
                    val rows = sourceConnector.fetchData(config.tableName)
                    collectedRows.getOrPut(config.tableName) { mutableListOf() }.addAll(rows)
                    logger.debug("Lookup table ${config.tableName}: collected ${rows.size} rows")
                }
                config.isTargetTable -> {
                    val rows = fetchWithLimit(sourceConnector, config.tableName, config.limitType, config.limitValue)
                    collectedRows.getOrPut(config.tableName) { mutableListOf() }.addAll(rows)
                    logger.debug("Target table ${config.tableName}: seeded ${rows.size} rows")
                }
            }
        }

        // Track PKs for FK traversal — use "id" as default PK column heuristic
        for ((tableName, rows) in collectedRows) {
            val pkSet: MutableSet<Any?> = rows.map { it["id"] }.toMutableSet()
            collectedPks[tableName] = pkSet
        }

        // Iterative FK traversal: collect parent rows referenced by collected child rows
        // Max 10 iterations to handle indirect chains and potential cycles
        repeat(10) { iteration ->
            var newRowsFound = false

            for (step in plan) {
                val tableName = step.tableName
                val config = step.config
                if (config.isLookupTable) return@repeat  // lookup tables already fully collected

                // For each FK where this table is the child (fromTable),
                // collect parent rows that are referenced
                val outboundFks = fks.filter { it.fromTable == tableName }
                val childRows = collectedRows[tableName] ?: continue

                for (fk in outboundFks) {
                    val referencedValues = childRows.mapNotNull { it[fk.fromColumn] }.toSet()
                    if (referencedValues.isEmpty()) continue

                    val alreadyCollectedPks = collectedPks.getOrPut(fk.toTable) { mutableSetOf() }
                    val missingValues = referencedValues - alreadyCollectedPks
                    if (missingValues.isEmpty()) continue

                    val whereClause = buildWhereInClause(fk.toColumn, missingValues)
                    val parentRows = sourceConnector.fetchData(fk.toTable, whereClause = whereClause)
                    if (parentRows.isNotEmpty()) {
                        collectedRows.getOrPut(fk.toTable) { mutableListOf() }.addAll(parentRows)
                        alreadyCollectedPks.addAll(parentRows.mapNotNull { it[fk.toColumn] })
                        newRowsFound = true
                        logger.debug("FK traversal iter=$iteration: ${fk.toTable} ← ${fk.fromTable}.${fk.fromColumn}: +${parentRows.size} rows")
                    }
                }

                // For non-target, non-lookup tables with no direct seeding: pull rows where FK matches collected parents
                if (!config.isTargetTable && !config.isLookupTable && tableName !in collectedRows) {
                    val inboundFks = fks.filter { it.toTable == tableName }
                    for (fk in inboundFks) {
                        val parentPks = collectedPks[fk.fromTable] ?: continue
                        if (parentPks.isEmpty()) continue
                        val whereClause = buildWhereInClause(fk.toColumn, parentPks)
                        val rows = sourceConnector.fetchData(tableName, whereClause = whereClause)
                        if (rows.isNotEmpty()) {
                            collectedRows.getOrPut(tableName) { mutableListOf() }.addAll(rows)
                            collectedPks.getOrPut(tableName) { mutableSetOf() }
                                .addAll(rows.mapNotNull { it["id"] })
                            newRowsFound = true
                        }
                    }
                }
            }

            if (!newRowsFound) return@repeat
        }

        // For remaining configured tables that still have no rows, fetch with limit config
        for (step in plan) {
            val config = step.config
            if (config.tableName !in collectedRows && !config.isLookupTable && !config.isTargetTable) {
                val rows = fetchWithLimit(sourceConnector, config.tableName, config.limitType, config.limitValue)
                if (rows.isNotEmpty()) {
                    collectedRows[config.tableName] = rows.toMutableList()
                }
            }
        }

        return collectedRows
    }

    fun buildWhereInClause(column: String, values: Set<Any?>): String {
        val valueList = values.filterNotNull().joinToString(",") { "'$it'" }
        return if (valueList.isEmpty()) "1=0" else "$column IN ($valueList)"
    }

    private fun fetchWithLimit(
        connector: DatabaseConnector,
        tableName: String,
        limitType: SubsetLimitType,
        limitValue: Int
    ): List<Map<String, Any?>> {
        return when (limitType) {
            SubsetLimitType.ALL -> connector.fetchData(tableName)
            SubsetLimitType.ROW_COUNT -> connector.fetchData(tableName, limit = limitValue)
            SubsetLimitType.PERCENTAGE -> {
                // Fetch all to calculate percentage; for large tables a COUNT query would be preferable
                // but DatabaseConnector doesn't expose COUNT, so we fetch with a row cap heuristic
                val allRows = connector.fetchData(tableName)
                val count = (allRows.size * limitValue / 100).coerceAtLeast(1)
                allRows.take(count)
            }
        }
    }
}
