package com.opendatamask.controller

import com.opendatamask.model.PostJobAction
import com.opendatamask.service.PostJobActionService
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
        @RequestBody action: PostJobAction
    ): ResponseEntity<PostJobAction> {
        val toSave = action.copy(workspaceId = workspaceId)
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAction(toSave))
    }

    @PutMapping("/{actionId}")
    fun updateAction(
        @PathVariable workspaceId: Long,
        @PathVariable actionId: Long,
        @RequestBody action: PostJobAction
    ): ResponseEntity<PostJobAction> =
        ResponseEntity.ok(service.updateAction(actionId, action.copy(workspaceId = workspaceId)))

    @DeleteMapping("/{actionId}")
    fun deleteAction(
        @PathVariable workspaceId: Long,
        @PathVariable actionId: Long
    ): ResponseEntity<Void> {
        service.deleteAction(actionId)
        return ResponseEntity.noContent().build()
    }
}
