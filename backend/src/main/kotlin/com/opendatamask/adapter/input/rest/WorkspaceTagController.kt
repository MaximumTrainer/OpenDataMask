package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.model.WorkspaceTag
import com.opendatamask.adapter.output.persistence.WorkspaceTagRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tags")
class WorkspaceTagController(
    private val tagRepository: WorkspaceTagRepository
) {
    @GetMapping
    fun listTags(@PathVariable workspaceId: Long): ResponseEntity<List<String>> =
        ResponseEntity.ok(tagRepository.findByWorkspaceId(workspaceId).map { it.tag })

    @PostMapping
    fun addTag(
        @PathVariable workspaceId: Long,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<WorkspaceTag> {
        val tag = body["tag"]?.trim()
            ?: return ResponseEntity.badRequest().build()
        if (tag.isBlank() || tag.length > 100) return ResponseEntity.badRequest().build()
        val saved = tagRepository.save(WorkspaceTag(workspaceId = workspaceId, tag = tag))
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @DeleteMapping("/{tag}")
    fun removeTag(
        @PathVariable workspaceId: Long,
        @PathVariable tag: String
    ): ResponseEntity<Void> {
        tagRepository.deleteByWorkspaceIdAndTag(workspaceId, tag)
        return ResponseEntity.noContent().build()
    }
}
