package com.opendatamask.domain.port.input

import com.opendatamask.dto.PostJobActionRequest
import com.opendatamask.model.PostJobAction

interface PostJobActionUseCase {
    fun listActions(workspaceId: Long): List<PostJobAction>
    fun createAction(action: PostJobAction): PostJobAction
    fun updateAction(workspaceId: Long, id: Long, request: PostJobActionRequest): PostJobAction
    fun deleteAction(id: Long)
}
