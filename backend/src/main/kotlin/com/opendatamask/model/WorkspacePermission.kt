package com.opendatamask.model

enum class WorkspacePermission {
    CONFIGURE_GENERATORS,
    PREVIEW_SOURCE_DATA,
    PREVIEW_DESTINATION_DATA,
    CONFIGURE_SUBSETTING,
    CONFIGURE_SENSITIVITY,
    CONFIGURE_POST_JOB_ACTIONS,
    CONFIGURE_SCHEMA_CHANGE_SETTINGS,
    RESOLVE_SCHEMA_CHANGES,
    RUN_JOBS;

    companion object {
        val DEFAULT_PERMISSIONS: Map<WorkspaceRole, Set<WorkspacePermission>> = mapOf(
            WorkspaceRole.ADMIN to values().toSet(),
            WorkspaceRole.USER to setOf(
                CONFIGURE_GENERATORS,
                PREVIEW_SOURCE_DATA,
                PREVIEW_DESTINATION_DATA,
                RUN_JOBS
            ),
            WorkspaceRole.VIEWER to setOf(
                PREVIEW_SOURCE_DATA,
                PREVIEW_DESTINATION_DATA
            )
        )
    }
}
