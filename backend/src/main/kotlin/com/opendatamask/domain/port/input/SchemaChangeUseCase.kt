package com.opendatamask.domain.port.input

import com.opendatamask.domain.model.SchemaChange

interface SchemaChangeUseCase {
    fun detectChanges(workspaceId: Long): List<SchemaChange>
    fun getUnresolvedChanges(workspaceId: Long): List<SchemaChange>
    fun resolveChange(changeId: Long)
    fun dismissChange(changeId: Long)
    fun resolveAll(workspaceId: Long)
    fun dismissAll(workspaceId: Long)
    fun isBlockingJobRun(workspaceId: Long): Boolean
}
