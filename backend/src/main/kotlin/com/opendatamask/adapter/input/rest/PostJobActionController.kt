package com.opendatamask.adapter.input.rest

import com.opendatamask.adapter.input.rest.dto.PostJobActionRequest
import com.opendatamask.domain.model.PostJobAction
import com.opendatamask.application.service.PostJobActionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/actions")
class PostJobActionController(
    private val service: PostJobActionService
) {
    @GetMapping
    fun listActions(@PathVariable workspaceId: Long): ResponseEntity<List<PostJobAction>> =
        ResponseEntity.ok(service.listActions(workspaceId))

    @PostMapping
    fun createAction(
        @PathVariable workspaceId: Long,
        @RequestBody request: PostJobActionRequest
    ): ResponseEntity<PostJobAction> {
        val toSave = PostJobAction(
            workspaceId = workspaceId,
            actionType = request.actionType,
            config = request.config,
            enabled = request.enabled
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAction(toSave))
    }

    @PutMapping("/{actionId}")
    fun updateAction(
        @PathVariable workspaceId: Long,
        @PathVariable actionId: Long,
        @RequestBody request: PostJobActionRequest
    ): ResponseEntity<PostJobAction> =
        ResponseEntity.ok(service.updateAction(workspaceId, actionId, request))

    @DeleteMapping("/{actionId}")
    fun deleteAction(
        @PathVariable workspaceId: Long,
        @PathVariable actionId: Long
    ): ResponseEntity<Void> {
        service.deleteAction(actionId)
        return ResponseEntity.noContent().build()
    }
}
