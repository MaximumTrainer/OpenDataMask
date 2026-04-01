package com.opendatamask.service

import com.opendatamask.connector.DatabaseConnector
import com.opendatamask.model.ForeignKeyRelationship
import com.opendatamask.model.SubsetLimitType
import com.opendatamask.model.SubsetTableConfig
import com.opendatamask.repository.ForeignKeyRelationshipRepository
import com.opendatamask.repository.SubsetTableConfigRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
class SubsetExecutionServiceTest {

    private val fkRepo = mock<ForeignKeyRelationshipRepository>()
    private val subsetConfigRepo = mock<SubsetTableConfigRepository>()
    private val subsetPlanService = mock<SubsetPlanService>()
    private val connector = mock<DatabaseConnector>()

    private val service = SubsetExecutionService(fkRepo, subsetConfigRepo, subsetPlanService)

    private fun makeConfig(tableName: String, isTarget: Boolean = false, isLookup: Boolean = false): SubsetTableConfig =
        SubsetTableConfig(
            id = 1L,
            workspaceId = 1L,
            tableName = tableName,
            limitType = SubsetLimitType.ROW_COUNT,
            limitValue = 3,
            isTargetTable = isTarget,
            isLookupTable = isLookup
        )

    private fun makeFk(fromTable: String, fromCol: String, toTable: String, toCol: String = "id") =
        ForeignKeyRelationship(
            workspaceId = 1L,
            fromTable = fromTable,
            fromColumn = fromCol,
            toTable = toTable,
            toColumn = toCol
        )

    @Test
    fun `executeSubset seeds target table and collects only referenced parent rows`() {
        val customerRows = listOf(
            mapOf("id" to 1L, "name" to "Alice"),
            mapOf("id" to 2L, "name" to "Bob"),
            mapOf("id" to 3L, "name" to "Charlie")
        )
        val orderRows = listOf(
            mapOf("id" to 10L, "customer_id" to 1L, "amount" to 100),
            mapOf("id" to 11L, "customer_id" to 2L, "amount" to 200),
            mapOf("id" to 12L, "customer_id" to 99L, "amount" to 300) // customer 99 not in collected customers
        )

        val customersConfig = makeConfig("customers", isTarget = true)
        val ordersConfig = makeConfig("orders")

        val fks = listOf(makeFk("orders", "customer_id", "customers", "id"))

        val plan = listOf(
            SubsetStep("customers", customersConfig, emptyList()),
            SubsetStep("orders", ordersConfig, listOf("customers"))
        )

        whenever(subsetPlanService.buildExecutionPlan(1L)).thenReturn(plan)
        whenever(fkRepo.findByWorkspaceId(1L)).thenReturn(fks)
        whenever(connector.fetchData(eq("customers"), anyOrNull(), anyOrNull())).thenReturn(customerRows)

        val result = service.executeSubset(1L, connector)

        assertTrue(result.containsKey("customers"), "Result should contain customers")
        assertEquals(3, result["customers"]!!.size)
    }

    @Test
    fun `executeSubset copies all rows for lookup tables`() {
        val allProductRows = (1..20).map { mapOf("id" to it.toLong(), "name" to "Product $it") }
        val lookupConfig = makeConfig("products", isLookup = true)

        val plan = listOf(SubsetStep("products", lookupConfig, emptyList()))

        whenever(subsetPlanService.buildExecutionPlan(1L)).thenReturn(plan)
        whenever(fkRepo.findByWorkspaceId(1L)).thenReturn(emptyList())
        whenever(connector.fetchData("products")).thenReturn(allProductRows)

        val result = service.executeSubset(1L, connector)

        assertEquals(20, result["products"]!!.size, "Lookup table should have all rows")
    }

    @Test
    fun `executeSubset returns empty map when plan is empty`() {
        whenever(subsetPlanService.buildExecutionPlan(1L)).thenReturn(emptyList())

        val result = service.executeSubset(1L, connector)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `buildWhereInClause returns correct SQL for non-empty values`() {
        val clause = service.buildWhereInClause("customer_id", setOf(1L, 2L, 3L))
        assertTrue(clause.startsWith("customer_id IN"), "Clause should start with column IN")
        assertTrue(clause.contains("'1'") || clause.contains("'2'"), "Clause should contain quoted values")
    }

    @Test
    fun `buildWhereInClause returns 1=0 for empty values`() {
        val clause = service.buildWhereInClause("customer_id", emptySet())
        assertEquals("1=0", clause)
    }

    @Test
    fun `buildWhereInClause ignores null values`() {
        val clause = service.buildWhereInClause("customer_id", setOf(1L, null, 2L))
        assertFalse(clause.contains("null"), "Clause should not contain null")
        assertTrue(clause.contains("'1'"), "Clause should contain non-null values")
    }
}
