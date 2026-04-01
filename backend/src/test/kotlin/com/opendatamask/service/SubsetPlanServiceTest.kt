package com.opendatamask.service

import com.opendatamask.model.ForeignKeyRelationship
import com.opendatamask.model.SubsetLimitType
import com.opendatamask.model.SubsetTableConfig
import com.opendatamask.repository.ForeignKeyRelationshipRepository
import com.opendatamask.repository.SubsetTableConfigRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
class SubsetPlanServiceTest {

    private val fkRepo = mock<ForeignKeyRelationshipRepository>()
    private val subsetConfigRepo = mock<SubsetTableConfigRepository>()
    private val service = SubsetPlanService(fkRepo, subsetConfigRepo)

    private fun makeConfig(tableName: String, isTarget: Boolean = false): SubsetTableConfig =
        SubsetTableConfig(
            id = null,
            workspaceId = 1L,
            tableName = tableName,
            limitType = SubsetLimitType.PERCENTAGE,
            limitValue = 10,
            isTargetTable = isTarget
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
    fun `buildExecutionPlan returns customers before orders before order_items`() {
        val fks = listOf(
            makeFk("orders", "customer_id", "customers"),
            makeFk("order_items", "order_id", "orders")
        )
        val configs = listOf(
            makeConfig("customers", isTarget = true),
            makeConfig("orders"),
            makeConfig("order_items")
        )
        whenever(fkRepo.findByWorkspaceId(1L)).thenReturn(fks)
        whenever(subsetConfigRepo.findByWorkspaceId(1L)).thenReturn(configs)

        val plan = service.buildExecutionPlan(1L)

        val tableNames = plan.map { it.tableName }
        assertTrue(tableNames.contains("customers"), "Plan should include customers")
        assertTrue(tableNames.contains("orders"), "Plan should include orders")
        assertTrue(tableNames.contains("order_items"), "Plan should include order_items")

        val customersIdx = tableNames.indexOf("customers")
        val ordersIdx = tableNames.indexOf("orders")
        val orderItemsIdx = tableNames.indexOf("order_items")

        assertTrue(customersIdx < ordersIdx, "customers must come before orders")
        assertTrue(ordersIdx < orderItemsIdx, "orders must come before order_items")
    }

    @Test
    fun `buildExecutionPlan populates dependsOn correctly`() {
        val fks = listOf(
            makeFk("orders", "customer_id", "customers")
        )
        val configs = listOf(
            makeConfig("customers", isTarget = true),
            makeConfig("orders")
        )
        whenever(fkRepo.findByWorkspaceId(1L)).thenReturn(fks)
        whenever(subsetConfigRepo.findByWorkspaceId(1L)).thenReturn(configs)

        val plan = service.buildExecutionPlan(1L)
        val ordersStep = plan.first { it.tableName == "orders" }
        assertTrue(ordersStep.dependsOn.contains("customers"), "orders should depend on customers")
    }

    @Test
    fun `buildExecutionPlan returns empty when no configs`() {
        whenever(fkRepo.findByWorkspaceId(1L)).thenReturn(emptyList())
        whenever(subsetConfigRepo.findByWorkspaceId(1L)).thenReturn(emptyList())

        val plan = service.buildExecutionPlan(1L)
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `buildExecutionPlan handles single table with no FKs`() {
        val configs = listOf(makeConfig("users", isTarget = true))
        whenever(fkRepo.findByWorkspaceId(1L)).thenReturn(emptyList())
        whenever(subsetConfigRepo.findByWorkspaceId(1L)).thenReturn(configs)

        val plan = service.buildExecutionPlan(1L)
        assertEquals(1, plan.size)
        assertEquals("users", plan[0].tableName)
    }
}
