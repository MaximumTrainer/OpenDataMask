package com.opendatamask.service

import com.opendatamask.domain.model.SubsetTableConfig
import com.opendatamask.repository.ForeignKeyRelationshipRepository
import com.opendatamask.repository.SubsetTableConfigRepository
import org.springframework.stereotype.Service

data class SubsetStep(
    val tableName: String,
    val config: SubsetTableConfig,
    val dependsOn: List<String>
)

@Service
class SubsetPlanService(
    private val foreignKeyRelationshipRepository: ForeignKeyRelationshipRepository,
    private val subsetTableConfigRepository: SubsetTableConfigRepository
) {
    /**
     * Build topological execution order. Parents (referenced tables) come before children (tables with FKs).
     * This ensures referenced rows are available when child rows need to reference them.
     */
    fun buildExecutionPlan(workspaceId: Long): List<SubsetStep> {
        val fks = foreignKeyRelationshipRepository.findByWorkspaceId(workspaceId)
        val configs = subsetTableConfigRepository.findByWorkspaceId(workspaceId)
        val configByTable = configs.associateBy { it.tableName }

        // Collect all tables involved
        val allTables = (configs.map { it.tableName } +
            fks.map { it.fromTable } +
            fks.map { it.toTable }).toSet()

        // Build adjacency: child -> list of parents (toTable depends on fromTable referencing toTable)
        // In topological sort, parent must come before child
        // child (fromTable) depends on parent (toTable)
        val dependsOnMap = mutableMapOf<String, MutableSet<String>>()
        for (table in allTables) {
            dependsOnMap.getOrPut(table) { mutableSetOf() }
        }
        for (fk in fks) {
            // fromTable has FK pointing to toTable → fromTable depends on toTable
            dependsOnMap.getOrPut(fk.fromTable) { mutableSetOf() }.add(fk.toTable)
        }

        val sorted = topologicalSort(allTables, dependsOnMap)

        return sorted.mapNotNull { tableName ->
            val config = configByTable[tableName] ?: return@mapNotNull null
            val deps = fks.filter { it.fromTable == tableName }.map { it.toTable }
            SubsetStep(tableName = tableName, config = config, dependsOn = deps.distinct())
        }
    }

    private fun topologicalSort(
        tables: Set<String>,
        dependsOn: Map<String, Set<String>>
    ): List<String> {
        val visited = mutableSetOf<String>()
        val result = mutableListOf<String>()

        fun visit(table: String) {
            if (table in visited) return
            visited.add(table)
            for (dep in dependsOn[table] ?: emptySet()) {
                visit(dep)
            }
            result.add(table)
        }

        for (table in tables.sorted()) {
            visit(table)
        }

        // result is in post-order (parents before children when reversed)
        // Actually with the approach above: we visit dependencies first then add current,
        // so result has parents before children already
        return result
    }
}
