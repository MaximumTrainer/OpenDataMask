package com.opendatamask.domain.port.input

import com.opendatamask.model.ForeignKeyRelationship

interface ForeignKeyUseCase {
    fun discoverForeignKeys(workspaceId: Long): List<ForeignKeyRelationship>
    fun getVirtualForeignKeys(workspaceId: Long): List<ForeignKeyRelationship>
    fun createVirtualForeignKey(workspaceId: Long, fromTable: String, fromColumn: String, toTable: String, toColumn: String): ForeignKeyRelationship
    fun deleteForeignKey(workspaceId: Long, fkId: Long)
}
