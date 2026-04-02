package com.opendatamask.controller

import com.opendatamask.domain.model.ColumnComment
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.service.ColumnCommentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tables/{tableName}/columns/{columnName}/comments")
class ColumnCommentController(
    private val commentService: ColumnCommentService,
    private val userRepository: UserRepository
) {
    @GetMapping
    fun getComments(
        @PathVariable workspaceId: Long,
        @PathVariable tableName: String,
        @PathVariable columnName: String
    ): ResponseEntity<List<ColumnComment>> =
        ResponseEntity.ok(commentService.getComments(workspaceId, tableName, columnName))

    @PostMapping
    fun addComment(
        @PathVariable workspaceId: Long,
        @PathVariable tableName: String,
        @PathVariable columnName: String,
        @RequestBody body: Map<String, String>,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ColumnComment> {
        val comment = body["comment"] ?: return ResponseEntity.badRequest().build()
        val user = userRepository.findByUsername(userDetails.username).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val saved = commentService.addComment(workspaceId, tableName, columnName, user.id, comment)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @PathVariable workspaceId: Long,
        @PathVariable tableName: String,
        @PathVariable columnName: String,
        @PathVariable commentId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Void> {
        val user = userRepository.findByUsername(userDetails.username).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        commentService.deleteComment(commentId, user.id)
        return ResponseEntity.noContent().build()
    }
}
